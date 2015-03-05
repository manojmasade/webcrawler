package com.imaginea.training.crawler.core;

import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
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
public class Crawler implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	private String name;
	
	private String year;
		
	@Autowired
	private Controller controller;
	
	@Autowired
	private Parser parser;
	
	@Autowired
	private Config config;
	
	private CrawlMonitor crawlMonitor;
	
	private int elapsedDuration = 0;
	
	private int shutdownDuration = 0;
	
	private boolean shutdown = false;
	
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
	
	public Crawler(String year) {
		this.year = year;
		this.name = year;
	}
	
	private void init() {
		setShutdownDuration(Config.SHUTDOWN_TIME);
	}
	
	private void initMonitorThread() {
		// Monitor thread
		CrawlMonitor crawlMonitor = new CrawlMonitor(this);
		Thread monitor = new Thread(crawlMonitor);
		monitor.start();
	}

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

	/**
	 * Initialize crawl data with information from disk and fill up map for use by month threads 
	 */
	private void initCrawlData() {
		boolean initializeAllThreads = false;
		String crawl_monthMsgCount = null;
		int msgCount = 0;
		
		if(Config.isResumeCrawling()) {
			String crawlStatus = controller.getFileUtil().getFileContent(config.getEmailsDownloadDir(), Config.FILE_CRAWL);
			if(crawlStatus != null){
				if(crawlStatus.equalsIgnoreCase(Config.STATE_RUNNING)) {
					initializeAllThreads = true;
				} else if(crawlStatus.equalsIgnoreCase(Config.STATE_COMPLETED)) {
					skipCrawling = true;
					return;
				}
			}
		}
		
		for (int i = 0; i < months.length; i++) {
			if(initializeAllThreads) {
				crawl_monthMsgCount = controller.getFileUtil().getFileContent(config.getEmailsDownloadDir(), months[i]);
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
					}
					
					// Month threads
					if(crawledMonthMsgCountMap.get(month) != null) {
						CrawlerThread crawlerThread = new CrawlerThread(this, parser, controller, month, year);
			        	crawlerThread.setName(month);
			        	int crawledMsgCount = crawledMonthMsgCountMap.get(month);
			        	int pendingTotalMsgCount = Integer.parseInt(msgCount) - crawledMsgCount;
			        	crawlerThread.setTotalMsgCount(pendingTotalMsgCount);
			        	crawlerThread.setCurrentMsgCount(crawledMsgCount);
						Thread child = new Thread(crawlerThread);
						child.start();	
					}
				}

				//webClient.closeAllWindows();
			} else {
				handleShutdown();
				if(controller.getNetUtil().isInternetReachable()) {
					processCrawl();	
				}
			}
			logger.debug("processPage end");
		} catch (Exception e) {
			if(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof NoRouteToHostException || e instanceof RuntimeException) {
				handleShutdown();
			} else {
				logger.error("processPage failed", e);
				throw new CrawlException(e);
			}
		}
	}

	/**
	 * Handle shutdown process
	 */
	private void handleShutdown() {
		logger.debug("handleShutdown");
		while(true && !this.isShutdown()) {
			if(controller.getNetUtil().isInternetReachable()) {
				this.setElapsedDuration(0);
				break;
			} else {
				try {
					synchronized (this) {
						if(this.getElapsedDuration() >= this.getShutdownDuration()) {
							break;
						} else {
							if(null == this.getLockApplied()) {
								this.setLockApplied(this);	
							}
							if(this == this.getLockApplied()) {
								logger.debug("Sleep {}", new Date());
								Thread.sleep(Config.SLEEP_INTERVAL);
								this.setElapsedDuration(this.getElapsedDuration() + Config.SLEEP_INTERVAL);	
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
		logger.debug("store crawlers data");
		if(!this.getShutdownMap().containsKey(this.name)) {
			controller.getFileUtil().createFile(config.getEmailsDownloadDir(), Config.FILE_CRAWL, Config.STATE_INITIALIZE);
			this.setShutdown(true);
			this.setExit(true);
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

	public Controller getController() {
		return controller;
	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public CrawlMonitor getCrawlMonitor() {
		return crawlMonitor;
	}

	public void setCrawlMonitor(CrawlMonitor crawlMonitor) {
		this.crawlMonitor = crawlMonitor;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
	
}
