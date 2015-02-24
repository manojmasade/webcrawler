package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.util.FileUtil;

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
	protected boolean shuttingDown;
	
	private FileUtil fileUtil = null;
	
	public Controller() {
		finished = false;
		shuttingDown = false;
		fileUtil = new FileUtil();
	}

	public FileUtil getFileUtil() {
		return fileUtil;
	}
	
}
