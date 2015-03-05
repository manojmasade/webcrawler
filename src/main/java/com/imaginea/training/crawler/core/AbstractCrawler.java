package com.imaginea.training.crawler.core;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCrawler {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractCrawler.class);
	
	private boolean shutdown = false;
	
	@Autowired
	private Controller controller;
	
	@Autowired
	private Config config;
	

	public void handleShutdown(Crawler crawler) {
		logger.debug("handleShutdown");
		
		while(true && !this.isShutdown()) {
			if(getController().getNetUtil().isInternetReachable()) {
				crawler.setElapsedDuration(0);
				break;
			} else {
				try {
					synchronized (this) {
						if(crawler.getElapsedDuration() >= crawler.getShutdownDuration()) {
							break;
						} else {
							if(null == crawler.getLockApplied()) {
								crawler.setLockApplied(this);	
							}
							if(this == crawler.getLockApplied()) {
								logger.debug("Sleep {}", new Date());
								Thread.sleep(config.getSleepInterval());
								crawler.setElapsedDuration(crawler.getElapsedDuration() + config.getSleepInterval());	
							}	
						}	
					}
				} catch (InterruptedException e1) {} 
			}
		}
		storeCrawlersData();
	}
	
	public abstract void storeCrawlersData();

	public synchronized boolean isShutdown() {
		return shutdown;
	}

	public synchronized void setShutdown(boolean shutdown) {
		this.shutdown = shutdown;
	}

	public Controller getController() {
		return controller;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
	
}
