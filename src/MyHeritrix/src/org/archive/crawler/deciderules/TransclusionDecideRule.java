/* TransclusionDecideRule
*
* $Id: TransclusionDecideRule.java 4952 2007-03-03 01:31:56Z gojomo $
*
* Created on Apr 1, 2005
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

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;



/**
 * Rule ACCEPTs any CrawlURIs whose path-from-seed ('hopsPath' -- see
 * {@link CandidateURI#getPathFromSeed()}) ends 
 * with at least one, but not more than, the given number of 
 * non-navlink ('L') hops. 
 * 
 * Otherwise, if the path-from-seed is empty or if a navlink ('L') occurs
 * within max-trans-hops of the tail of the path-from-seed, this rule
 * returns PASS.
 *  
 * <p>Thus, it allows things like embedded resources (frames/images/media) 
 * and redirects to be transitively included ('transcluded') in a crawl, 
 * even if they otherwise would not, for some reasonable number of hops
 * (1-4).
 *
 * @see <a href="http://www.google.com/search?q=define%3Atransclusion&sourceid=mozilla&start=0&start=0&ie=utf-8&oe=utf-8">Transclusion</a>
 *
 * @author gojomo
 */
public class TransclusionDecideRule extends PredicatedDecideRule {

    private static final long serialVersionUID = -3975688876990558918L;

    private static final String ATTR_MAX_TRANS_HOPS = "max-trans-hops";

    private static final String ATTR_MAX_SPECULATIVE_HOPS = "max-speculative-hops";

    /**
     * Default maximum transitive hops -- any type
     * Default access so can be accessed by unit tests.
     */
    static final Integer DEFAULT_MAX_TRANS_HOPS = new Integer(3);

    /**
     * Default maximum speculative ('X') hops.
     * Default access so can be accessed by unit tests.
     */
    static final Integer DEFAULT_MAX_SPECULATIVE_HOPS = new Integer(1);

    /**
     * Usual constructor. 
     * @param name Name of this DecideRule.
     */
    public TransclusionDecideRule(String name) {
        super(name);
        setDescription("TransclusionDecideRule. ACCEPTs URIs whose path " +
                "from the seed ends with up to (but not more than) the " +
                "configured '" + ATTR_MAX_TRANS_HOPS +
                "' number of non-navlink ('L') hops.");
        // make default ACCEPT unchangeable 
        Type type = getElementFromDefinition(ATTR_DECISION);
        type.setTransient(true);
        addElementToDefinition(new SimpleType(ATTR_MAX_TRANS_HOPS,
            "Maximum number of non-navlink (non-'L') hops to ACCEPT.", 
            DEFAULT_MAX_TRANS_HOPS));
        addElementToDefinition(new SimpleType(ATTR_MAX_SPECULATIVE_HOPS,
            "Maximum number of speculative ('X') hops to ACCEPT.", 
            DEFAULT_MAX_SPECULATIVE_HOPS));
    }

    /**
     * Evaluate whether given object is within the threshold number of
     * transitive hops.
     * 
     * @param object Object to make decision on.
     * @return true if the transitive hops >0 and <= max
     */
    protected boolean evaluate(Object object) {
        CandidateURI curi = null;
        try {
            curi = (CandidateURI)object;
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard.
            return false;
        }
        String hopsPath = curi.getPathFromSeed();
        if (hopsPath == null || hopsPath.length() == 0) {
            return false; 
        }
        int count = 0;
        int specCount = 0; 
        for (int i = hopsPath.length() - 1; i >= 0; i--) {
            char c = hopsPath.charAt(i);
            if (c != Link.NAVLINK_HOP) {
                count++;
                if(c == Link.SPECULATIVE_HOP) {
                    specCount++;
                }
            } else {
                break;
            }
        }
        return count > 0 && (specCount <= getThresholdSpeculativeHops(object) && count <= getThresholdHops(object));
    }

    /**
     * @param obj Context object.
     * @return hops cutoff threshold
     */
    private int getThresholdHops(Object obj) {
        return ((Integer)getUncheckedAttribute(obj,ATTR_MAX_TRANS_HOPS)).
            intValue();
    }
    
    /**
     * @param obj Context object.
     * @return hops cutoff threshold
     */
    private int getThresholdSpeculativeHops(Object obj) {
        return ((Integer)getUncheckedAttribute(obj,ATTR_MAX_SPECULATIVE_HOPS)).
            intValue();
    }
}
