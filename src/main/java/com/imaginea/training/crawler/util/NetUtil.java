package com.imaginea.training.crawler.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.constant.Constant;

public class NetUtil implements INetUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

	/*@Override
	public Boolean isInternetReachable() {
		try {
			InetAddress address = InetAddress.getByName("java.sun.com");
			logger.info("address:" + address);
			if(address == null){
				return false;
			}
		} catch (UnknownHostException e) {
			//logger.info("Connection not reachable to host, {}", e);
			return false;
		} catch (Exception e) {
			//logger.info("Unexpected exception in obtaining connection, {}", e);
			return false;
		}
		return true;
	}*/
	
	@Override
	public Boolean isInternetReachable() {
		Socket socket = null;
		boolean reachable = false;
		try {
		    socket = new Socket("java.sun.com", 80);
		    reachable = true;
		} catch (UnknownHostException e) { 
		} catch (IOException e) { 
		} finally {            
		    if (socket != null) try { socket.close(); } catch(IOException e) {}
		}
		//logger.info("reachable:" + reachable);
		return reachable;
	}

}
