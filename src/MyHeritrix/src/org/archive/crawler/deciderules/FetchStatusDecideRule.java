/* FetchStatusDecideRule
*
* $Id: FetchStatusDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Aug 11, 2006
*
* Copyright (C) 2006 Internet Archive.
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

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;



/**
 * Rule applies the configured decision for any URI which has a
 * fetch status equal to the 'target-status' setting. 
 *
 * @author gojomo
 */
public class FetchStatusDecideRule extends PredicatedDecideRule {

    private static final long serialVersionUID = 5820599300395594619L;

    private static final String ATTR_TARGET_STATUS = "target-status";
    
    /**
     * Default access so available to test code.
     */
    static final Integer DEFAULT_TARGET_STATUS = new Integer(0);
    
    /**
     * Usual constructor. 
     * @param name Name of this DecideRule.
     */
    public FetchStatusDecideRule(String name) {
        super(name);
        setDescription("FetchStatusDecideRule. Applies configured decision " +
            "to any URI that has a fetch status equal to the setting.");
        addElementToDefinition(new SimpleType(ATTR_TARGET_STATUS, 
                "Fetch status for which the configured decision will be" +
                "applied.", DEFAULT_TARGET_STATUS));
    }

    /**
     * Evaluate whether given object is over the threshold number of
     * hops.
     * 
     * @param object
     * @return true if the mx-hops is exceeded
     */
    protected boolean evaluate(Object object) {
        try {
            CrawlURI curi = (CrawlURI)object;
            return curi.getFetchStatus() == 
                (Integer)getUncheckedAttribute(curi,ATTR_TARGET_STATUS); 
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard
            return false; 
        }
    }
}
