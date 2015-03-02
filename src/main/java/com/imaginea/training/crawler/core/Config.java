package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author manojm
 *
 */
public class Config {
	
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	public static final String DIR_DOWNLOAD_EMAILS = "tmp";
	
	public static final String FILE_EXTENSION = ".html";
	
	public static final String FILE_CRAWL = "crawl";

	/**
	 * The folder will be used by crawler for storing the snapshot of crawl data. 
	 * It has information like what files are downloaded.
	 */
	private String crawlStorageWatchFolder;

	/**
	 * Resume crawling after connection is available
	 */
	public static boolean resumeCrawling = false;
	
	/**
	 * Skips internet connectivity check when safeMode is true
	 */
	public static boolean runSafeMode;
	
	/**
	 * Connection timeout in milliseconds
	 */
	public static final int CONNECTION_TIMEOUT = 30000;
	
	public static final int JAVASCRIPT_TIMEOUT = 3000;
	
	public static final int SHUTDOWN_TIME = 10000;
	
	public static final int SLEEP_INTERVAL = 1000;
	
	public static final String STATE_INITIALIZE = "INITIALIZE";
	
	public static final String STATE_RUNNING = "RUNNING";
	
	public static final String STATE_COMPLETED = "COMPLETED";
	
	public static final int NO_OF_MONTH_THREADS = 12;
	

	public String getCrawlStorageFolder() {
		return crawlStorageWatchFolder;
	}

	public void setCrawlStorageFolder(String crawlStorageWatchFolder) {
		this.crawlStorageWatchFolder = crawlStorageWatchFolder;
	}

	public static void setRunSafeMode(boolean runSafeMode) {
		Config.runSafeMode = runSafeMode;
	}

	public static boolean isResumeCrawling() {
		return resumeCrawling;
	}

	public static void setResumeCrawling(boolean resumeCrawling) {
		Config.resumeCrawling = resumeCrawling;
	}
	
}
