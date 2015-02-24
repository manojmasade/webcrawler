package com.imaginea.training.crawler.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.imaginea.training.crawler.constant.Constant;
import com.imaginea.training.crawler.exception.CrawlException;

/**
 * 
 * @author manojm
 *
 */
public class Parser {
	private static final Logger logger = LoggerFactory.getLogger(Parser.class);

	/**
	 * Return the <table></table> which contains emails of Multiple years
	 * @param page
	 * @param yearsCSV
	 * @return
	 */
	public List<HtmlElement> parseTableForYears(HtmlPage page, String yearsCSV) throws CrawlException {
		logger.debug("parseTableForYears yearsCSV:" + yearsCSV);
		List<HtmlElement> yearElements = null;
		if(yearsCSV != null) {
			yearElements = new ArrayList<HtmlElement>();
			String yearArr[] = yearsCSV.split(Constant.COMMA);
			for (int i = 0; i < yearArr.length; i++) {
				yearElements.add(parseTableForYear(page, yearArr[i]));
			}	
		} else {
			logger.error("parseTableForYears failed - years cannot be null");
			throw new CrawlException("yearsCSV cannot be null");
		}
		return yearElements;			
	}
	
	/**
	 * Return the <table></table> which contains emails of Single year
	 * @param page
	 * @param year
	 * @return
	 */
	public HtmlElement parseTableForYear(HtmlPage page, String year){
		// Get all years table elements
		String contentYear = null;
		List<HtmlElement> table_yearElements = page.getBody().getElementsByAttribute(Constant.TABLE, Constant.CLASS, Constant.YEAR);
		List<HtmlElement> th_yearElements = null;
		for (HtmlElement table_yearElement : table_yearElements) {
			th_yearElements = table_yearElement.getElementsByAttribute(Constant.TH, Constant.COLSPAN, "3");
			contentYear = th_yearElements.get(0).asText();
			if(contentYear != null && contentYear.contains(year)) {
				return table_yearElement;
			} else {
				logger.info("parseTableForYear - year: {} not matched with content: {}", year, contentYear); 
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param webClient
	 * @return
	 * @throws CrawlException
	 */
	public HtmlPage getPage(WebClient webClient) throws CrawlException {
		HtmlPage page = null;
		try {
			page = webClient.getPage(Constant.URL_MAVEN_USERS);
		} catch (FailingHttpStatusCodeException | IOException e) {
			logger.error("getting page for url {} failed : {}", Constant.URL_MAVEN_USERS, e);
			throw new CrawlException(e);
		}
		return page;
	}
	
	/**
	 * Return email sent date
	 * @param emailResponse
	 * @return
	 */
	public String parseEmailSentDate(HtmlPage emailResponse) {
		HtmlElement table_msgviewElement = emailResponse.getHtmlElementById(Constant.MSGVIEW);
		List<HtmlElement> tr_dateElements = table_msgviewElement.getElementsByAttribute(Constant.TR, Constant.CLASS, Constant.DATE);
		HtmlElement tr_dateElement = tr_dateElements.get(0);
		String emailSentDate = tr_dateElement.getLastChild().asText();
		logger.debug("email sent date : " + emailSentDate);
		return emailSentDate;
	}
	
}
