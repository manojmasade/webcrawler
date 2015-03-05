package com.imaginea.training.crawler.util;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.imaginea.training.crawler.core.Config;

/**
 * 
 * @author manojm
 *
 */
public class NetUtil implements INetUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);
	
	@Autowired
	private Config config;

	@Override
	public Boolean isInternetReachable() {
		Socket socket = null;
		boolean reachable = false;
		
		try {
			if(!config.isRunSafeMode()) {
				socket = new Socket("java.sun.com", 80);	
			}
		    reachable = true;
		} catch (IOException e) {
		} finally {            
		    if (socket != null) try { socket.close(); } catch(IOException e) {}
		}
		
		logger.debug("isInternetReachable:" + reachable);
		return reachable;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

}
