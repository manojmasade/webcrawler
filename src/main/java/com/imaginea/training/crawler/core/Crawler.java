package com.imaginea.training.crawler.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
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

/**
 * 
 * @author manojm
 *
 */
public class Crawler {
	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
			
	private Controller controller;
	
	private Parser parser;

	private int currentMsgCount = 0;
	
	public void init() {
		this.parser = new Parser();
		this.controller = new Controller();
	}

	public void start() {
		run();
	}

	private void run() {
		try {
			processPage();
		} catch (CrawlException e) {
			logger.error("run failed - " + e.getMessage());
		}
	}

	/**
	 * 
	 * @throws CrawlException
	 */
	private void processPage() throws CrawlException {
		logger.debug("processPage begin");
		
		try {
			final WebClient webClient = new WebClient();
			final HtmlPage page = parser.getPage(webClient);
			List<HtmlElement> table_yearElements = parser.parseTableForYears(page, "2014");
			
			for (HtmlElement table_yearElement : table_yearElements) {
				List<HtmlElement> th_yearElements = table_yearElement.getElementsByAttribute("th", "colspan", "3");
				String year = th_yearElements.get(0).asText();
				
				DomNodeList<HtmlElement> tbody_yearElements = table_yearElement.getElementsByTagName("tbody");
				HtmlElement tbody_yearElement = tbody_yearElements.get(0);
				
				DomNodeList<HtmlElement> tr_monthNodes = tbody_yearElement.getElementsByTagName("tr");
				for (HtmlElement tr_monthNode : tr_monthNodes) {

					// Log information
					if(logger.isInfoEnabled()){
						List<HtmlElement> td_monthDateElement = tr_monthNode.getElementsByAttribute("td", "class", "date");
						String month = td_monthDateElement.get(0).asText();
						List<HtmlElement> td_monthMsgcountElement = tr_monthNode.getElementsByAttribute("td", "class", "msgcount");
						String msgCount = td_monthMsgcountElement.get(0).asText();
						logger.info("Year: " + year + ", Month: " + month + ", MsgCount: " + msgCount);
					}
					
					// href links for msgs by thread (subject)
					DomNodeList<HtmlElement> anchor_monthNodes = tr_monthNode.getElementsByTagName("a");
					for (HtmlElement anchor_monthNode : anchor_monthNodes) {
						if(anchor_monthNode.getAttribute("href").contains("mbox/thread") && anchor_monthNode.getAttribute("href").contains("2014")) {
							
							long startTime = System.currentTimeMillis();
							currentMsgCount = 0;
							HtmlElement msglistElement = extractMessagesContentInPage(anchor_monthNode); 
							boolean isNextPageAvailable = true;
							
							while (isNextPageAvailable) {
								List<HtmlElement> th_pagesElements = msglistElement.getElementsByAttribute("th", "class", "pages");
								HtmlElement th_pageElement = th_pagesElements.get(0);
								HtmlElement anchor_nextNode = (HtmlElement)th_pageElement.getLastElementChild();
								
								if(anchor_nextNode.getNodeName().equals("a") && anchor_nextNode.asText().contains("Next")) {
									msglistElement = extractMessagesContentInPage(anchor_nextNode);
								} else {
									isNextPageAvailable = false;	
								}
							} 

							// Log information
							if(logger.isInfoEnabled()) {
								logger.info("Message Count: " + currentMsgCount);
								long endTime = System.currentTimeMillis();
								logger.info("Duration- Seconds : " + (endTime-startTime)/1000 + ", Minutes: " + (endTime-startTime)/(1000*60)); 
								logger.info("-----");
							}
							
						}
					}
				}
			}
			webClient.closeAllWindows();
		} catch (FailingHttpStatusCodeException e) {
			logger.error(e.getMessage());
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error("processPage failed - " + e.getMessage());
			throw new CrawlException(e);
		}
	}
	
	/**
	 * 
	 * @param anchor_monthNode
	 * @return
	 * @throws CrawlException
	 */
	private HtmlElement extractMessagesContentInPage(HtmlElement anchor_monthNode) throws CrawlException {
		try {
			HtmlPage monthResponse = anchor_monthNode.click();
			HtmlElement msglistElement = monthResponse.getHtmlElementById("msglist");
			HtmlTable table_msgList = (HtmlTable) msglistElement;
			List<HtmlTableBody> tbody_msgslist = table_msgList.getBodies();
			HtmlTableBody tbody_msgList = tbody_msgslist.get(0);
			
			for (final HtmlTableRow row : tbody_msgList.getRows()) {
				for (final HtmlTableCell td_msgElement : row.getCells()) {
					DomAttr attr = td_msgElement.getAttributeNode("class");
					
					if(attr.getNodeValue().equals("subject")) {
						DomNodeList<HtmlElement> anchor_msgNodes = td_msgElement.getElementsByTagName("a");
						if(anchor_msgNodes.size() > 0) {
							currentMsgCount += 1;
							
							// Read emails
							extractMessageContent(anchor_msgNodes);
						}
					}
				}
			}
			return msglistElement;
		} catch (ElementNotFoundException e) {
			logger.error(e.getMessage()); 
			throw new CrawlException(e);
		} catch (IOException e) {
			logger.error(e.getMessage()); 
			throw new CrawlException(e);
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new CrawlException(e);
		}
		
	}
	
	/**
	 * Read emails
	 * @param tdElements
	 * @throws IOException
	 */
	private void extractMessageContent(DomNodeList<HtmlElement> emailAnchorNodes) throws CrawlException {
		try {
			String emailAddress = null;
			String emailContent = null;
			HtmlAnchor anchor = null; 
			HtmlPage emailResponse = null;
			StringBuffer fileNameBuffer = null;
			String fileName = null;
			String emailSentDate = null;
			
			for (HtmlElement emailAnchor : emailAnchorNodes) {
				anchor = (HtmlAnchor) emailAnchor;
				emailAddress = anchor.getHrefAttribute();
				emailResponse = anchor.click();
				emailContent = emailResponse.asXml();
				
				// Get email sent date
				emailSentDate = parseEmailSentDate(emailResponse);
				
				// Write email content to a file: <emailAddress>_<emailSentDate>
				fileNameBuffer = new StringBuffer();
				fileName = fileNameBuffer.append(emailAddress).append(Constant.UNDERSCORE).append(emailSentDate).toString();
				//writeEmailToDisk(fileName, emailContent);

				logger.debug("Email content: " + emailContent);
				logger.debug("-------------------------------------------------------");
				//break;
			}
		} catch (CrawlException e) { 
			throw e;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new CrawlException(e);
		}
	}

	/**
	 * 
	 * @param fileName
	 * @param emailContent
	 */
	private void writeEmailToDisk(String fileName, String emailContent) throws CrawlException {
		 File file = null;
		 FileOutputStream fop = null;
		
		 try {
			 	// Directory
			 	File fileDir = null;
			 	fileDir = new File("tmp");
			    if (!fileDir.exists()) {
			    	fileDir.mkdir();
			    }
			 
			 	// File : replace invalid chars from file name
			    fileName = fileName.replace(":", "-");
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

	/**
	 * Return email sent date
	 * @param emailResponse
	 * @return
	 */
	private String parseEmailSentDate(HtmlPage emailResponse) {
		HtmlElement table_msgviewElement = emailResponse.getHtmlElementById("msgview");
		List<HtmlElement> tr_dateElements = table_msgviewElement.getElementsByAttribute("tr", "class", "date");
		HtmlElement tr_dateElement = tr_dateElements.get(0);
		String emailSentDate = tr_dateElement.getLastChild().asText();
		logger.debug("emailSentDate : " + emailSentDate);
		return emailSentDate;
	}
	
}
