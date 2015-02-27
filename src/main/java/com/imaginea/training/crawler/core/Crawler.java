package com.imaginea.training.crawler.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.exception.CrawlException;
import com.imaginea.training.crawler.parser.HtmlPageParser;
import com.imaginea.training.crawler.parser.Parser;

/**
 * 
 * @author manojm
 *
 */
public class Crawler implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	private String name;
	
	private String year;
			
	private Controller controller;
	
	private Parser parser;
	
	private int m_elapsed = 0;
	
	private int m_length = Config.SHUTDOWN_TIME;
	
	private boolean shutdown = false;
	
	private boolean sleep = false;
	
	private boolean terminate = false;
	
	private Map<String, Boolean> shutdownMap = new LinkedHashMap<String, Boolean>();
	
	private List<String> totalMonthsCompletedList = new ArrayList<String>();

	private String months[] = {
		"Dec", "Nov", "Oct", "Sep", "Aug", "Jul",
		"Jun", "May", "Apr", "Mar", "Feb", "Jan"
	};
	
	public Crawler(String year) {
		this.year = year;
		this.name = year;
	}
	
	private void init() {
		this.parser = new HtmlPageParser();
		this.controller = new Controller();
	}

	@Override
	public void run() {
		try {
			init();
			processPage();
			logger.info("CRAWLER RUN END"); 
		} catch (CrawlException e) {
			logger.error("run failed", e);
		}
	}

	/**
	 * Process page for the year
	 * @throws CrawlException
	 */
	private void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			// Get the html content for all input years
			String month = null; 
			String msgCount = null;
			final WebClient webClient = new WebClient();
			webClient.getOptions().setTimeout(Config.CONNECTION_TIMEOUT);
			webClient.setJavaScriptTimeout(Config.JAVASCRIPT_TIMEOUT); 
			final HtmlPage page = parser.getPage(webClient);
			
			if(page != null && controller.getNetUtil().isInternetReachable()) {
				HtmlElement table_yearElement = parser.parseTableForYear(page, this.year);
				HtmlTable table_year = (HtmlTable) table_yearElement;
				HtmlTableBody tbody_year = table_year.getBodies().get(0);

				// Months
				for (final HtmlTableRow tr_monthNode : tbody_year.getRows()) {

					if(logger.isInfoEnabled() || logger.isDebugEnabled()){
						List<HtmlElement> td_monthDateElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
						month = td_monthDateElement.get(0).asText();
						month = month.substring(0, month.indexOf(Constant.SPACE));
						List<HtmlElement> td_monthMsgcountElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.MSGCOUNT);
						msgCount = td_monthMsgcountElement.get(0).asText();
						logger.info("year:" + year + " month:" + month + " emails: " + msgCount);
					}
					
					// Month threads
					CrawlerThread crawlerThread = new CrawlerThread(this, parser, controller, month, year);
		        	crawlerThread.setName(month);
		        	crawlerThread.setTotalMsgCount(Integer.parseInt(msgCount));
					Thread child = new Thread(crawlerThread);
					child.start();
				}
				
				// Monitor thread
				CrawlMonitor crawMonitor = new CrawlMonitor(this);
				Thread monitor = new Thread(crawMonitor);
				monitor.start();

				//webClient.closeAllWindows();
			} else {
				while(true && !this.isShutdown()) {
					if(controller.getNetUtil().isInternetReachable()) {
						this.setM_elapsed(0);
						break;
					} else {
						synchronized (this) {
							if(this.getM_elapsed() == this.getM_length()) {
								shutdown();
							} else {
								logger.info("SLEEP {}", new Date());
								Thread.sleep(Config.SLEEP_INTERVAL);
								this.setM_elapsed(this.getM_elapsed() + Config.SLEEP_INTERVAL);	
							}	
						}
					}
				}
				if(controller.getNetUtil().isInternetReachable()) {
					processPage();	
				}
			}
			logger.debug("processPage end");
		} catch (FailingHttpStatusCodeException e) {
			logger.error("Http status code exception", e);
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("processPage failed", e);
			throw new CrawlException(e);
		}
	}
	
	private void shutdown() {
		if(!this.getShutdownMap().containsKey(this.name)) {
			controller.getFileUtil().createFile(Config.DIR_DOWNLOAD_EMAILS, this.name, Config.STATE_INITIALIZE + Constant.SPACE + String.valueOf(0));
			//this.getShutdownMap().put(this.name, true);	
			this.setShutdown(true);
			this.setTerminate(true);
			logger.info("Shutdown Crawler"); 
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getMonths() {
		return months;
	}

	public String getYear() {
		return year;
	}

	public synchronized int getM_elapsed() {
		return m_elapsed;
	}

	public synchronized void setM_elapsed(int m_elapsed) {
		this.m_elapsed = m_elapsed;
	}

	public int getM_length() {
		return m_length;
	}

	public void setM_length(int m_length) {
		this.m_length = m_length;
	}

	public synchronized boolean isShutdown() {
		return shutdown;
	}

	public synchronized void setShutdown(boolean shutdown) {
		this.shutdown = shutdown;
	}

	public synchronized Map<String, Boolean> getShutdownMap() {
		return shutdownMap;
	}

	public synchronized void setShutdownMap(Map<String, Boolean> shutdownMap) {
		this.shutdownMap = shutdownMap;
	}

	public synchronized boolean isSleep() {
		return sleep;
	}

	public synchronized void setSleep(boolean sleep) {
		this.sleep = sleep;
	}

	public boolean isTerminate() {
		return terminate;
	}

	public void setTerminate(boolean terminate) {
		this.terminate = terminate;
	}

	public synchronized List<String> getTotalMonthsCompletedList() {
		return totalMonthsCompletedList;
	}

	public synchronized void setTotalMonthsCompletedList(List<String> totalMonthsCompletedList) {
		this.totalMonthsCompletedList = totalMonthsCompletedList;
	}
}
