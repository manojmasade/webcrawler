package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlMonitor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CrawlMonitor.class);

	private Crawler crawler;
	
	public CrawlMonitor(Crawler crawler) {
		this.crawler = crawler;
	}
	
	@Override
	public void run() {
		while (!crawler.isTerminate()) {
			if(crawler.getShutdownMap().size() == 12 || crawler.getTotalMonthsCompletedList().size() == 12){
				crawler.setTerminate(true);
			}
		}
		logger.info("Crawling process exiting status for months, Completed:{}, Shutdown:{}", crawler.getTotalMonthsCompletedList().size(), crawler.getShutdownMap().size());
		System.exit(1);
	}

}
