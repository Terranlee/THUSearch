/* FilterDecideRule
*
* $Id: DecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Mar 15, 2007
*
* Copyright (C) 2007 Internet Archive.
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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SimpleType;


/**
 * FilterDecideRule wraps a legacy Filter for use in DecideRule 
 * contexts. 
 *
 * @author gojomo
 */
public class FilterDecideRule extends DecideRule {
	private static final long serialVersionUID = -3193099932171335572L;
	private static Logger logger =
        Logger.getLogger(FilterDecideRule.class.getName());

    /**
     * Filter(s) to apply. Package protections for testing.
     */
    MapType filters = null;
    /** Filters setting */
    public final static String ATTR_FILTERS = "filters";
    
    public final static String ATTR_TRUE_DECISION = "true-decision";
    public final static String ATTR_FALSE_DECISION = "false-decision";
    public final static String[] ALLOWED_TYPES = 
    	new String[] {ACCEPT, PASS, REJECT};

    /**
     * Constructor.
     * @param name Name of this rule.
     */
    public FilterDecideRule(String name) {
        super(name);
        setDescription("FilterDecideRule wraps legacy Filters, allowing " +
        		"them to be used in places expecting DecideRules.");

        this.filters = (MapType) addElementToDefinition(
            new MapType(ATTR_FILTERS, "Filters considered to determine " +
                "decision.  If any filter returns FALSE, the configured " +
                "false-decision (usually REJECT) is applied. If no filter " +
                "returns false, the configured true-decision (usually " +
                "ACCEPT) is applied.", Filter.class));
        addElementToDefinition(new SimpleType(ATTR_TRUE_DECISION,
                "Decision applied if filters all return true. ", 
                ACCEPT, ALLOWED_TYPES));
        addElementToDefinition(new SimpleType(ATTR_FALSE_DECISION,
                "Decision applied if any filter returns false. ", 
                REJECT, ALLOWED_TYPES));
    }

    /**
     * Make decision on passed <code>object</code>.
     * @param object Object to rule on.
     * @return {@link #ACCEPT}, {@link #REJECT}, or {@link #PASS}.
     */
    public Object decisionFor(Object object) {
    	if(! (object instanceof CrawlURI)) {
    		return PASS;
    	}
        if (filtersAccept((CrawlURI) object)) {
			return ((String) getUncheckedAttribute(object, ATTR_TRUE_DECISION))
					.intern();
		} else {
			return ((String) getUncheckedAttribute(object, ATTR_FALSE_DECISION))
					.intern();
		}
    }

	/**
     * Do all specified filters (if any) accept this CrawlURI?
     *
     * @param curi
     * @return True if all filters accept this CrawlURI.
     */
    protected boolean filtersAccept(CrawlURI curi) {
        return filtersAccept(this.filters, curi);
    }
    
    /**
     * Do all specified filters (if any) accept this CrawlURI?
     *
     * @param curi
     * @param fs Filters to process.
     * @return True if all filters accept this CrawlURI.
     */
    protected boolean filtersAccept(MapType fs, CrawlURI curi) {
        if (fs.isEmpty(curi)) {
            return true;
        }
        for (Iterator i = fs.iterator(curi); i.hasNext();) {
            Filter filter = (Filter)i.next();
            if (!filter.accepts(curi)) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(filter + " rejected " + curi +
                        " in Processor " + getName());
                }
                return false;
            }
        }
        return true;
    }
}
