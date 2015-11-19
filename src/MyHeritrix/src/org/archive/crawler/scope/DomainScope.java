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
public class DomainScope extends SeedCachingScope {

    private static final long serialVersionUID = 648062105277258820L;

    private static final Logger logger =
        Logger.getLogger(DomainScope.class.getName());

    public static final String ATTR_TRANSITIVE_FILTER = "transitiveFilter";
    public static final String ATTR_ADDITIONAL_FOCUS_FILTER =
        "additionalScopeFocus";
    public static final String DOT = ".";

    Filter additionalFocusFilter;
    Filter transitiveFilter;

    public DomainScope(String name) {
        super(name);
        setDescription(
            "DomainScope: A scope for domain crawls *Deprecated* Use " +
            "DecidingScope instead. Crawls made with this" +
            " scope will be limited to the domain of its seeds. It will" +
            " however reach subdomains of the seeds' original domains." +
            " www[#].host is considered to be the same as host.");
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
        return this.transitiveFilter.accepts(o);
    }

    /**
     * Check if an URI is part of this scope.
     *
     * @param o An instance of UURI or of CandidateURI.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        UURI u = UURI.from(o);
        if (u == null) {
            return false;
        }
        // Get the seeds to refresh and then get an iterator inside a
        // synchronization block.  The seeds list may get updated during our
        // iteration. This will throw a concurrentmodificationexception unless
        // we synchronize.
        String seedDomain = null;
        String candidateDomain =null;

        // Get candidate domain where www[0-9]*\. is stripped.
        try {
            candidateDomain = u.getHostBasename();
        }
        catch (URIException e1) {
            logger.severe(
                "UURI getHostBasename failed for candidate URI: " + u);
        }
        if (candidateDomain == null) {
            // either an opaque, unfetchable, or unparseable URI
            return false;
        }

        Iterator iter = seedsIterator();
        while(iter.hasNext()) {
            UURI s = (UURI)iter.next();
            // Get seed domain where www[0-9]*\. is stripped.
            try {
                seedDomain = s.getHostBasename();
            }
            catch (URIException e) {
                logger.severe("UURI getHostBasename failed for seed: " +
                    s);
            }
            if (seedDomain == null) {
                // GetHost can come back null.  See bug item
                // [ 910120 ] java.net.URI#getHost fails when leading digit
                continue;
            }

            // Check if stripped hosts are same.
            if (seedDomain.equals(candidateDomain)) {
                checkClose(iter);
                return true;
            }

            // Hosts are not same. Adjust seed basename to check if
            // candidate domain ends with .seedDomain
            seedDomain = DOT + seedDomain;
            if (seedDomain.regionMatches(0, candidateDomain,
                candidateDomain.length() - seedDomain.length(),
                seedDomain.length())) {
                // Domain suffix congruence
                checkClose(iter);
                return true;
            } // Else keep trying other seeds
        }
        // if none found, fail
        checkClose(iter);
        return false;
    }

    protected boolean additionalFocusAccepts(Object o) {
        return additionalFocusFilter.accepts(o);
    }
}
