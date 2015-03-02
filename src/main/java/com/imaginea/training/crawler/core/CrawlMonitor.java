package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author manojm
 *
 */
public class CrawlMonitor implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CrawlMonitor.class);

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
			if(completedThreads == Config.NO_OF_MONTH_THREADS) {
				crawler.getController().getFileUtil().createFile(Config.DIR_DOWNLOAD_EMAILS, Config.FILE_CRAWL, Config.STATE_COMPLETED);
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {}
			
			logger.debug("shutdownThreads:" + shutdownThreads + ", completedThreads:" + completedThreads);
			if((shutdownThreads + completedThreads) == Config.NO_OF_MONTH_THREADS){
				crawler.setExit(true);
			}
		}
		logger.info("Crawling process exited. Status of months, Completed:{}, Shutdown:{}", crawler.getTotalMonthsCompletedList().size(), crawler.getShutdownMap().size());
		logger.info("Completed: {}, Shutdown: {}", crawler.getTotalMonthsCompletedList(), crawler.getShutdownMap().keySet()); 
		System.exit(1);
	}

}
