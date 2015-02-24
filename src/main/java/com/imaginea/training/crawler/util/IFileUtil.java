package com.imaginea.training.crawler.util;

import com.imaginea.training.crawler.exception.CrawlException;

public interface IFileUtil {

	public void downloadEmail(String fileName, String emailContent, String year, String month) throws CrawlException;
	
}
