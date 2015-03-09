package com.imaginea.training.crawler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.imaginea.training.crawler.core.Config;
import com.imaginea.training.crawler.core.Crawler;
import com.imaginea.training.crawler.parser.Parser;
import com.imaginea.training.crawler.util.FileUtil;
import com.imaginea.training.crawler.util.NetUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/applicationContextTest.xml" })
public class CrawlerTest {
	
	@Autowired
	private Config config;
	
	@Autowired
	private Crawler crawler;
	
	@Autowired
	private Parser parser;
	
	@Autowired
	private NetUtil netUtil;
	
	@Autowired
	private FileUtil fileUtil;
	
	@Test
	public void autoWiredBeansNotNull() {
		Assert.assertNotNull(config);
		Assert.assertNotNull(crawler);
	}
	
	@Test
	public void invokeCrawler() {
		String year = "2014";
		String months[] = {"Dec"};
		String fileName = "%3c547C1A5F.7070709@uni-jena.de%3e_Mon, 01 Dec 2014 07-35-59 GMT.txt";
		crawler.setYear(year);
		crawler.setMonths(months);
    	crawler.run();	

    	try {
			Thread.sleep(10000);
			File tempDir = new File(config.getEmailsDownloadDir());
			Assert.assertTrue(tempDir.exists()); 
			
			File yearDir = new File(tempDir, year);
			Assert.assertTrue(yearDir.exists()); 
			
			File monthDir = new File(yearDir, months[0]);
			Assert.assertTrue(monthDir.exists()); 
			
			File file = new File(monthDir, fileName);
			Assert.assertTrue(file.exists());
		} catch (InterruptedException e) {}
	}

	@Test
	public void isInternetReachable() {
		Assert.assertTrue(netUtil.isInternetReachable());
	}
	
	@Test
	public void createFile() {
		String dir = "tmp";
		File file = null;
		FileReader reader = null;
		String result = null;
		String fileName = "sample";
		
		try {
			fileUtil.createFile(dir, "sample", "Sample Content");
			
			File fileDir = new File("tmp");
			file = new File(fileDir, fileName + ".txt");
			reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			result = new String(chars);
			reader.close();
		 } catch (IOException e) {
			 e.printStackTrace();
		 } finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		 }
		Assert.assertEquals("Sample Content", result);
	}
	
	@Test
	public void getFileContent() {
		fileUtil.createFile("tmp", "sample", "Sample Content");
		String result = fileUtil.getFileContent("tmp", "sample");
		Assert.assertEquals("Sample Content", result);
	}
	
	
}
