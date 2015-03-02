package com.imaginea.training.crawler.util;

import com.imaginea.training.crawler.exception.CrawlException;

public interface IFileUtil {

	public void downloadEmail(String fileName, String content, String year, String month) throws CrawlException;
	
	public void createFile(String dir, String fileName, String content) throws CrawlException;
	
	public String getFileContent(String dir, String fileName) throws CrawlException;
	
}
