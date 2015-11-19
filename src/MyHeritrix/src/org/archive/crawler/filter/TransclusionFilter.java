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
 * TransclusionFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.scope.ClassicScope;
import org.archive.crawler.settings.SimpleType;

/**
 * Filter which accepts CandidateURI/CrawlURI instances which contain more
 * than zero but fewer than max-trans-hops entries at the end of their
 * discovery path.
 *
 * @author Gordon Mohr
 * @deprecated As of release 1.10.0.  Replaced by {@link DecidingFilter} and
 * equivalent {@link DecideRule}.
 */
public class TransclusionFilter extends Filter {

    private static final long serialVersionUID = 4251767672778714051L;

    private static final String ATTR_MAX_SPECULATIVE_HOPS =
        "max-speculative-hops";
    private static final String ATTR_MAX_REFERRAL_HOPS = "max-referral-hops";
    private static final String ATTR_MAX_EMBED_HOPS = "max-embed-hops";
    private static final int DEFAULT_MAX_TRANS_HOPS = 4;

    /**
     * Default speculative hops.
     *
     * No more than 1
     */
    private static final int DEFAULT_MAX_SPECULATIVE_HOPS = 1;

    /**
     * Default maximum referral hops.
     *
     * No limit beside the overall trans limit
     */
    private static final int DEFAULT_MAX_REFERRAL_HOPS = -1;

    /**
     * Default embedded link hops.
     *
     * No limit beside the overall trans limit
     */
    private static final int DEFAULT_MAX_EMBED_HOPS = -1;

    int maxTransHops = DEFAULT_MAX_TRANS_HOPS;
    int maxSpeculativeHops = DEFAULT_MAX_SPECULATIVE_HOPS;
    int maxReferralHops = DEFAULT_MAX_REFERRAL_HOPS;
    int maxEmbedHops = DEFAULT_MAX_EMBED_HOPS;

//  // 1-3 trailing P(recondition)/R(eferral)/E(mbed)/X(speculative-embed) hops
//  private static final String TRANSCLUSION_PATH = ".*[PREX][PREX]?[PREX]?$";

    /**
     * @param name
     */
    public TransclusionFilter(String name) {
        super(name, "Transclusion filter *Deprecated* Use" +
        		"DecidingFilter and equivalent DecideRule instead.");

        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_SPECULATIVE_HOPS,
                "Maximum number of consecutive speculative (i.e. URIs" +
                " extracted that we are not sure if they are embeds or" +
                " not) hops to allow.\nA value of -1 means no upper limit.",
                new Integer(DEFAULT_MAX_SPECULATIVE_HOPS)));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_REFERRAL_HOPS,
                "Maximum number of consecutive referral hops to allow.\n" +
                "A value of -1 means no upper limit.",
                new Integer(DEFAULT_MAX_REFERRAL_HOPS)));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_EMBED_HOPS,
                "Maximum number of consecutive embed hops to allow.\n" +
                "A value of -1 means no upper limit.",
                new Integer(DEFAULT_MAX_EMBED_HOPS)));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
     */
    protected boolean innerAccepts(Object o) {
        if(! (o instanceof CandidateURI)) {
            return false;
        }
        String path = ((CandidateURI)o).getPathFromSeed();
        int transCount = 0;
        int specCount = 0;
        int refCount = 0;
        int embedCount = 0;
        loop: for(int i=path.length()-1;i>=0;i--) {
            // everything except 'L' is considered transitive
            switch (path.charAt(i)) {
                case Link.NAVLINK_HOP: {
                    break loop;
                }
                case Link.PREREQ_HOP: {
                    if(transCount==0) {
                        // always consider a trailing P as a 1-hop trans inclusion; disregard previous hops
                        transCount++;
                        break loop;
                    }
                    // otherwise, just count as another regular trans hop
                    break;
                }
                case Link.SPECULATIVE_HOP: {
                    specCount++;
                    break;
                }
                case Link.REFER_HOP: {
                    refCount++;
                    break;
                }
                case Link.EMBED_HOP: {
                    embedCount++;
                    break;
                }
                // FIXME: what is 'D'?
                // 'D's get a free pass
            }
            transCount++;
        }

        readMaxValues(o);

        // This is a case of possible transclusion
        return (transCount > 0) 
            // ...and the overall number of hops isn't too high
            && (transCount <= this.maxTransHops) 
            // ...and the number of spec-hops isn't too high
            && (this.maxSpeculativeHops < 0 ||  specCount <= this.maxSpeculativeHops) 
            // ...and the number of referral-hops isn't too high
            && (this.maxReferralHops < 0 || refCount <= this.maxReferralHops)
            // ...and the number of embed-hops isn't too high
            && (this.maxEmbedHops < 0 || embedCount <= this.maxEmbedHops);
    }

    public void readMaxValues(Object o) {
        try {
            CrawlScope scope =
                (CrawlScope) globalSettings().getModule(CrawlScope.ATTR_NAME);
            this.maxTransHops = ((Integer) scope.getAttribute(o, ClassicScope.ATTR_MAX_TRANS_HOPS)).intValue();
            this.maxSpeculativeHops = ((Integer) getAttribute(o, ATTR_MAX_SPECULATIVE_HOPS)).intValue();
            this.maxReferralHops = ((Integer) getAttribute(o, ATTR_MAX_REFERRAL_HOPS)).intValue();
            this.maxEmbedHops = ((Integer) getAttribute(o, ATTR_MAX_EMBED_HOPS)).intValue();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
