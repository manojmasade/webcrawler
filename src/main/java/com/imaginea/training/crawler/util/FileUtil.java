package com.imaginea.training.crawler.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.core.Config;
import com.imaginea.training.crawler.exception.CrawlException;

public class FileUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	/**
	 * Create a directory if does not exist
	 * @return
	 */
	public static File createDirectory() {
		File fileDir = new File(Config.DIR_DOWNLOAD_EMAILS);
	    if (!fileDir.exists()) {
	    	fileDir.mkdir();
	    }
	    return fileDir;
	}
	
	/**
	 * 
	 * @param fileName
	 * @param emailContent
	 */
	public static void storageEmail(String fileName, String emailContent) throws CrawlException {
		 File file = null;
		 FileOutputStream fop = null;
		
		 try {
			 	// Create directory
			 	File fileDir = createDirectory();
			 
			 	// File : replace invalid chars from file name
			    fileName = fileName.replace(Constant.COLON, Constant.HYPHEN);
			 	file = new File(fileDir, fileName);
			 	logger.debug("Absolute path:" + file.getAbsolutePath());
			 	
			 	if(!file.exists()) {
			 		logger.debug("Creating a new file as it does not exist : " + fileName);
					file.createNewFile();
					fop = new FileOutputStream(file);
					byte[] contentInBytes = emailContent.getBytes();
					fop.write(contentInBytes);
					fop.flush();
					fop.close();
			 	} else {
			 		logger.info("File alreaddy exists");
			 	}  
		 } catch (IOException e) {
			 logger.error(e.getMessage());
			 throw new CrawlException(e);
		 } finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
				throw new CrawlException(e);
			}
		 }
	}

}
