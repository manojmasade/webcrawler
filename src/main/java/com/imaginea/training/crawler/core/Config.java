package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.constant.Constant;

/**
 * 
 * @author manojm
 *
 */
public class Config {
	
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	private String emailsDownloadDir = "tmp";
	
	private String fileExtension = ".txt";
	
	private String crawlFileName = "crawl";
	
	private String contentFormat = Constant.CONTENT_FORMAT_TEXT;

	/**
	 * Resume crawling after connection is available
	 */
	private boolean resumeCrawling = false;
	
	/**
	 * Skips internet connectivity check when safeMode is true
	 */
	private boolean runSafeMode = false;
	
	/**
	 * Connection timeout in milliseconds
	 */
	private int connectionTimeout = 15000;
	
	private int javascriptTimeout = 3000;
	
	private int shutdownTime = 10000;
	
	private int sleepInterval = 1000;

	private int noOfMonths = Constant.NO_OF_MONTHS;
	
	private String state = Constant.STATE_COMPLETED;
	

	public void setRunSafeMode(boolean runSafeMode) {
		this.runSafeMode = runSafeMode;
	}

	public boolean isRunSafeMode() {
		return runSafeMode;
	}

	public boolean isResumeCrawling() {
		return resumeCrawling;
	}

	public void setResumeCrawling(boolean resumeCrawling) {
		this.resumeCrawling = resumeCrawling;
	}

	public String getEmailsDownloadDir() {
		return emailsDownloadDir;
	}

	public void setEmailsDownloadDir(String emailsDownloadDir) {
		this.emailsDownloadDir = emailsDownloadDir;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getCrawlFileName() {
		return crawlFileName;
	}

	public void setCrawlFileName(String crawlFileName) {
		this.crawlFileName = crawlFileName;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getJavascriptTimeout() {
		return javascriptTimeout;
	}

	public void setJavascriptTimeout(int javascriptTimeout) {
		this.javascriptTimeout = javascriptTimeout;
	}

	public int getShutdownTime() {
		return shutdownTime;
	}

	public void setShutdownTime(int shutdownTime) {
		this.shutdownTime = shutdownTime;
	}

	public int getSleepInterval() {
		return sleepInterval;
	}

	public void setSleepInterval(int sleepInterval) {
		this.sleepInterval = sleepInterval;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getNoOfMonths() {
		return noOfMonths;
	}

	public void setNoOfMonths(int noOfMonths) {
		this.noOfMonths = noOfMonths;
	}

	public String getContentFormat() {
		return contentFormat;
	}

	public void setContentFormat(String contentFormat) {
		this.contentFormat = contentFormat;
	}
	
}
