package com.imaginea.training.crawler.core;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.WebClient;
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

public class CrawlerThread implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CrawlerThread.class);

	private Crawler crawler;
	
	private WebClient webClient = null;

	private HtmlPage page = null;
	
	private HtmlElement table_yearElement = null;
	
	private HtmlTableBody tbody_year = null;
	
	private int currentMsgCount = 0;
	
	private int beginCrawlIndex = 0;
	
	private String name;
	
	private String year;
	
	private String month;
	
	private Controller controller;
	
	private Parser parser;
	
	private int totalMsgCount = 0;
	

	public CrawlerThread(Crawler crawler, Parser parser, Controller controller, String month, String year) {
		logger.debug("year:" + year + ",month:" + month);
		this.crawler = crawler;
		this.parser = parser;
		this.controller = controller;
		this.month = month;
		this.year = year;
	}
	
	private void init() {
		this.webClient = new WebClient();
		this.webClient.getOptions().setTimeout(Config.CONNECTION_TIMEOUT);
		webClient.setJavaScriptTimeout(Config.JAVASCRIPT_TIMEOUT);
	}
	
	@Override
	public void run() {
		init();
		processCrawl();
	}
	
	/**
	 * Get table row <tr></tr> for the month
	 * @return
	 */
	private HtmlTableRow getTableRowForMonth() {
		if(page != null) {
			this.table_yearElement = parser.parseTableForYear(page, year);
			HtmlTable table_yearList = (HtmlTable) table_yearElement;
			List<HtmlTableBody> tbody_yearlist = table_yearList.getBodies();
			this.tbody_year = tbody_yearlist.get(0);
			
			for (final HtmlTableRow result : this.tbody_year.getRows()) {
				List<HtmlElement> td_monthDateElement = result.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
				logger.debug("td_monthDateElement: {}", td_monthDateElement);
				if(td_monthDateElement.get(0).asText().contains(month)) {
					return result;	
				}
			}	
		}
		return null;
	}
	
	/**
	 * Process page for the month
	 * @throws CrawlException
	 */
	public void processCrawl() throws CrawlException {
		
		try {
			this.page = parser.getPage(webClient);
			HtmlTableRow tr_monthNode = getTableRowForMonth();
			if(tr_monthNode != null) {
				DomNodeList<HtmlElement> anchor_monthNodes = tr_monthNode.getElementsByTagName(Constant.TAG_A);
				for (HtmlElement anchor_monthNode : anchor_monthNodes) {
					if(anchor_monthNode.getAttribute(Constant.HREF).contains("mbox/thread") && anchor_monthNode.getAttribute(Constant.HREF).contains(year)) {
						
						// Get the list of emails from 1st page
						long startTime = System.currentTimeMillis();
						beginCrawlIndex = this.currentMsgCount;
						HtmlElement msglistElement = extractListOfEmailsFromPage(anchor_monthNode, year, month);	
						boolean isNextPageAvailable = true;
						
						// Get the list of emails from all pages
						while (isNextPageAvailable) {
							if(msglistElement != null) {
								List<HtmlElement> th_pagesElements = msglistElement.getElementsByAttribute(Constant.TH, Constant.CLASS, Constant.PAGES);
								HtmlElement th_pageElement = th_pagesElements.get(0);
								HtmlElement anchor_nextNode = (HtmlElement)th_pageElement.getLastElementChild();
								
								if(anchor_nextNode != null && anchor_nextNode.getNodeName().equals(Constant.TAG_A) && anchor_nextNode.asText().contains(Constant.NEXT)) {
									this.beginCrawlIndex = 0;
									msglistElement = extractListOfEmailsFromPage(anchor_nextNode, year, month);
								} else {
									isNextPageAvailable = false;	
								}	
							}
						} 
						
						// Log information
						if(logger.isInfoEnabled() || logger.isDebugEnabled()) {
							logger.info("{} {} emails downloaded: {}" , month, year, this.currentMsgCount);
							long endTime = System.currentTimeMillis();
							logger.debug("Duration - Seconds : " + (endTime-startTime)/1000 + ", Minutes: " + (endTime-startTime)/(1000*60)); 
							logger.debug("--------");
						}
					}
				}
			}
		} catch(CrawlException e) {
			handleShutdown();
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
			HtmlPage monthResponse = null;
			
			if(!crawler.isShutdown() && controller.getNetUtil().isInternetReachable()) {
				monthResponse = anchor.click();
				HtmlElement result = monthResponse.getHtmlElementById(Constant.MSGLIST);
				HtmlTable table_msgList = (HtmlTable) result;
				List<HtmlTableBody> tbody_msgslist = table_msgList.getBodies();
				HtmlTableBody tbody_msg = tbody_msgslist.get(0);
				List<HtmlTableRow> tr_msgs = tbody_msg.getRows();
				
				for (int i = this.beginCrawlIndex; (i < tr_msgs.size() && !crawler.isShutdown()); i++) {
					final HtmlTableRow tr_msg = tr_msgs.get(i);
					List<HtmlTableCell> td_msgs = tr_msg.getCells();
					final HtmlTableCell td_msg = td_msgs.get(1); 

					if(!crawler.isShutdown()) {
						if(this.totalMsgCount > 0) {
							DomNodeList<HtmlElement> anchor_msgNodes = td_msg.getElementsByTagName(Constant.TAG_A);
							if(anchor_msgNodes.size() > 0) {
								logger.debug("currentMsgCount:" + this.currentMsgCount + ", totalMsgCount:" + this.totalMsgCount);
								HtmlAnchor anchor_msgNode = (HtmlAnchor) anchor_msgNodes.get(0);
								extractEmailContent(anchor_msgNode, year, month);
							}
						} else {
							handleShutdown();
						}
					}
				}
				return result;
			}
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown();
			} else {
				logger.error("extract list of emails failed", e);
				throw new CrawlException(e);
			}
		} 
		return null;
	}
	
	/**
	 * Read emails
	 * @param tdElements
	 * @throws IOException
	 */
	public void extractEmailContent(HtmlAnchor emailAnchorNode, String year, String month) throws CrawlException {
		try {
			String emailAddress = null;
			String emailContent = null;
			HtmlPage emailResponse = null;
			StringBuffer fileNameBuffer = null;
			String fileName = null;
			String emailSentDate = null;
			emailAddress = emailAnchorNode.getHrefAttribute();
			
			if(!crawler.isShutdown() && controller.getNetUtil().isInternetReachable()) {
				emailResponse = emailAnchorNode.click();
				emailContent = emailResponse.asXml();
				emailSentDate = parser.parseEmailSentDate(emailResponse);
				
				// Write email content to a file: <emailAddress>_<emailSentDate>
				fileNameBuffer = new StringBuffer();
				fileName = fileNameBuffer.append(emailAddress).append(Constant.UNDERSCORE).append(emailSentDate).toString();
				controller.getFileUtil().downloadEmail(fileName, emailContent, year, month);
				logger.debug("Email content: " + emailContent);
				this.currentMsgCount += 1;	
				this.totalMsgCount -= 1;
			} else {
				handleShutdown();
			}
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown();
			} else {
				logger.error("extract email content failed", e);
				throw new CrawlException(e);
			}
		} 
	}

	/**
	 * Handle shutdown process
	 */
	private void handleShutdown() {
		logger.debug("handleShutdown {}", month);
		
		while(true && !crawler.isShutdown()) {
			if(controller.getNetUtil().isInternetReachable()) {
				crawler.setElapsedDuration(0);
				break;
			} else {
				try {
					synchronized (this) {
						if(crawler.getElapsedDuration() >= crawler.getShutdownDuration()) {
							break;
						} else {
							if(null == crawler.getLockApplied()) {
								crawler.setLockApplied(this);	
							}
							if(this == crawler.getLockApplied()) {
								logger.info("Sleep {}", new Date());
								Thread.sleep(Config.SLEEP_INTERVAL);
								crawler.setElapsedDuration(crawler.getElapsedDuration() + Config.SLEEP_INTERVAL);	
							}
						}	
					}
				} catch (InterruptedException e1) {}  
			}
		}
		storeCrawlersData();
	}
	
	/**
	 * Save the crawler threads information to file for resume operation
	 */
	private void storeCrawlersData() {
		logger.debug("storeCrawlersData");
		
		if(this.totalMsgCount == 0) {
			if(!crawler.getTotalMonthsCompletedList().contains(month)) {
				crawler.getTotalMonthsCompletedList().add(month);	
			}
		} else {
			if(!crawler.getShutdownMap().containsKey(this.name)) {
				crawler.getShutdownMap().put(this.name, true);
				crawler.setShutdown(true);
				logger.info("Shutdown CrawlerThread");
			}
		}	
		controller.getFileUtil().createFile(Config.DIR_DOWNLOAD_EMAILS, Config.FILE_CRAWL, Config.STATE_RUNNING);
		controller.getFileUtil().createFile(Config.DIR_DOWNLOAD_EMAILS, this.name, String.valueOf(currentMsgCount));
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

	public String getMonth() {
		return month;
	}

	public Controller getController() {
		return controller;
	}

	public Parser getParser() {
		return parser;
	}

	public int getTotalMsgCount() {
		return totalMsgCount;
	}

	public void setTotalMsgCount(int totalMsgCount) {
		this.totalMsgCount = totalMsgCount;
	}

}
