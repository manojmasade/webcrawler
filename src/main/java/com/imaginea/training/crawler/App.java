package com.imaginea.training.crawler;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.imaginea.training.crawler.core.Config;
import com.imaginea.training.crawler.core.Crawler;

/**
 * App: Java Crawler
 *
 */
public class App {
	
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	
	private static final String CONFIG_PATH = "classpath*:applicationContext.xml";
	
	private static final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONFIG_PATH);
	
	
    public static void main( String[] args ) {
    	final App app = (App) context.getBean("app");
    	app.invoke();
    }
    
    /**
     * Invokes the process of crawler
     */
    private void invoke() { 
    	logger.info("Java Crawler -- Download emails for specified year" );
    	long processStartTime = System.currentTimeMillis();
    	logger.info("Begin Date:" + new Date());
    	
    	Config.setRunSafeMode(true); 
    	Config.setResumeCrawling(false);
    	
    	String years[] = { "2014" };
    	for (int i = 0; i < years.length; i++) {
    		Crawler crawler = (Crawler) context.getBean("crawler", years[i], years[i]);
        	Thread yearThread = new Thread(crawler);
        	yearThread.start();	
		}
        
        long processEndTime = System.currentTimeMillis();
		long processDiffTime = processEndTime-processStartTime;
		logger.info("Crawl Duration - Seconds : " + processDiffTime/1000 + ", Minutes: " + processDiffTime/(1000*60));
    }
}
