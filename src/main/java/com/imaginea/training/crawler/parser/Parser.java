package com.imaginea.training.crawler.parser;

import java.util.List;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.imaginea.training.crawler.exception.CrawlException;

/**
 * 
 * @author manojm
 *
 */
public interface Parser {

	public List<HtmlElement> parseTableForYears(HtmlPage page, String yearsCSV) throws CrawlException;
	
	public HtmlElement parseTableForYear(HtmlPage page, String year);
	
	public HtmlPage getPage(WebClient webClient) throws CrawlException;

	public String parseEmailSentDate(HtmlPage emailResponse);
	
}
