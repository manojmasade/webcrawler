package com.imaginea.training.crawler.core;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.TextPage;
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

public class CrawlerThread extends AbstractCrawler implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CrawlerThread.class);
	
	//@Autowired
	private Config config;

	private Crawler crawler;

	private Controller controller;
	
	private Parser parser;
	
	private int currentMsgCount = 0;
	
	private int beginCrawlIndex = 0;
	
	private String name;
	
	private String year;
	
	private String month;
	
	private int totalMsgCount = 0;
	

	public CrawlerThread(Crawler crawler, String month, String year) {
		logger.debug("year:" + year + ",month:" + month);
		this.crawler = crawler;
		this.month = month;
		this.year = year;
	}
	
	@Override
	public void run() {
		processCrawl();
	}
	
	/**
	 * Get table row <tr></tr> for the month
	 * @return
	 */
	private HtmlTableRow getTableRowForMonth(HtmlPage page) {
		
		if(page != null) {
			HtmlElement table_yearElement = parser.parseTableForYear(page, year);
			HtmlTable table_yearList = (HtmlTable) table_yearElement;
			List<HtmlTableBody> tbody_yearlist = table_yearList.getBodies();
			HtmlTableBody tbody_year = tbody_yearlist.get(0);
			
			for (final HtmlTableRow result : tbody_year.getRows()) {
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
			final WebClient webClient = new WebClient();
			webClient.getOptions().setTimeout(config.getConnectionTimeout());
			webClient.setJavaScriptTimeout(config.getJavascriptTimeout());
			final HtmlPage page = parser.getPage(webClient);
			HtmlTableRow tr_monthNode = getTableRowForMonth(page);
		
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
				// To mark the completion trace
				handleShutdown(crawler);
			}
		} catch(CrawlException e) {
			handleShutdown(crawler);
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
	private HtmlElement extractListOfEmailsFromPage(HtmlElement anchor_monthNode, String year, String month) throws CrawlException {
		try {
			HtmlAnchor anchor = (HtmlAnchor) anchor_monthNode;
			HtmlPage monthResponse = null;
			
			if(!this.isShutdown() && controller.getNetUtil().isInternetReachable()) {
				monthResponse = anchor.click();
				HtmlElement result = monthResponse.getHtmlElementById(Constant.MSGLIST);
				HtmlTable table_msgList = (HtmlTable) result;
				List<HtmlTableBody> tbody_msgslist = table_msgList.getBodies();
				HtmlTableBody tbody_msg = tbody_msgslist.get(0);
				List<HtmlTableRow> tr_msgs = tbody_msg.getRows();
				
				for (int i = this.beginCrawlIndex; (i < tr_msgs.size() && !this.isShutdown()); i++) {
					final HtmlTableRow tr_msg = tr_msgs.get(i);
					List<HtmlTableCell> td_msgs = tr_msg.getCells();
					final HtmlTableCell td_msg = td_msgs.get(1); 

					if(!this.isShutdown()) {
						if(this.totalMsgCount > 0) {
							DomNodeList<HtmlElement> anchor_msgNodes = td_msg.getElementsByTagName(Constant.TAG_A);
							if(anchor_msgNodes.size() > 0) {
								logger.debug("Month: {}, CurrentMsgCount: {}, TotalMsgCount: {}", month, this.currentMsgCount, this.totalMsgCount);
								HtmlAnchor anchor_msgNode = (HtmlAnchor) anchor_msgNodes.get(0);
								extractEmailContent(anchor_msgNode, year, month);
							}
						} else {
							handleShutdown(crawler);
						}
					}
				}
				return result;
			}
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown(crawler);
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
	private void extractEmailContent(HtmlAnchor emailAnchorNode, String year, String month) throws CrawlException {
		try {
			String emailAddress = null;
			String emailContent = null;
			HtmlPage emailResponse = null;
			StringBuffer fileNameBuffer = null;
			String fileName = null;
			String emailSentDate = null;
			emailAddress = emailAnchorNode.getHrefAttribute();
			
			if(!this.isShutdown() && controller.getNetUtil().isInternetReachable()) {
				emailResponse = emailAnchorNode.click();
				emailSentDate = parser.parseEmailSentDate(emailResponse);
				
				// Fetch raw email content as text
				emailContent = extractRawEmailContent(emailResponse);
				
				// Write email content to a file: <emailAddress>_<emailSentDate>
				fileNameBuffer = new StringBuffer();
				fileName = fileNameBuffer.append(emailAddress).append(Constant.UNDERSCORE).append(emailSentDate).toString();
				controller.getFileUtil().downloadEmail(fileName, emailContent, year, month);
				//logger.debug("Email content: " + emailContent);
				this.currentMsgCount += 1;	
				this.totalMsgCount -= 1;
			} else {
				handleShutdown(crawler);
			}
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown(crawler);
			} else {
				logger.error("extract email content failed", e);
				throw new CrawlException(e);
			}
		} 
	}
	
	/**
	 * 
	 * @param emailResponse
	 * @throws CrawlException
	 */
	private String extractRawEmailContent(HtmlPage emailResponse) throws CrawlException {
		String result = null;
		try {
			if(!this.isShutdown() && controller.getNetUtil().isInternetReachable()) {
				HtmlElement table_msgviewElement = emailResponse.getHtmlElementById(Constant.MSGVIEW);
				List<HtmlElement> tr_rawElements = table_msgviewElement.getElementsByAttribute(Constant.TR, Constant.CLASS, Constant.RAW);
				HtmlElement tr_rawElement = tr_rawElements.get(0);
				HtmlTableCell td_rawMsg = (HtmlTableCell)tr_rawElement.getLastChild();
				
				DomNodeList<HtmlElement> anchor_rawNodes = td_rawMsg.getElementsByTagName(Constant.TAG_A);
				HtmlAnchor anchor_rawNode = (HtmlAnchor) anchor_rawNodes.get(0);
				TextPage rawEmailResponse = anchor_rawNode.click();
				result = rawEmailResponse.getContent();
			} else {
				handleShutdown(crawler);
			}
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown(crawler);
			} else {
				logger.error("extract email content failed", e);
				throw new CrawlException(e);
			}		
		}
		return result;
	}

	/**
	 * Save the crawler threads information to file for resume operation
	 */
	@Override
	public void storeCrawlersData() {
		logger.debug("storeCrawlersData");
		
		if(this.totalMsgCount == 0) {
			if(!crawler.getTotalMonthsCompletedList().contains(month)) {
				crawler.getTotalMonthsCompletedList().add(month);	
			}
		} else {
			if(!crawler.getShutdownMap().containsKey(this.name)) {
				crawler.getShutdownMap().put(this.name, true);
				this.setShutdown(true);
				logger.info("Shutdown CrawlerThread");
			}
		}	
		controller.getFileUtil().createFile(config.getEmailsDownloadDir(), config.getCrawlFileName(), Constant.STATE_RUNNING);
		controller.getFileUtil().createFile(config.getEmailsDownloadDir(), this.name, String.valueOf(currentMsgCount));
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

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
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

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

}
