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
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.filter.FilePatternFilter;
import org.archive.crawler.filter.TransclusionFilter;
import org.archive.crawler.framework.Filter;
import org.archive.net.UURI;

/**
 * A core CrawlScope suitable for the most common
 * crawl needs.
 *
 * Roughly, its logic is that a URI is included if:
 *
 *    (( isSeed(uri) || focusFilter.accepts(uri) )
 *      || transitiveFilter.accepts(uri) )
 *     && ! excludeFilter.accepts(uri)
 *
 * The focusFilter may be specified by either:
 *   - adding a 'mode' attribute to the
 *     <code>scope</code> element. mode="broad" is equivalent
 *     to no focus; modes "path", "host", and "domain"
 *     imply a SeedExtensionFilter will be used, with
 *     the <code>scope</code> element providing its configuration
 *   - adding a <code>focus</code> subelement
 * If unspecified, the focusFilter will default to
 * an accepts-all filter.
 *
 * The transitiveFilter may be specified by supplying
 * a <code>transitive</code> subelement. If unspecified, a
 * TransclusionFilter will be used, with the <code>scope</code>
 * element providing its configuration.
 *
 * The excludeFilter may be specified by supplying
 * a <code>exclude</code> subelement. If unspecified, a
 * accepts-none filter will be used -- meaning that
 * no URIs will pass the filter and thus be excluded.
 *
 * @author gojomo
 * @deprecated As of release 1.10.0.  Replaced by {@link DecidingScope}.
 */
public class PathScope extends SeedCachingScope {

    private static final long serialVersionUID = -2217024073240277527L;

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.basic.PathScope");

    public static final String ATTR_TRANSITIVE_FILTER = "transitiveFilter";
    public static final String ATTR_ADDITIONAL_FOCUS_FILTER =
        "additionalScopeFocus";

    Filter additionalFocusFilter;
    Filter transitiveFilter;

    public PathScope(String name) {
        super(name);
        setDescription(
            "PathScope: A scope for path crawls *Deprecated* Use " +
            "DecidingScope instead. Crawls made with this scope" +
            " will be limited to a specific portion of the hosts its seeds" +
            " provide. More specifically the paths those seeds provide." +
            " For example if one of the seeds is 'archive.org/example/'" + 
            " all URIs under the path 'examples' will be crawled (like" +
            " 'archive.org/examples/hello.html') but not URIs in other" +
            " paths or root (i.e. 'archive.org/index.html).");
        this.additionalFocusFilter = (Filter) addElementToDefinition(
                new FilePatternFilter(ATTR_ADDITIONAL_FOCUS_FILTER));
        this.transitiveFilter = (Filter) addElementToDefinition(
                new TransclusionFilter(ATTR_TRANSITIVE_FILTER));
    }

    /**
     * @param o
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(Object o) {
        if (this.transitiveFilter == null) {
            return true;
        }
        return this.transitiveFilter.accepts(o);
    }

    /**
     * @param o
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        UURI u = UURI.from(o);
        if (u == null) {
            return false;
        }
        // Get the seeds to refresh 
        Iterator iter = seedsIterator();
        while(iter.hasNext()) {
            UURI s = (UURI) iter.next();
            if (isSameHost(s, u)) {
                try {
                    // Protect against non-parseable URIs. See
                    // "[ 910120 ] java.net.URI#getHost fails when
                    // leading digit"
                    if (s.getPath() == null || u.getPath() == null) {
                        continue;
                    }
                }
                catch (URIException e) {
                    logger.severe("Failed get path on " + u + " or " + s +
                        ": " + e.getMessage());
                }
                try {
                    if (s.getPath().regionMatches(0, u.getPath(), 0,
                        s.getPath().lastIndexOf('/'))) {
                        // matches up to last '/'
                        checkClose(iter);
                        return true;
                    } else {
                        // no match; try next seed
                        continue;
                    }
                }
                catch (URIException e) {
                    logger.severe("Failed get path on " + u + " or " + s +
                        ": " + e.getMessage());
                }
            }
        }
        // if none found, fail
        checkClose(iter);
        return false;
    }

    // Javadoc inherited
    @Override
    protected boolean additionalFocusAccepts(Object o) {
        return this.additionalFocusFilter.accepts(o);
    }

}
