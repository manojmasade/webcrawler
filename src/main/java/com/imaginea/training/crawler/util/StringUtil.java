package com.imaginea.training.crawler.util;

import com.imaginea.training.crawler.constant.Constant;

/**
 * 
 * @author manojm
 *
 */
public class StringUtil {

	public static String replaceColonWithHyphen(String content) { 
		return content.replace(Constant.COLON, Constant.HYPHEN);
	}
	
}
