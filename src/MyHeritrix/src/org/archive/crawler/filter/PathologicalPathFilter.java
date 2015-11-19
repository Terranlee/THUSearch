/* PathologicalFilter
 *
 * $Id: PathologicalPathFilter.java 4652 2006-09-25 18:41:10Z paul_jack $
 *
 * Created on Feb 20, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.filter;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/** 
 * Checks if a URI contains a repeated pattern.
 *
 * This filter is checking if a pattern is repeated a specific number of times.
 * The use is to avoid crawler traps where the server adds the same pattern to
 * the requested URI like: <code>http://host/img/img/img/img....</code>. This
 * filter returns TRUE if the path is pathological.  FALSE otherwise.
 *
 * @author John Erik Halse
 * @deprecated As of release 1.10.0.  Replaced by {@link DecidingFilter} and
 * equivalent {@link DecideRule}.
 */
public class PathologicalPathFilter extends URIRegExpFilter {

    private static final long serialVersionUID = 2797805167250054353L;

    private static final Logger logger =
        Logger.getLogger(PathologicalPathFilter.class.getName());

    public static final String ATTR_REPETITIONS = "repetitions";

    public static final Integer DEFAULT_REPETITIONS = new Integer(3);
    
    private final String REGEX_PREFIX = ".*?/(.*?/)\\1{";
    private final String REGEX_SUFFIX = ",}.*";

    /** Constructs a new PathologicalPathFilter.
     *
     * @param name the name of the filter.
     */
    public PathologicalPathFilter(String name) {
        super(name);
        setDescription("Pathological path filter *Deprecated* Use" +
        		"DecidingFilter and equivalent DecideRule instead. " +
        		"The Pathologicalpath filter" +
                " is used to avoid crawler traps by adding a constraint on" +
                " how many times a pattern in the URI could be repeated." +
                " Returns false if the path is NOT pathological (There" +
                " are no subpath reptitions or reptitions are less than" +
                " the '" + ATTR_REPETITIONS + "' limit).");

        Type type = getElementFromDefinition(ATTR_MATCH_RETURN_VALUE);
        type.setTransient(true);

        type = getElementFromDefinition(ATTR_REGEXP);
        type.setTransient(true);

        addElementToDefinition(new SimpleType(ATTR_REPETITIONS,
                "Number of times the pattern should be allowed to occur. \n" +
                "This filter returns true if number of repetitions of a" +
                " pattern exceeds this value",
                DEFAULT_REPETITIONS));
    }

    /** 
     * Construct the regexp string to be matched aginst the URI.
     * @param o an object to extract a URI from.
     * @return the regexp pattern.
     */
    protected String getRegexp(Object o) {
        int rep = 0;
        try {
            rep = ((Integer)getAttribute(o, ATTR_REPETITIONS)).intValue();
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return rep == 0? null: REGEX_PREFIX + (rep - 1) + REGEX_SUFFIX;
    }
    
    protected boolean getFilterOffPosition(CrawlURI curi) {
        return false;
    }
}
