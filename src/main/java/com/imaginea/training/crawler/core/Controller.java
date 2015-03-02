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

	private FileUtil fileUtil;
	
	private NetUtil netUtil;
	
	public Controller() {
		fileUtil = new FileUtil();
		netUtil = new NetUtil();
	}

	public FileUtil getFileUtil() {
		return fileUtil;
	}

	public NetUtil getNetUtil() {
		return netUtil;
	}

	public void setFileUtil(FileUtil fileUtil) {
		this.fileUtil = fileUtil;
	}

	public void setNetUtil(NetUtil netUtil) {
		this.netUtil = netUtil;
	}
	
}
