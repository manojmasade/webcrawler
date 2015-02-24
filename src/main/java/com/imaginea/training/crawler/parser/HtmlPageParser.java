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

public class HtmlPageParser implements Parser {
	
	private static final Logger logger = LoggerFactory.getLogger(HtmlPageParser.class);
	
	/**
	 * Return the <table></table> which contains emails of Multiple years
	 * @param page
	 * @param yearsCSV
	 * @return
	 */
	@Override
	public List<HtmlElement> parseTableForYears(HtmlPage page, String yearsCSV) throws CrawlException {
		logger.debug("parseTableForYears yearsCSV:" + yearsCSV);
		List<HtmlElement> result = null;
		if(yearsCSV != null) {
			result = new ArrayList<HtmlElement>();
			String yearArr[] = yearsCSV.split(Constant.COMMA);
			for (int i = 0; i < yearArr.length; i++) {
				result.add(parseTableForYear(page, yearArr[i]));
			}	
		} else {
			logger.error("parseTableForYears failed - years cannot be null");
			throw new CrawlException("yearsCSV cannot be null");
		}
		return result;			
	}
	
	/**
	 * Return the <table></table> which contains emails of Single year
	 * @param page
	 * @param year
	 * @return
	 */
	@Override
	public HtmlElement parseTableForYear(HtmlPage page, String year){
		// Get all years table elements
		String contentYear = null;
		List<HtmlElement> table_yearElements = page.getBody().getElementsByAttribute(Constant.TABLE, Constant.CLASS, Constant.YEAR);
		List<HtmlElement> th_yearElements = null;
		for (HtmlElement result : table_yearElements) {
			th_yearElements = result.getElementsByAttribute(Constant.TH, Constant.COLSPAN, "3");
			contentYear = th_yearElements.get(0).asText();
			if(contentYear != null && contentYear.contains(year)) {
				return result;
			}
		}
		logger.info("parseTableForYear - content not matched for year: {}", year);
		return null;
	}
	
	/**
	 * 
	 * @param webClient
	 * @return
	 * @throws CrawlException
	 */
	@Override
	public HtmlPage getPage(WebClient webClient) throws CrawlException {
		HtmlPage result = null;
		try {
			result = webClient.getPage(Constant.URL_MAVEN_USERS);
		} catch (FailingHttpStatusCodeException | IOException e) {
			logger.error("getting page for url {} failed : {}", Constant.URL_MAVEN_USERS, e);
			throw new CrawlException(e);
		}
		return result;
	}
	
	/**
	 * Return email sent date
	 * @param emailResponse
	 * @return
	 */
	@Override
	public String parseEmailSentDate(HtmlPage emailResponse) {
		HtmlElement table_msgviewElement = emailResponse.getHtmlElementById(Constant.MSGVIEW);
		List<HtmlElement> tr_dateElements = table_msgviewElement.getElementsByAttribute(Constant.TR, Constant.CLASS, Constant.DATE);
		HtmlElement tr_dateElement = tr_dateElements.get(0);
		String result = tr_dateElement.getLastChild().asText();
		logger.debug("email sent date : " + result);
		return result;
	}
}
