/* $Id: ExceedsDocumentLengthTresholdDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
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

import org.archive.crawler.settings.SimpleType;

public class ExceedsDocumentLengthTresholdDecideRule extends
NotExceedsDocumentLengthTresholdDecideRule {

    private static final long serialVersionUID = -3008503096295212224L;

    /**
     * Usual constructor. 
     * @param name Name of this rule.
     */
    public ExceedsDocumentLengthTresholdDecideRule(String name) {
    	super(name);
    	setDescription("ExceedsDocumentLengthTresholdDecideRule. ACCEPTs URIs "+
             "with content length exceeding a given treshold. "+
             "Either examines HTTP header content length or " +
             "actual downloaded content length and returns false " +
             "for documents exceeding a given length treshold.");

        addElementToDefinition(new SimpleType(ATTR_CONTENT_LENGTH_TRESHOLD,
        	"Min " +
    	    "content-length this filter will allow to pass through. If -1, " +
    	    "then no limit.", DEFAULT_CONTENT_LENGTH_TRESHOLD));    }
    
    /**
     * @param contentLength content length to check against treshold
     * @param obj Context object.
     * @return contentLength exceeding treshold?
     */
    protected Boolean makeDecision(int contentLength, Object obj) {
    	return contentLength > getContentLengthTreshold(obj);
    }
    
    /**
     * @param obj Context object.
     * @return content length threshold
     */
    protected int getContentLengthTreshold(Object obj) {
        int len = ((Integer)getUncheckedAttribute(obj,
        		ATTR_CONTENT_LENGTH_TRESHOLD)).intValue();
        return len == -1? 0: len;
    }
}