package com.imaginea.training.crawler.core;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.exception.CrawlException;
import com.imaginea.training.crawler.parser.Parser;
import com.imaginea.training.crawler.util.FileUtil;

/**
 * 
 * @author manojm
 *
 */
public class Crawler {
	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
			
	private Controller controller;
	
	private Parser parser;

	private int currentMsgCount = 0;
	
	public void init() {
		this.parser = new Parser();
		this.controller = new Controller();
	}

	public void start() {
		run();
	}

	private void run() {
		try {
			processPage();
		} catch (CrawlException e) {
			logger.error("Crawler - run failed", e);
		}
	}

	/**
	 * 
	 * @throws CrawlException
	 */
	private void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			// Get the html content for all input years
			final WebClient webClient = new WebClient();
			final HtmlPage page = parser.getPage(webClient);
			List<HtmlElement> table_yearElements = parser.parseTableForYears(page, Constant.YEAR_2014);
			String month = null; 
			String msgCount = null;
			String year = null;

			// Iterate Years
			for (HtmlElement table_yearElement : table_yearElements) {
				List<HtmlElement> th_yearElements = table_yearElement.getElementsByAttribute(Constant.TH, Constant.COLSPAN, "3");
				year = th_yearElements.get(0).asText();
				HtmlTable table_yearList = (HtmlTable) table_yearElement;
				List<HtmlTableBody> tbody_yearlist = table_yearList.getBodies();
				HtmlTableBody tbody_year = tbody_yearlist.get(0);

				// Iterate Months
				for (final HtmlTableRow tr_monthNode : tbody_year.getRows()) {
					if(logger.isInfoEnabled() || logger.isDebugEnabled()){
						List<HtmlElement> td_monthDateElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
						month = td_monthDateElement.get(0).asText();
						List<HtmlElement> td_monthMsgcountElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, "msgcount");
						msgCount = td_monthMsgcountElement.get(0).asText();
						logger.info("Year: " + year + ", Month: " + month + ", MsgCount: " + msgCount);
					}
					
					// href links for msgs by thread (subject)
					DomNodeList<HtmlElement> anchor_monthNodes = tr_monthNode.getElementsByTagName(Constant.TAG_A);
					for (HtmlElement anchor_monthNode : anchor_monthNodes) {
						if(anchor_monthNode.getAttribute("href").contains("mbox/thread") && anchor_monthNode.getAttribute(Constant.HREF).contains(Constant.YEAR_2014)) {
							
							// Get the list of emails from 1st page
							long startTime = System.currentTimeMillis();
							currentMsgCount = 0;
							HtmlElement msglistElement = extractListOfEmailsFromPage(anchor_monthNode, year, month); 
							boolean isNextPageAvailable = true;
							
							// Get the list of emails from all pages
							while (isNextPageAvailable) {
								List<HtmlElement> th_pagesElements = msglistElement.getElementsByAttribute(Constant.TH, Constant.CLASS, Constant.PAGES);
								HtmlElement th_pageElement = th_pagesElements.get(0);
								HtmlElement anchor_nextNode = (HtmlElement)th_pageElement.getLastElementChild();
								
								if(anchor_nextNode.getNodeName().equals(Constant.TAG_A) && anchor_nextNode.asText().contains(Constant.NEXT)) {
									msglistElement = extractListOfEmailsFromPage(anchor_nextNode, year, month);
								} else {
									isNextPageAvailable = false;	
								}
							} 

							// Log information
							if(logger.isInfoEnabled() || logger.isDebugEnabled()) {
								logger.info("Message Count: " + currentMsgCount);
								long endTime = System.currentTimeMillis();
								logger.info("Duration- Seconds : " + (endTime-startTime)/1000 + ", Minutes: " + (endTime-startTime)/(1000*60)); 
								logger.info("-----");
							}
						}
					}
				}
			}
			webClient.closeAllWindows();
			logger.debug("processPage end");
		} catch (FailingHttpStatusCodeException e) {
			logger.error("Crawler - Http status code exception", e);
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("Crawler - processPage failed", e);
			throw new CrawlException(e);
		}
	}
	
	/**
	 * 
	 * @param anchor_monthNode
	 * @return
	 * @throws CrawlException
	 */
	private HtmlElement extractListOfEmailsFromPage(HtmlElement anchor_monthNode, String year, String month) throws CrawlException {
		try {
			HtmlPage monthResponse = anchor_monthNode.click();
			HtmlElement msglistElement = monthResponse.getHtmlElementById("msglist");
			HtmlTable table_msgList = (HtmlTable) msglistElement;
			List<HtmlTableBody> tbody_msgslist = table_msgList.getBodies();
			HtmlTableBody tbody_msgList = tbody_msgslist.get(0);
			
			for (final HtmlTableRow row : tbody_msgList.getRows()) {
				for (final HtmlTableCell td_msgElement : row.getCells()) {
					DomAttr attr = td_msgElement.getAttributeNode(Constant.CLASS);
					
					if(attr.getNodeValue().equals(Constant.SUBJECT)) {
						DomNodeList<HtmlElement> anchor_msgNodes = td_msgElement.getElementsByTagName(Constant.TAG_A);
						if(anchor_msgNodes.size() > 0) {
							currentMsgCount += 1;
							
							// Read emails
							extractEmailContent(anchor_msgNodes, year, month);
						}
					}
				}
			}
			return msglistElement;
		} catch (ElementNotFoundException | IOException e) {
			logger.error("Crawler - extract list of emails failed", e); 
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("Crawler - extract list of emails failed", e);
			throw new CrawlException(e);
		}
		
	}
	
	/**
	 * Read emails
	 * @param tdElements
	 * @throws IOException
	 */
	private void extractEmailContent(DomNodeList<HtmlElement> emailAnchorNodes, String year, String month) throws CrawlException {
		try {
			String emailAddress = null;
			String emailContent = null;
			HtmlAnchor anchor = null; 
			HtmlPage emailResponse = null;
			StringBuffer fileNameBuffer = null;
			String fileName = null;
			String emailSentDate = null;
			
			for (HtmlElement emailAnchor : emailAnchorNodes) {
				anchor = (HtmlAnchor) emailAnchor;
				emailAddress = anchor.getHrefAttribute();
				emailResponse = anchor.click();
				emailContent = emailResponse.asXml();
				
				// Get email sent date
				emailSentDate = parser.parseEmailSentDate(emailResponse);
				
				// Write email content to a file: <emailAddress>_<emailSentDate>
				fileNameBuffer = new StringBuffer();
				fileName = fileNameBuffer.append(emailAddress).append(Constant.UNDERSCORE).append(emailSentDate).toString();
				FileUtil.storageEmail(fileName, emailContent, year, month);

				logger.debug("Email content: " + emailContent);
				logger.debug("------------------------------");
			}
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("Crawler - extract email content failed", e);
			throw new CrawlException(e);
		}
	}
	
}
