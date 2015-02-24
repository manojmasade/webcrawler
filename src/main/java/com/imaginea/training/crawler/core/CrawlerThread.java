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
	
	private HtmlTableRow tr_monthNode;
	
	private Controller controller;
	
	private Parser parser;

	public CrawlerThread(HtmlTableRow tr_monthNode, Parser parser, Controller controller, String month, String year) {
		//this.tr_monthNode = tr_monthNode;
		this.parser = parser;
		this.controller = controller;
		this.month = month;
		this.year = year;
		this.webClient = new WebClient();
		this.page = parser.getPage(webClient);
		this.table_yearElement = parser.parseTableForYear(page, year);
		HtmlTable table_yearList = (HtmlTable) table_yearElement;
		List<HtmlTableBody> tbody_yearlist = table_yearList.getBodies();
		this.tbody_year = tbody_yearlist.get(0);
		
		for (final HtmlTableRow row : this.tbody_year.getRows()) {
			List<HtmlElement> td_monthDateElement = row.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
			if(td_monthDateElement.get(0).asText().equals(month)) {
				//logger.info("SOURCE:" + td_monthDateElement.get(0).asText() + ", TARGET:" + month);
				this.tr_monthNode = row;	
			}
		}
		
	}
	
	private void init() {
		//this.parser = new Parser();
		//this.controller = new Controller();
	}
	
	@Override
	public void run() {
		//init();
		processPage();	
	}
	
	
	public void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		//synchronized(this) {
		try {
			// href links for msgs by thread (subject)
			DomNodeList<HtmlElement> anchor_monthNodes = this.getTR_monthNode().getElementsByTagName(Constant.TAG_A);
			for (HtmlElement anchor_monthNode : anchor_monthNodes) {
				if(anchor_monthNode.getAttribute("href").contains("mbox/thread") && anchor_monthNode.getAttribute(Constant.HREF).contains(Constant.YEAR_2014)) {
					
					// Get the list of emails from 1st page
					long startTime = System.currentTimeMillis();
					currentMsgCount = 0;
					HtmlElement msglistElement = null;
					//synchronized (this) {
						msglistElement = extractListOfEmailsFromPage(anchor_monthNode, year, month);	
					//}
					boolean isNextPageAvailable = true;
					
					// Get the list of emails from all pages
					while (isNextPageAvailable) {
						List<HtmlElement> th_pagesElements = msglistElement.getElementsByAttribute(Constant.TH, Constant.CLASS, Constant.PAGES);
						HtmlElement th_pageElement = th_pagesElements.get(0);
						HtmlElement anchor_nextNode = (HtmlElement)th_pageElement.getLastElementChild();
						
						if(anchor_nextNode.getNodeName().equals(Constant.TAG_A) && anchor_nextNode.asText().contains(Constant.NEXT)) {
							//synchronized (this) {
								msglistElement = extractListOfEmailsFromPage(anchor_nextNode, year, month);
							//}
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
		} catch(CrawlException e) {
			throw e;
		} catch(Exception e) {
			logger.error("Crawler - processPage failed", e);
			throw new CrawlException(e);
		}
		//}
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
			//logger.info("URL : " + anchor.getHrefAttribute() + "," + anchor.getBaseURI());
			//page = webClient.getPage(anchor_monthNode.getBaseURI());
			HtmlPage monthResponse = anchor.click();
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
							//synchronized (this) {
								currentMsgCount += 1;	
							//}
							
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
					
					// Get email sent date
					//synchronized (this) {
						emailSentDate = parser.parseEmailSentDate(emailResponse);
					//}
					
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

	public HtmlTableRow getTR_monthNode() {
		return tr_monthNode;
	}

	public Controller getController() {
		return controller;
	}

	public Parser getParser() {
		return parser;
	}

}
