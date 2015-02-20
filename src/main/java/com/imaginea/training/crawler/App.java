package com.imaginea.training.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.core.Crawler;

/**
 * App: Java Crawler
 *
 */
public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	
    public static void main( String[] args ) {
    	logger.info("Java Crawler -- Download emails for year 2014" );
    	long processStartTime = System.currentTimeMillis();
		
        Crawler crawler = new Crawler();
        crawler.init();
        crawler.start();
        
        long processEndTime = System.currentTimeMillis();
		long processDiffTime = processEndTime-processStartTime;
		logger.info("Crawl Duration - Seconds : " + processDiffTime/1000 + ", Minutes: " + processDiffTime/(1000*60));
    }
}
