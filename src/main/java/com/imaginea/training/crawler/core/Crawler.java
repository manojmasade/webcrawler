package com.imaginea.training.crawler.core;

import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.exception.CrawlException;
import com.imaginea.training.crawler.parser.Parser;

/**
 * 
 * @author manojm
 *
 */
public class Crawler extends AbstractCrawler implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	private String year;
		
	@Autowired
	private Parser parser;
	
	@Autowired
	private Config config;
	
	private int elapsedDuration = 0;
	
	private int shutdownDuration = 0;
	
	private boolean exit = false;
	
	private boolean skipCrawling = false;
	
	private Object lockApplied;
	
	private Map<String, Boolean> shutdownMap = new LinkedHashMap<String, Boolean>();
	
	private Map<String, Integer> crawledMonthMsgCountMap = new LinkedHashMap<String, Integer>();
	
	private List<String> totalMonthsCompletedList = new ArrayList<String>();

	private String months[] = {
		"Dec", "Nov", "Oct", "Sep", "Aug", "Jul",
		"Jun", "May", "Apr", "Mar", "Feb", "Jan"
	};

	@Override
	public void run() {
		try {
			init();
			initMonitorThread();
			initCrawlData();
			
			if(!skipCrawling) {
				processCrawl();
			} else {
				logger.info("Skipping process as crawling is Complete"); 
				this.setExit(true);
			}
		} catch (CrawlException e) {
			logger.error("run failed", e);
		}
	}
	
	private void init() {
		setShutdownDuration(config.getShutdownTime());
	}
	
	private void initMonitorThread() {
		// Monitor thread
		CrawlMonitor crawlMonitor = new CrawlMonitor(this);
		crawlMonitor.setConfig(config);
		Thread monitor = new Thread(crawlMonitor);
		monitor.start();
	}

	/**
	 * Initialize crawl data with information from disk and fill up map for use by month threads 
	 */
	private void initCrawlData() {
		boolean initializeAllThreads = false;
		String crawl_monthMsgCount = null;
		int msgCount = 0;
		
		if(config.isResumeCrawling()) {
			String crawlStatus = getController().getFileUtil().getFileContent(config.getEmailsDownloadDir(), config.getCrawlFileName());
			if(crawlStatus != null){
				if(crawlStatus.equalsIgnoreCase(Constant.STATE_RUNNING)) {
					initializeAllThreads = true;
				} else if(crawlStatus.equalsIgnoreCase(Constant.STATE_COMPLETED)) {
					skipCrawling = true;
					return;
				}
			}
		}
		
		for (int i = 0; i < months.length; i++) {
			if(initializeAllThreads) {
				crawl_monthMsgCount = getController().getFileUtil().getFileContent(config.getEmailsDownloadDir(), months[i]);
				if(crawl_monthMsgCount != null) {
					msgCount = Integer.parseInt(crawl_monthMsgCount);
					crawledMonthMsgCountMap.put(months[i], msgCount);
				}	
			} else {
				crawledMonthMsgCountMap.put(months[i], msgCount);
			}
		}
	}

	/**
	 * Process page for the year
	 * @throws CrawlException
	 */
	private void processCrawl() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			// Get the html content for all input years
			String month = null; 
			String msgCount = null;
			final WebClient webClient = new WebClient();
			webClient.getOptions().setTimeout(config.getConnectionTimeout());
			webClient.setJavaScriptTimeout(config.getJavascriptTimeout()); 
			final HtmlPage page = parser.getPage(webClient);
			
			if(page != null && getController().getNetUtil().isInternetReachable()) {
				HtmlElement table_yearElement = parser.parseTableForYear(page, this.year);
				HtmlTable table_year = (HtmlTable) table_yearElement;
				HtmlTableBody tbody_year = table_year.getBodies().get(0);

				// Months
				for (final HtmlTableRow tr_monthNode : tbody_year.getRows()) {

					// Get month and msgCount
					List<HtmlElement> td_monthDateElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
					month = td_monthDateElement.get(0).asText();
					month = month.substring(0, month.indexOf(Constant.SPACE));
					List<HtmlElement> td_monthMsgcountElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.MSGCOUNT);
					msgCount = td_monthMsgcountElement.get(0).asText();
					
					// Create threads for each month
					if(crawledMonthMsgCountMap.get(month) != null) {
						int crawledMsgCount = crawledMonthMsgCountMap.get(month);
			        	int pendingTotalMsgCount = Integer.parseInt(msgCount) - crawledMsgCount;
			        	
						CrawlerThread crawlerThread = new CrawlerThread(this, month, year);
			        	crawlerThread.setName(month);
			        	crawlerThread.setParser(parser);
			        	crawlerThread.setController(getController());
			        	crawlerThread.setConfig(config);
			        	crawlerThread.setTotalMsgCount(pendingTotalMsgCount);
			        	crawlerThread.setCurrentMsgCount(crawledMsgCount);
						Thread child = new Thread(crawlerThread);
						child.start();	
					}
				}

				//webClient.closeAllWindows();
			} else {
				handleShutdown(this);
				if(getController().getNetUtil().isInternetReachable()) {
					processCrawl();	
				}
			}
			logger.debug("processPage end");
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown(this);
			} else {
				logger.error("processPage failed", e);
				throw new CrawlException(e);
			}
		}
	}

	/**
	 * Save the crawler threads information to file for resume operation
	 */
	@Override
	public void storeCrawlersData() {
		logger.debug("store crawlers data");
		if(!this.getShutdownMap().containsKey(this.year)) {
			getController().getFileUtil().createFile(config.getEmailsDownloadDir(), config.getCrawlFileName(), Constant.STATE_INITIALIZE);
			this.setShutdown(true);
			this.setExit(true);
			logger.info("Shutdown Crawler"); 
		}
	}
	
	public String[] getMonths() {
		return months;
	}

	public String getYear() {
		return year;
	}

	public synchronized int getElapsedDuration() {
		return elapsedDuration;
	}

	public synchronized void setElapsedDuration(int elapsedDuration) {
		this.elapsedDuration = elapsedDuration;
	}

	public int getShutdownDuration() {
		return shutdownDuration;
	}

	public void setShutdownDuration(int shutdownDuration) {
		this.shutdownDuration = shutdownDuration;
	}

	public synchronized Map<String, Boolean> getShutdownMap() {
		return shutdownMap;
	}

	public synchronized void setShutdownMap(Map<String, Boolean> shutdownMap) {
		this.shutdownMap = shutdownMap;
	}

	public synchronized boolean isExit() {
		return exit;
	}

	public synchronized void setExit(boolean terminate) {
		this.exit = terminate;
	}

	public synchronized List<String> getTotalMonthsCompletedList() {
		return totalMonthsCompletedList;
	}

	public synchronized void setTotalMonthsCompletedList(List<String> totalMonthsCompletedList) {
		this.totalMonthsCompletedList = totalMonthsCompletedList;
	}

	public synchronized Object getLockApplied() {
		return lockApplied;
	}

	public synchronized void setLockApplied(Object lockApplied) {
		this.lockApplied = lockApplied;
	}

	public Map<String, Integer> getCrawledMonthMsgCountMap() {
		return crawledMonthMsgCountMap;
	}

	public void setCrawledMonthMsgCountMap(Map<String, Integer> crawledMonthMsgCountMap) {
		this.crawledMonthMsgCountMap = crawledMonthMsgCountMap;
	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public void setMonths(String[] months) {
		this.months = months;
	}
	
}
