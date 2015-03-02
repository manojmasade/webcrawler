package com.imaginea.training.crawler.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.core.Config;

public class NetUtil implements INetUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

	@Override
	public Boolean isInternetReachable() {
		Socket socket = null;
		boolean reachable = false;
		try {
			if(!Config.runSafeMode) {
				socket = new Socket("java.sun.com", 80);	
			}
		    reachable = true;
		} catch (UnknownHostException e) {
		} catch (IOException e) {
		} finally {            
		    if (socket != null) try { socket.close(); } catch(IOException e) {}
		}
		logger.debug("isInternetReachable:" + reachable);
		return reachable;
	}

}
