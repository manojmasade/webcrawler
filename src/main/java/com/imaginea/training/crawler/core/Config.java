package com.imaginea.training.crawler.core;

import java.util.concurrent.atomic.AtomicInteger;

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

	/**
	 * The folder will be used by crawler for storing the snapshot of crawl data. 
	 * It has information like what files are downloaded.
	 */
	private String crawlStorageWatchFolder;

	/**
	 * Resume crawling after connection is available
	 */
	private boolean resumeCrawling = false;
	
	/**
	 * Skips internet connectivity check when safeMode is true
	 */
	public static final boolean RUN_SAFEMODE = true;
	
	/**
	 * Connection timeout in milliseconds
	 */
	public static final int CONNECTION_TIMEOUT = 30000;
	
	public static final int JAVASCRIPT_TIMEOUT = 3000;
	
	public static final int SHUTDOWN_TIME = 5000;
	
	public static final int SLEEP_INTERVAL = 1000;
	
	public static final String STATE_INITIALIZE = "INITIALIZE";
	
	public static final String STATE_RUNNING = "RUNNING";
	

	public String getCrawlStorageFolder() {
		return crawlStorageWatchFolder;
	}

	public void setCrawlStorageFolder(String crawlStorageWatchFolder) {
		this.crawlStorageWatchFolder = crawlStorageWatchFolder;
	}

	public boolean isResumeCrawling() {
		return resumeCrawling;
	}

	public void setResumeCrawling(boolean resumeCrawling) {
		this.resumeCrawling = resumeCrawling;
	}
	
}
