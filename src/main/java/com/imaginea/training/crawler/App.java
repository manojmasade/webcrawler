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
        
        Crawler crawler = new Crawler();
        crawler.init();
        crawler.start();
    }
}
