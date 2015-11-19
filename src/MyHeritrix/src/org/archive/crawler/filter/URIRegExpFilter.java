/* Copyright (C) 2003 Internet Archive.
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
 *
 * RegExpFilter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;


/**
 * Compares passed object -- a CrawlURI, UURI, or String --
 * against a regular expression, accepting matches.
 *
 * @author Gordon Mohr
 * @deprecated As of release 1.10.0.  Replaced by {@link DecidingFilter} and
 * equivalent {@link DecideRule}.
 */
public class URIRegExpFilter
extends Filter {

    private static final long serialVersionUID = 1878356276332865537L;

    private static final Logger logger =
        Logger.getLogger(URIRegExpFilter.class.getName());
    public static final String ATTR_REGEXP = "regexp";
    public static final String ATTR_MATCH_RETURN_VALUE = "if-match-return";

    /**
     * @param name Filter name.
     */
    public URIRegExpFilter(String name) {
        this(name, "URI regexp filter *Deprecated* Use DecidingFilter and " +
        	"equivalent DecideRule instead. ", "");
        addElementToDefinition(
            new SimpleType(ATTR_MATCH_RETURN_VALUE, "What to return when" +
                " regular expression matches. \n", new Boolean(true)));
        addElementToDefinition(
            new SimpleType(ATTR_REGEXP, "Java regular expression.", ""));
    }

    public URIRegExpFilter(String name, String regexp) {
        this(name, "URI regexp filter.", regexp);
    }

    protected URIRegExpFilter(String name, String description, String regexp) {
        super(name, description);
        addElementToDefinition(new SimpleType(ATTR_MATCH_RETURN_VALUE,
            "What to return when" + " regular expression matches. \n",
            new Boolean(true)));
        addElementToDefinition(new SimpleType(ATTR_REGEXP,
            "Java regular expression.", regexp)); 
    }

    protected boolean innerAccepts(Object o) {
        String regexp = getRegexp(o);
        String str = o.toString();
        boolean result = (regexp == null)?
            false: TextUtils.matches(regexp, str);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Tested '" + str + "' match with regex '" +
                getRegexp(o) + " and result was " + result);
        }
        return result;
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
            // Basically the filter is inactive if this occurs
            // (The caller should be returning false when regexp is null).
            return null;  
        }
    }

    protected boolean returnTrueIfMatches(CrawlURI curi) {
        try {
            return ((Boolean)getAttribute(ATTR_MATCH_RETURN_VALUE, curi)).
                booleanValue();
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return true;
        }
    }
}
