package com.imaginea.training.crawler.core;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.exception.CrawlException;
import com.imaginea.training.crawler.parser.Parser;
import com.imaginea.training.crawler.util.FileUtil;

/**
 * 
 * @author manojm
 *
 */
public class Crawler implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	private String name;
	
	private String year;
			
	private Controller controller;
	
	private Parser parser;

	private int currentMsgCount = 0;
	
	private String months[] = {
			"Dec 2014", "Nov 2014", "Oct 2014", "Sep 2014", "Aug 2014", "Jul 2014",
			"Jun 2014", "May 2014", "Apr 2014", "Mar 2014", "Feb 2014", "Jan 2014"
	};
	
	public Crawler(String year) {
		this.year = year;
	}
	
	private void init() {
		this.parser = new Parser();
		this.controller = new Controller();
	}

	/*public void start() {
		run();
	}*/
	
	@Override
	public void run() {
		try {
			init();
			processPage();
		} catch (CrawlException e) {
			logger.error("Crawler - run failed", e);
		}
	}

	/**
	 * 
	 * @throws CrawlException
	 */
	private void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			// Get the html content for all input years
			final WebClient webClient = new WebClient();
			final HtmlPage page = parser.getPage(webClient);
			List<HtmlElement> table_yearElements = parser.parseTableForYears(page, this.year);
			logger.info("table_yearElements size: {}", table_yearElements.size());
			
			String month = null; 
			String msgCount = null;
			String year = null;
			
			// Iterate Years
			for (HtmlElement table_yearElement : table_yearElements) {
				List<HtmlElement> th_yearElements = table_yearElement.getElementsByAttribute(Constant.TH, Constant.COLSPAN, "3");
				year = th_yearElements.get(0).asText();
				HtmlTable table_yearList = (HtmlTable) table_yearElement;
				List<HtmlTableBody> tbody_yearlist = table_yearList.getBodies();
				HtmlTableBody tbody_year = tbody_yearlist.get(0);

				if(logger.isInfoEnabled() || logger.isDebugEnabled()){
					logger.info("Year: " + year);
				}
				
				//int x = 1;
				
				// Iterate Months
				for (final HtmlTableRow tr_monthNode : tbody_year.getRows()) {
					
					/*if(x == 3) {
						break;
					}
					x++;*/
					
					// Logging
					if(logger.isInfoEnabled() || logger.isDebugEnabled()){
						List<HtmlElement> td_monthDateElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
						month = td_monthDateElement.get(0).asText();
						List<HtmlElement> td_monthMsgcountElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, "msgcount");
						msgCount = td_monthMsgcountElement.get(0).asText();
						logger.info("Month: " + month + ", MsgCount: " + msgCount);
						logger.info("this.name : " + this.name);
					}
					
					// Threads - child/month
					CrawlerThread crawlerThread = new CrawlerThread(null, parser, controller, month, year);
		        	crawlerThread.setName(month);
		        	//crawlerThread.setYear(year);
		        	//crawlerThread.setMonth(month);
					Thread child = new Thread(crawlerThread);
					logger.info("Child Thread; Year:" + year + ", Month:" + month);
					child.start();	
					//crawlerThread.processPage();
				}
			}
			//webClient.closeAllWindows();
			logger.debug("processPage end");
		} catch (FailingHttpStatusCodeException e) {
			logger.error("Crawler - Http status code exception", e);
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("Crawler - processPage failed", e);
			throw new CrawlException(e);
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getMonths() {
		return months;
	}

	public String getYear() {
		return year;
	}
	
}
