package com.imaginea.training.crawler.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.core.Config;
import com.imaginea.training.crawler.exception.CrawlException;

/**
 * 
 * @author manojm
 *
 */
public class FileUtil implements IFileUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	@Autowired
	private Config config;
	
	/**
	 * Create a directory if does not exist
	 * @return
	 */
	private File createDirectory(String year, String month) {
		File tempDir = new File(config.getEmailsDownloadDir());
	    if (!tempDir.exists()) {
	    	tempDir.mkdir();
	    	logger.debug("{} directory created", config.getEmailsDownloadDir());
	    }
		File fileDir = new File(tempDir, year);
	    if (!fileDir.exists()) {
	    	fileDir.mkdir();
	    	logger.debug("{} directory created", year);
	    }
	    File result = new File(fileDir, month);
	    if (!result.exists()) {
	    	result.mkdir();
	    	logger.debug("{} directory created", month);
	    }
	    return result;
	}
	
	@Override
	public void createFile(String dir, String fileName, String content)
			throws CrawlException {
		File file = null;
		FileOutputStream fop = null;
		
		try {
			File fileDir = new File(dir);
		    if (!fileDir.exists()) {
		    	fileDir.mkdir();
		    	logger.debug("{} directory created", config.getEmailsDownloadDir());
		    }
		    
			file = new File(fileDir, fileName + config.getFileExtension());
			if(!file.exists()) {
		 		logger.debug("Creating a file {} as it does not exist", file.getPath());
				file.createNewFile();
			}
			fop = new FileOutputStream(file);
			byte[] contentInBytes = content.getBytes();
			fop.write(contentInBytes);
			fop.flush();
		 } catch (IOException e) {
			 logger.error("Creating a file failed", e);
			 throw new CrawlException(e);
		 } finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				logger.error("Closing file output stream failed", e);
				throw new CrawlException(e);
			}
		 }
	}
	
	/**
	 * 
	 * @param fileName
	 * @param emailContent
	 */
	@Override
	public void downloadEmail(String fileName, String emailContent, String year, String month) throws CrawlException {
		 File file = null;
		 FileOutputStream fop = null;
		
		 try {
			 	// Create directory
			 	File fileDir = createDirectory(year, month);
			 
			 	// File : replace invalid chars from file name
			    fileName = fileName.replace(Constant.COLON, Constant.HYPHEN);
			 	file = new File(fileDir, fileName + config.getFileExtension());
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
			 		logger.debug("File {} already exists", file.getPath());
			 	}  
		 } catch (IOException e) {
			 logger.error("Creating a file failed", e);
			 throw new CrawlException(e);
		 } finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				logger.error("Closing file output stream failed", e);
				throw new CrawlException(e);
			}
		 }
	}

	
	@Override
	public String getFileContent(String dir, String fileName) throws CrawlException {
		File file = null;
		String result = null;
		FileReader reader = null;
		
		try {
			File fileDir = new File(dir);
			file = new File(fileDir, fileName + config.getFileExtension());
			reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			result = new String(chars);
			reader.close();
		 } catch (IOException e) {
			 logger.error("Reading contents from file {} failed", fileName, e);
			 throw new CrawlException(e);
		 } finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				logger.error("Closing file reader failed", e);
				throw new CrawlException(e);
			}
		 }
		return result;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
}
