package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.util.FileUtil;
import com.imaginea.training.crawler.util.NetUtil;

/**
 * 
 * @author manojm
 *
 */
public class Controller {
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);

	/**
	 * Is crawling finished
	 */
	protected boolean finished;
	
	/**
	 * Is crawler shutdown abnormally as connection is not available
	 */
	//protected boolean shuttingDown;
	
	private FileUtil fileUtil = null;
	
	private NetUtil netUtil = null;
	
	public Controller() {
		finished = false;
		//shuttingDown = false;
		fileUtil = new FileUtil();
		netUtil = new NetUtil();
	}

	public FileUtil getFileUtil() {
		return fileUtil;
	}

	public NetUtil getNetUtil() {
		return netUtil;
	}
	
}
