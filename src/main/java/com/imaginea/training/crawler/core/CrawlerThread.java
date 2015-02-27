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

public class CrawlerThread implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CrawlerThread.class);

	private Crawler crawler;
	
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
		/*this.parser = new Parser();
		this.controller = new Controller();*/
		this.webClient = new WebClient();
		this.webClient.getOptions().setTimeout(Config.CONNECTION_TIMEOUT);
		webClient.setJavaScriptTimeout(Config.JAVASCRIPT_TIMEOUT);
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
	public void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			HtmlTableRow tr_monthNode = getTableRowForMonth();
			if(tr_monthNode != null) {
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
							if(msglistElement != null) {
								List<HtmlElement> th_pagesElements = msglistElement.getElementsByAttribute(Constant.TH, Constant.CLASS, Constant.PAGES);
								HtmlElement th_pageElement = th_pagesElements.get(0);
								HtmlElement anchor_nextNode = (HtmlElement)th_pageElement.getLastElementChild();

								if(anchor_nextNode != null && anchor_nextNode.getNodeName().equals(Constant.TAG_A) && anchor_nextNode.asText().contains(Constant.NEXT)) {
									msglistElement = extractListOfEmailsFromPage(anchor_nextNode, year, month);
								} else {
									isNextPageAvailable = false;	
								}	
							}
						} 

						// Log information
						if(logger.isInfoEnabled() || logger.isDebugEnabled()) {
							crawler.getTotalMonthsCompletedList().add(month);
							logger.info("{} {} emails downloaded: {}" , month, year, currentMsgCount);
							long endTime = System.currentTimeMillis();
							logger.debug("Duration - Seconds : " + (endTime-startTime)/1000 + ", Minutes: " + (endTime-startTime)/(1000*60)); 
							logger.debug("--------");
						}
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
			HtmlPage monthResponse = null;
			
			if(controller.getNetUtil().isInternetReachable()) {
				monthResponse = anchor.click();
				HtmlElement result = monthResponse.getHtmlElementById(Constant.MSGLIST);
				HtmlTable table_msgList = (HtmlTable) result;
				List<HtmlTableBody> tbody_msgslist = table_msgList.getBodies();
				HtmlTableBody tbody_msg = tbody_msgslist.get(0);
				List<HtmlTableRow> tr_msgs = tbody_msg.getRows();

				for (int i = 0; (i < tr_msgs.size() && !crawler.isShutdown()); i++) {
					final HtmlTableRow tr_msg = tr_msgs.get(i);
					List<HtmlTableCell> td_msgs = tr_msg.getCells();
					  
					for (int j = 0; (j < td_msgs.size() && !crawler.isShutdown()); j++) {
						final HtmlTableCell td_msg = td_msgs.get(j); 
						
						if(!crawler.isShutdown()) {
							if(this.totalMsgCount > 0 && controller.getNetUtil().isInternetReachable()) {
								DomAttr td_msg_class = td_msg.getAttributeNode(Constant.CLASS);
								if(td_msg_class.getNodeValue().equals(Constant.SUBJECT)) {
									DomNodeList<HtmlElement> anchor_msgNodes = td_msg.getElementsByTagName(Constant.TAG_A);
									if(anchor_msgNodes.size() > 0) {
										logger.debug("currentMsgCount:" + currentMsgCount + ", this.totalMsgCount:" + this.totalMsgCount); 
										if(controller.getNetUtil().isInternetReachable()) {
											extractEmailContent(anchor_msgNodes, year, month);
										}
									}
								}
							} else {
								handleShutdown();
							}
						}
					}
				}
				return result;
			} else {
				// might not require
				logger.info("extractListOfEmailsFromPage - Not Reachable");
				storeCrawlersData();
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
				
				if(!crawler.isShutdown()) {
					if(controller.getNetUtil().isInternetReachable()) {
						emailResponse = anchor.click();
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
						logger.info("extractEmailContent - Not Reachable");
						handleShutdown();
					}
				} else {
					storeCrawlersData();
				}
			}	
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown();
				if(crawler.isShutdown()) {
					storeCrawlersData();
				}
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
		while(true && !crawler.isShutdown()) {
			if(controller.getNetUtil().isInternetReachable()) {
				crawler.setElapsedDuration(0);
				break;
			} else {
				try {
					synchronized (this) {
						if(crawler.getElapsedDuration() == crawler.getShutdownDuration()) {
							storeCrawlersData();
						} else {
							logger.info("SLEEP {}", new Date());
							Thread.sleep(Config.SLEEP_INTERVAL);
							crawler.setElapsedDuration(crawler.getElapsedDuration() + Config.SLEEP_INTERVAL);	
						}	
					}
				} catch (InterruptedException e1) {}  
			}
		}
	}
	
	/**
	 * Save the crawler threads information to file for resume operation
	 */
	private void storeCrawlersData() {
		if(!crawler.getShutdownMap().containsKey(this.name)) {
			controller.getFileUtil().createFile(Config.DIR_DOWNLOAD_EMAILS, this.name, Config.STATE_RUNNING + Constant.SPACE + String.valueOf(currentMsgCount));
			crawler.getShutdownMap().put(this.name, true);	
			crawler.setShutdown(true);
			logger.info("Shutdown CrawlerThread"); 
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
