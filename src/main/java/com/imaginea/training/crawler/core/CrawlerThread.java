package com.imaginea.training.crawler.core;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
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

public class CrawlerThread implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CrawlerThread.class);

	private WebClient webClient = null;

	private HtmlPage page = null;
	
	private HtmlElement table_yearElement = null;
	private HtmlTableBody tbody_year = null;
	
	private int currentMsgCount = 0;
	
	private String name;
	
	private String year;
	
	private String month;
	
	private Controller controller;
	
	private Parser parser;

	public CrawlerThread(Parser parser, Controller controller, String month, String year) {
		logger.debug("year:" + year + ",month:" + month);
		//this.tr_monthNode = tr_monthNode;
		this.parser = parser;
		this.controller = controller;
		this.month = month;
		this.year = year;

		
	}
	
	private void init() {
		/*this.parser = new Parser();
		this.controller = new Controller();*/
		this.webClient = new WebClient();
		this.page = parser.getPage(webClient);
	}
	
	@Override
	public void run() {
		init();
		processPage();	
	}
	
	/**
	 * Get table row <tr></tr> for the month
	 * @return
	 */
	private HtmlTableRow getTableRowForMonth() {
		this.table_yearElement = parser.parseTableForYear(page, year);
		HtmlTable table_yearList = (HtmlTable) table_yearElement;
		List<HtmlTableBody> tbody_yearlist = table_yearList.getBodies();
		this.tbody_year = tbody_yearlist.get(0);
		
		for (final HtmlTableRow row : this.tbody_year.getRows()) {
			List<HtmlElement> td_monthDateElement = row.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
			if(td_monthDateElement.get(0).asText().contains(month)) {
				return row;	
			}
		}
		return null;
	}
	
	/**
	 * Process page for the month
	 * @throws CrawlException
	 */
	public void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			HtmlTableRow tr_monthNode = getTableRowForMonth();
			DomNodeList<HtmlElement> anchor_monthNodes = tr_monthNode.getElementsByTagName(Constant.TAG_A);
			for (HtmlElement anchor_monthNode : anchor_monthNodes) {
				if(anchor_monthNode.getAttribute(Constant.HREF).contains("mbox/thread") && anchor_monthNode.getAttribute(Constant.HREF).contains(year)) {
					
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
						
						if(anchor_nextNode != null && anchor_nextNode.getNodeName().equals(Constant.TAG_A) && anchor_nextNode.asText().contains(Constant.NEXT)) {
							msglistElement = extractListOfEmailsFromPage(anchor_nextNode, year, month);
						} else {
							isNextPageAvailable = false;	
						}
					} 

					// Log information
					if(logger.isInfoEnabled() || logger.isDebugEnabled()) {
						logger.info("{} {} emails count: {}" , month, year, currentMsgCount);
						long endTime = System.currentTimeMillis();
						logger.debug("Duration - Seconds : " + (endTime-startTime)/1000 + ", Minutes: " + (endTime-startTime)/(1000*60)); 
						logger.debug("--------");
					}
				}
			}
		} catch(CrawlException e) {
			throw e;
		} catch(Exception e) {
			logger.error("processPage failed", e);
			throw new CrawlException(e);
		}
	}
	
	/**
	 * 
	 * @param anchor_monthNode
	 * @return
	 * @throws CrawlException
	 */
	public HtmlElement extractListOfEmailsFromPage(HtmlElement anchor_monthNode, String year, String month) throws CrawlException {
		try {
			HtmlAnchor anchor = (HtmlAnchor) anchor_monthNode;
			HtmlPage monthResponse = anchor.click();
			HtmlElement msglistElement = monthResponse.getHtmlElementById(Constant.MSGLIST);
			HtmlTable table_msgList = (HtmlTable) msglistElement;
			List<HtmlTableBody> tbody_msgslist = table_msgList.getBodies();
			HtmlTableBody tbody_msg = tbody_msgslist.get(0);
			
			for (final HtmlTableRow tr_msg : tbody_msg.getRows()) {
				for (final HtmlTableCell td_msg : tr_msg.getCells()) {
					DomAttr td_msg_class = td_msg.getAttributeNode(Constant.CLASS);
					
					if(td_msg_class.getNodeValue().equals(Constant.SUBJECT)) {
						DomNodeList<HtmlElement> anchor_msgNodes = td_msg.getElementsByTagName(Constant.TAG_A);
						if(anchor_msgNodes.size() > 0) {
							currentMsgCount += 1;	
							extractEmailContent(anchor_msgNodes, year, month);
						}
					}
				}
			}
			return msglistElement;
		} catch (ElementNotFoundException | IOException e) {
			logger.error("extract list of emails from page failed", e); 
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("extract list of emails failed", e);
			throw new CrawlException(e);
		}
		
	}
	
	/**
	 * Read emails
	 * @param tdElements
	 * @throws IOException
	 */
	public void extractEmailContent(DomNodeList<HtmlElement> emailAnchorNodes, String year, String month) throws CrawlException {
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
				emailSentDate = parser.parseEmailSentDate(emailResponse);
				
				// Write email content to a file: <emailAddress>_<emailSentDate>
				fileNameBuffer = new StringBuffer();
				fileName = fileNameBuffer.append(emailAddress).append(Constant.UNDERSCORE).append(emailSentDate).toString();
				FileUtil.storageEmail(fileName, emailContent, year, month);
				logger.debug("Email content: " + emailContent);
			}	
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("extract email content failed", e);
			throw new CrawlException(e);
		}
	}

	public int getCurrentMsgCount() {
		return currentMsgCount;
	}

	public void setCurrentMsgCount(int currentMsgCount) {
		this.currentMsgCount = currentMsgCount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getYear() {
		return year;
	}

	/*public void setYear(String year) {
		this.year = year;
	}*/

	public String getMonth() {
		return month;
	}

	/*public void setMonth(String month) {
		this.month = month;
	}*/

	public Controller getController() {
		return controller;
	}

	public Parser getParser() {
		return parser;
	}

}
