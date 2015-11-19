/* Copyright (C) 2005 Internet Archive.
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
 * ClassicScope.java
 * Created on Apr 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.extractor.Link;
//import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.settings.SimpleType;

/**
 * ClassicScope: superclass with shared Scope behavior for
 * most common scopes. 
 *
 * Roughly, its logic is captured in innerAccept(). A URI is 
 * included if:
 * <pre>
 *    forceAccepts(uri)
 *    || (((isSeed(uri) 
 *         || focusAccepts(uri)) 
 *         || additionalFocusAccepts(uri) 
 *         || transitiveAccepts(uri))
 *       && !excludeAccepts(uri));</pre>
 *
 * Subclasses should override focusAccepts, additionalFocusAccepts,
 * and transitiveAccepts. 
 *
 * The excludeFilter may be specified by supplying
 * a <code>exclude</code> subelement. If unspecified, a
 * accepts-none filter will be used -- meaning that
 * no URIs will pass the filter and thus be excluded.
 *  
 * @author gojomo
 */
public class ClassicScope extends CrawlScope {

    private static final long serialVersionUID = 4494905304855590002L;

    //private static final Logger logger = Logger.getLogger(ClassicScope.class
    //        .getName());

    public static final String ATTR_EXCLUDE_FILTER = "exclude-filter";

    public static final String ATTR_MAX_LINK_HOPS = "max-link-hops";

    public static final String ATTR_MAX_TRANS_HOPS = "max-trans-hops";

    // FIXME: Replace deprecated OrFilter with non-deprecated something
    
    @SuppressWarnings("deprecation")
    private org.archive.crawler.filter.OrFilter excludeFilter;

    /**
     * @param name
     *            ignored by superclass
     */
    @SuppressWarnings("deprecation")
    public ClassicScope(String name) {
        super(name);
        addElementToDefinition(new SimpleType(ATTR_MAX_LINK_HOPS,
            "Max link hops to include. URIs more than this number "
            + "of links from a seed will not be ruled in-scope. (Such "
            + "determination does not preclude later inclusion if a "
            + "shorter path is later discovered.)", new Integer(25)));
        addElementToDefinition(new SimpleType(ATTR_MAX_TRANS_HOPS,
            "Max transitive hops (embeds, referrals, preconditions) to " +
            "include. URIs reached by more than this number of transitive " +
            "hops will not be ruled in-scope, even if otherwise on an " +
            "in-focus site. (Such determination does not preclude later " +
            " inclusion if a shorter path is later discovered.)", 
            new Integer(5)));
        this.excludeFilter = (org.archive.crawler.filter.OrFilter)
            addElementToDefinition(new org.archive.crawler.filter.OrFilter(
                ATTR_EXCLUDE_FILTER));

        // Try to preserve the values of these attributes when we exchange
        // scopes.
        setPreservedFields(new String[] { ATTR_SEEDS, ATTR_MAX_LINK_HOPS,
            ATTR_MAX_TRANS_HOPS, ATTR_EXCLUDE_FILTER });
    }

    /**
     * Default constructor.
     */
    public ClassicScope() {
        this(CrawlScope.ATTR_NAME);
    }

    /**
     * Returns whether the given object (typically a CandidateURI) falls within
     * this scope.
     * 
     * @param o
     *            Object to test.
     * @return Whether the given object (typically a CandidateURI) falls within
     *         this scope.
     */
    protected final boolean innerAccepts(Object o) {
        return (((isSeed(o) || focusAccepts(o)) ||
            additionalFocusAccepts(o) || transitiveAccepts(o)) &&
            !excludeAccepts(o));
    }

    /**
     * Check if URI is accepted by the additional focus of this scope.
     * 
     * This method should be overridden in subclasses.
     * 
     * @param o
     *            the URI to check.
     * @return True if additional focus filter accepts passed object.
     */
    protected boolean additionalFocusAccepts(Object o) {
        return false;
    }

    /**
     * @param o
     *            the URI to check.
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(Object o) {
        return false;
    }

    /**
     * @param o the URI to check.
     * @return True if force-accepts filter accepts passed object.
     */
    protected boolean xforceAccepts(Object o) {
        return false;
    }
    
    /**
     * Check if URI is accepted by the focus of this scope.
     * 
     * This method should be overridden in subclasses.
     * 
     * @param o
     *            the URI to check.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        // The CrawlScope doesn't accept any URIs
        return false;
    }

    /**
     * Check if URI is excluded by any filters.
     * 
     * @param o
     *            the URI to check.
     * @return True if exclude filter accepts passed object.
     */
    @SuppressWarnings("deprecation")
    protected boolean excludeAccepts(Object o) {
        return (this.excludeFilter.isEmpty(o)) ? exceedsMaxHops(o)
                : this.excludeFilter.accepts(o) || exceedsMaxHops(o);
    }

    /**
     * Check if there are too many hops
     * 
     * @param o
     *            URI to check.
     * @return true if too many hops.
     */
    protected boolean exceedsMaxHops(Object o) {
        if (!(o instanceof CandidateURI)) {
            return false;
        }

        int maxLinkHops = 0;
//        int maxTransHops = 0;

        try {
            maxLinkHops = ((Integer) getAttribute(o, ATTR_MAX_LINK_HOPS))
                    .intValue();
//            maxTransHops = ((Integer) getAttribute(o, ATTR_MAX_TRANS_HOPS))
//                    .intValue();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CandidateURI cand = (CandidateURI) o;

        String path = cand.getPathFromSeed();
        int linkCount = 0;
        int transCount = 0;
        for (int i = path.length() - 1; i >= 0; i--) {
            if (path.charAt(i) == Link.NAVLINK_HOP) {
                linkCount++;
            } else if (linkCount == 0) {
                transCount++;
            }
        }
//      return (linkCount > maxLinkHops) || (transCount > maxTransHops);
        // base only on links, don't treat trans count as hard max
        return (linkCount > maxLinkHops);
    }

    /**
     * Take note of a situation (such as settings edit) where involved
     * reconfiguration (such as reading from external files) may be necessary.
     */
    @SuppressWarnings("deprecation")
    public void kickUpdate() {
        super.kickUpdate();
        excludeFilter.kickUpdate();
    }
}
