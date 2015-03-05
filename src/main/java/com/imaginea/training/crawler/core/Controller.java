package com.imaginea.training.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.imaginea.training.crawler.util.FileUtil;
import com.imaginea.training.crawler.util.NetUtil;

/**
 * 
 * @author manojm
 *
 */
public class Controller {
	
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);

	@Autowired
	private FileUtil fileUtil;
	
	@Autowired
	private NetUtil netUtil;
	
	
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
