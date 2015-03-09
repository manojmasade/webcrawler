package com.imaginea.training.crawler;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.imaginea.training.crawler.constant.Constant;
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
	
	@Autowired
	private Config config;
	
	/**
	 * Starting point for App to start
	 * @param args
	 */
    public static void main( String[] args ) {
    	final App app = (App) context.getBean("app");
    	app.setArgs(args);
    	app.invoke();
    }

    /**
     * Set args to Config
     * @param args
     */
    public void setArgs(String[] args) { 
    	logger.debug("Arguments length: {}", args.length);

    	Map<String, String> argsMap = new LinkedHashMap<>();
    	for (int i = 0; i < args.length; i++) {
			String argMap[] = args[i].split("=");
			argsMap.put(argMap[0], argMap[1]);
		}
    	logger.info("arguments: {}", argsMap); 
    	
    	if(argsMap.get(Constant.ARG_RESUME) != null){
    		config.setResumeCrawling(Boolean.valueOf(argsMap.get(Constant.ARG_RESUME)));
    	}
    	if(argsMap.get(Constant.ARG_SAFE_MODE) != null){
    		config.setRunSafeMode(Boolean.valueOf(argsMap.get(Constant.ARG_SAFE_MODE)));
    	}
    }
    
    /**
     * Invokes the process of crawler
     */
    public void invoke() { 
    	logger.info("Java Crawler -- Download emails for specified year" );
    	long processStartTime = System.currentTimeMillis();
    	logger.info("Begin Date:" + new Date());
    	
    	String years[] = { "2014" };
    	for (int i = 0; i < years.length; i++) {
    		Crawler crawler = (Crawler) context.getBean("crawler");
    		crawler.setYear(years[i]);
        	Thread yearThread = new Thread(crawler);
        	yearThread.start();	
		}
        
        long processEndTime = System.currentTimeMillis();
		long processDiffTime = processEndTime-processStartTime;
		logger.info("Crawl Duration - Seconds : " + processDiffTime/1000 + ", Minutes: " + processDiffTime/(1000*60));
    }

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
}
