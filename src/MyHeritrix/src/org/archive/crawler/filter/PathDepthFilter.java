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
 * HopsFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.SimpleType;
import org.archive.net.UURI;

/**
 * Accepts all urls passed in with a path depth
 * less or equal than the max-path-depth
 * value.
 *
 * @author Igor Ranitovic
 * @deprecated As of release 1.10.0.  Replaced by {@link DecidingFilter} and
 * equivalent {@link DecideRule}.
 */
public class PathDepthFilter extends Filter {

    private static final long serialVersionUID = 1626115117327154205L;

    private static final Logger logger =
        Logger.getLogger(PathDepthFilter.class.getName());
    public static final String ATTR_MATCH_RETURN_VALUE =
        "path-less-or-equal-return";
    public static final String ATTR_MAX_PATH_DEPTH = "max-path-depth";
    Integer maxPathDepth = new Integer(Integer.MAX_VALUE);
    final static char slash = '/';

    /**
     * @param name
     */
    public PathDepthFilter(String name) {
        super(name, "Path depth less or equal filter  *Deprecated* Use" +
        		"DecidingFilter and equivalent DecideRule instead.");
        addElementToDefinition(new SimpleType(ATTR_MAX_PATH_DEPTH, "Max path" +
                " depth for which this filter will match", maxPathDepth));
        addElementToDefinition(new SimpleType(ATTR_MATCH_RETURN_VALUE,
                "What to return when path depth is less or equal to max path" +
                " depth. \n", new Boolean(true)));
    }

    protected boolean innerAccepts(Object o) {
        String path = null;
        if (o == null) {
            return false;
        }
        
        if (o instanceof CandidateURI) {
            try {
                if (((CandidateURI)o).getUURI() != null) {
                    path = ((CandidateURI)o).getUURI().getPath();
                }
            }
            catch (URIException e) {
                logger.severe("Failed getpath for " +
                    ((CandidateURI)o).getUURI());
            }
        } else if (o instanceof UURI) {
            try {
                path = ((UURI)o).getPath();
            }
            catch (URIException e) {
                logger.severe("Failed getpath for " + o);
            }
        }

        if (path == null) {
            return true;
        }

        int count = 0;
        for (int i = path.indexOf(slash); i != -1;
        		i = path.indexOf(slash, i + 1)) {
            count++;
        }
        
        if (o instanceof CrawlURI) {
            try {
                this.maxPathDepth = (Integer) getAttribute(
                        ATTR_MAX_PATH_DEPTH, (CrawlURI) o);
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
            }
        }
        
        return (this.maxPathDepth != null) ?
            count <= this.maxPathDepth.intValue():
            false;
    }

    protected boolean returnTrueIfMatches(CrawlURI curi) {
       try {
           return ((Boolean) getAttribute(ATTR_MATCH_RETURN_VALUE, curi)).
               booleanValue();
       } catch (AttributeNotFoundException e) {
           logger.severe(e.getMessage());
           return true;
       }
    }
    
    protected boolean getFilterOffPosition(CrawlURI curi) {
        return returnTrueIfMatches(curi);
    }
}