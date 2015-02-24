package com.imaginea.training.crawler.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.exception.CrawlException;
import com.imaginea.training.crawler.parser.HtmlPageParser;
import com.imaginea.training.crawler.parser.Parser;

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

	private String months[] = {
		"Dec", "Nov", "Oct", "Sep", "Aug", "Jul",
		"Jun", "May", "Apr", "Mar", "Feb", "Jan"
	};
	
	public Crawler(String year) {
		this.year = year;
		this.name = year;
	}
	
	private void init() {
		this.parser = new HtmlPageParser();
		this.controller = new Controller();
	}

	@Override
	public void run() {
		try {
			init();
			processPage();
		} catch (CrawlException e) {
			logger.error("run failed", e);
		}
	}

	/**
	 * Process page for the year
	 * @throws CrawlException
	 */
	private void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			// Get the html content for all input years
			String month = null; 
			String msgCount = null;
			final WebClient webClient = new WebClient();
			final HtmlPage page = parser.getPage(webClient);
			HtmlElement table_yearElement = parser.parseTableForYear(page, this.year);
			HtmlTable table_year = (HtmlTable) table_yearElement;
			HtmlTableBody tbody_year = table_year.getBodies().get(0);

			// Months
			for (final HtmlTableRow tr_monthNode : tbody_year.getRows()) {

				if(logger.isInfoEnabled() || logger.isDebugEnabled()){
					List<HtmlElement> td_monthDateElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.DATE);
					month = td_monthDateElement.get(0).asText();
					month = month.substring(0, month.indexOf(Constant.SPACE));
					List<HtmlElement> td_monthMsgcountElement = tr_monthNode.getElementsByAttribute(Constant.TD, Constant.CLASS, Constant.MSGCOUNT);
					msgCount = td_monthMsgcountElement.get(0).asText();
					logger.info("year:" + year + " month:" + month + " emails: " + msgCount);
				}
				
				// Child thread for months
				CrawlerThread crawlerThread = new CrawlerThread(parser, controller, month, year);
	        	crawlerThread.setName(month);
				Thread child = new Thread(crawlerThread);
				child.start();	
			}
			//webClient.closeAllWindows();
			logger.debug("processPage end");
		} catch (FailingHttpStatusCodeException e) {
			logger.error("Http status code exception", e);
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("processPage failed", e);
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
