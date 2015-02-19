package com.imaginea.training.crawler.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author manojm
 *
 */
public class CrawlException extends RuntimeException {
	private static final Logger logger = LoggerFactory.getLogger(CrawlException.class);

	private String message = null;
	
	public CrawlException() {
		super();
	}
	
	public CrawlException(String message) {
		super(message);
		this.message = message;
	}
	
	public CrawlException(Throwable cause) {
        super(cause);
    }
	
	public CrawlException(String message, Throwable cause) {
		super(message, cause);
	}
	
	@Override
	public String toString() {
		return message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
