package com.imaginea.training.crawler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imaginea.training.crawler.constant.Constant;

/**
 * 
 * @author manojm
 *
 */
public class StringUtil {
	private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

	public static String replaceColonWithHyphen(String content) { 
		return content.replace(Constant.COLON, Constant.HYPHEN);
	}
	
}
