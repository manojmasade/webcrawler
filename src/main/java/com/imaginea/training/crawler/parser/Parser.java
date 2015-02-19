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
	 * Multiple years
	 * @param page
	 * @param yearsCSV
	 * @return
	 */
	public List<HtmlElement> parseTableForYears(HtmlPage page, String yearsCSV){
		List<HtmlElement> yearElements = new ArrayList<HtmlElement>();
		String yearArr[] = yearsCSV.split(Constant.COMMA);
		for (int i = 0; i < yearArr.length; i++) {
			yearElements.add(parseTableForYear(page, yearArr[i]));
		}
		return yearElements;			
	}
	
	/**
	 * Single year
	 * @param page
	 * @param year
	 * @return
	 */
	public HtmlElement parseTableForYear(HtmlPage page, String year){
		// Get all years table elements
		String sourceYear = null;
		List<HtmlElement> table_yearElements = page.getBody().getElementsByAttribute("table", "class", "year");
		for (HtmlElement table_yearElement : table_yearElements) {
			List<HtmlElement> th_yearElements = table_yearElement.getElementsByAttribute("th", "colspan", "3");
			sourceYear = th_yearElements.get(0).asText();
			if(sourceYear != null && sourceYear.contains(year)) {
				return table_yearElement;
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
			logger.error(e.getMessage());
			throw new CrawlException(e);
		}
		return page;
	}
	
}
