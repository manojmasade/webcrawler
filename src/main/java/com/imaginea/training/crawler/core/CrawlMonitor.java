package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.imaginea.training.crawler.constant.Constant;

/**
 * 
 * @author manojm
 *
 */
public class CrawlMonitor implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CrawlMonitor.class);

	private Config config;
	
	@Autowired
	private Crawler crawler;
	
	public CrawlMonitor(Crawler crawler) {
		this.crawler = crawler;
	}
	
	@Override
	public void run() {
		
		while (!crawler.isExit()) {
			int shutdownThreads =  crawler.getShutdownMap().size();
			int completedThreads = crawler.getTotalMonthsCompletedList().size();

			// When all threads have completed their job
			if(completedThreads == config.getNoOfMonths()) {
				crawler.getController().getFileUtil().createFile(config.getEmailsDownloadDir(), config.getCrawlFileName(), Constant.STATE_COMPLETED);
			}
			
			try {
				//logger.info("shutdownThreads:" + shutdownThreads + ", completedThreads:" + completedThreads);
				Thread.sleep(5000);
			} catch (InterruptedException e) {}
			
			if((shutdownThreads + completedThreads) == config.getNoOfMonths()){
				crawler.setExit(true);
			}
		}
		logger.info("Crawling process exited");
		logger.info("Status of months, Completed:{}, Shutdown:{}", crawler.getTotalMonthsCompletedList().size(), crawler.getShutdownMap().size());
		logger.info("Completed: {}, Shutdown: {}", crawler.getTotalMonthsCompletedList(), crawler.getShutdownMap().keySet());
		System.exit(1);
	}

	public Crawler getCrawler() {
		return crawler;
	}

	public void setCrawler(Crawler crawler) {
		this.crawler = crawler;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

}
