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
public class HostScope extends SeedCachingScope {

    private static final long serialVersionUID = -6257664892667267266L;

    public static final String ATTR_TRANSITIVE_FILTER = "transitiveFilter";
    public static final String ATTR_ADDITIONAL_FOCUS_FILTER =
        "additionalScopeFocus";

    Filter additionalFocusFilter;
    Filter transitiveFilter;

    public HostScope(String name) {
        super(name);
        setDescription(
            "HostScope: A scope for host crawls *Deprecated* Use " +
            "DecidingScope instead. Crawls made with this scope" +
            " will be limited to the hosts its seeds. Thus if one of" +
            " the seeds is 'archive.org' the subdomain" +
            " 'crawler.archive.org' will not be crawled." +
            " 'www.host' is considered to be the same as host.");
       additionalFocusFilter = (Filter) addElementToDefinition(
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
            if (isSameHost((UURI)iter.next(), u)) {
                checkClose(iter);
                return true;
            }
        }
        // if none found, fail
        checkClose(iter);
        return false;
    }

   
    // Javadoc inherited.
    @Override
    protected boolean additionalFocusAccepts(Object o) {
        return additionalFocusFilter.accepts(o);
    }

}
