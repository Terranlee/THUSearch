/* MatchesRegExpDecideRule
*
* $Id: MatchesRegExpDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Apr 4, 2005
*
* Copyright (C) 2005 Internet Archive.
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

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;



/**
 * Rule applies configured decision to any CrawlURIs whose String URI
 * matches the supplied regexp.
 *
 * @author gojomo
 */
public class MatchesRegExpDecideRule extends PredicatedDecideRule {

    private static final long serialVersionUID = 6441410917074319295L;

    private static final Logger logger =
        Logger.getLogger(MatchesRegExpDecideRule.class.getName());
    
    public static final String ATTR_REGEXP = "regexp";

    /**
     * Usual constructor. 
     * @param name
     */
    public MatchesRegExpDecideRule(String name) {
        super(name);
        setDescription("MatchesRegExpDecideRule. Applies the configured " +
            "decision to URIs matching the supplied regular expression.");
        addElementToDefinition(new SimpleType(ATTR_REGEXP, "Java regular" +
            "expression to match.", ""));
    }

    /**
     * Evaluate whether given object's string version
     * matches configured regexp
     * 
     * @param object
     * @return true if regexp is matched
     */
    protected boolean evaluate(Object object) {
        try {
            String regexp = getRegexp(object);
            String str = object.toString();
            boolean result = (regexp == null)?
                    false: TextUtils.matches(regexp, str);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Tested '" + str + "' match with regex '" +
                        regexp + " and result was " + result);
            }
            return result;
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard
            return false; 
        }
    }
    
    /** 
     * Get the regular expression string to match the URI against.
     *
     * @param o the object for which the regular expression should be
     *          matched against.
     * @return the regular expression to match against.
     */
    protected String getRegexp(Object o) {
        try {
            return (String) getAttribute(o, ATTR_REGEXP);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;  // Basically the filter is inactive if this occurs.
        }
    }
}
