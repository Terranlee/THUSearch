/* $Id: NotExceedsDocumentLengthTresholdDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
 * 
 * Created on 28.8.2006
 *
 * Copyright (C) 2006 Olaf Freyer
 * 
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 * 
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * Heritrix is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.crawler.deciderules;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;

public class NotExceedsDocumentLengthTresholdDecideRule
extends PredicatedDecideRule implements CoreAttributeConstants {
	

    private static final long serialVersionUID = -8774160016195991876L;

    private static final Logger logger = Logger.
    	getLogger(NotExceedsDocumentLengthTresholdDecideRule.class.getName());
    public static final String ATTR_CONTENT_LENGTH_TRESHOLD =
    	"content-length-treshold";
    static final Integer DEFAULT_CONTENT_LENGTH_TRESHOLD = -1;
    public static final String ATTR_USE_AS_MIDFETCH = "use-as-midfetch-filter";
    static final Boolean DEFAULT_USE_AS_MIDFETCH = new Boolean(true);
    
    
    // Header predictor state constants
    public static final int HEADER_PREDICTS_MISSING = -1;
	
    public NotExceedsDocumentLengthTresholdDecideRule(String name){
    	super(name);
    	setDescription("NotExceedsDocumentLengthTresholdDecideRule. " +
    			"REJECTs URIs "+
                "with content length exceeding a given treshold. "+
                "Either examines HTTP header content length or " +
                "actual downloaded content length and returns false " +
                "for documents exceeding a given length treshold.");
    	
        addElementToDefinition(new SimpleType(ATTR_USE_AS_MIDFETCH,
                "Shall this rule be used as a midfetch rule? If true, " +
                "this rule will determine content length based on HTTP " +
                "header information, otherwise the size of the already " +
                "downloaded content will be used.",
                DEFAULT_USE_AS_MIDFETCH));

        addElementToDefinition(new SimpleType(ATTR_CONTENT_LENGTH_TRESHOLD,
        	"Max " +
	        "content-length this filter will allow to pass through. If -1, " +
	        "then no limit.",
	        DEFAULT_CONTENT_LENGTH_TRESHOLD));
    }
    
    protected boolean evaluate(Object object) {
        try {
            CrawlURI curi = (CrawlURI)object;
            
            int contentlength = HEADER_PREDICTS_MISSING;

            //filter used as midfetch filter
        	if (getIsMidfetchRule(object)){
        		
                	if(curi.containsKey(A_HTTP_TRANSACTION) == false){
                		// Missing header info, let pass
                		if (logger.isLoggable(Level.INFO)) {
                			logger.info("Error: Missing HttpMethod object in " +
                				"CrawlURI. " + curi.toString());
                		}
                		return false;
                	}
        		
                    // Initially assume header info is missing
                    HttpMethod method =
                    	(HttpMethod)curi.getObject(A_HTTP_TRANSACTION);

                    // get content-length 
                    String newContentlength = null;
                    if (method.getResponseHeader("content-length") != null) {
                        newContentlength = method.
                        	getResponseHeader("content-length").getValue();
                    }
                
                    if (newContentlength != null &&
                    		newContentlength.length() > 0) {
            	        try {
            	        	contentlength = Integer.parseInt(newContentlength);
            	        } catch (NumberFormatException nfe) {
            	        	// Ignore.
            	        }
                    }
                
                    // If no document length was reported or format was wrong, 
                    // let pass
                    if (contentlength == HEADER_PREDICTS_MISSING) {
                        return false;
                    }
        	} else {
        	    contentlength = (int)curi.getContentSize();
        	}

            return makeDecision(contentlength, object);
                
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard
            return false; 
        }
    }
    
    /**
     * @param contentLength content length to check against treshold
     * @param obj Context object.
     * @return contentLength not exceeding treshold?
     */
    protected Boolean makeDecision(int contentLength, Object obj) {
    	return contentLength < getContentLengthTreshold(obj);
    }
    
    /**
     * @param obj Context object.
     * @return content length threshold
     */
    protected int getContentLengthTreshold(Object obj) {
        int len = ((Integer)getUncheckedAttribute(obj,
        	ATTR_CONTENT_LENGTH_TRESHOLD)).intValue();
        return len == -1? Integer.MAX_VALUE: len;
    }

    /**
     * @param obj Context object.
     * @return to be used as midfetch rule?
     */
    private Boolean getIsMidfetchRule(Object obj) {
        return ((Boolean)getUncheckedAttribute(obj,ATTR_USE_AS_MIDFETCH)).
        	booleanValue();
    }
}