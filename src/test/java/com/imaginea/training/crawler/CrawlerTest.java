package com.imaginea.training.crawler;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.imaginea.training.crawler.core.Config;
import com.imaginea.training.crawler.core.Crawler;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/applicationContextTest.xml" })
public class CrawlerTest {
	
	@Autowired
	private Config config;
	
	@Autowired
	private Crawler crawler;
	
	@Test
	public void autoWiredBeansNotNull() {
		Assert.assertNotNull(config);
		Assert.assertNotNull(crawler);
	}
	
	@Test
	public void invoke() {
		String year = "2014";
		String months[] = {"Dec"};
		String fileName = "%3c547C1A5F.7070709@uni-jena.de%3e_Mon, 01 Dec 2014 07-35-59 GMT.txt";
		
		crawler.setYear(year);
		crawler.setMonths(months);
    	crawler.run();	

    	try {
			Thread.sleep(5000);
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


}
