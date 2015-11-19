/* HasViaDecideRule
*
* $Id: HasViaDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Aug 11, 2006
*
* Copyright (C) 2006 Internet Archive.
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



/**
 * Rule applies the configured decision for any URI which has a 'via' 
 * (essentially, any URI that was a seed or some kinds of mid-crawl adds).
 *
 * @author gojomo
 */
public class HasViaDecideRule extends PredicatedDecideRule {

    private static final long serialVersionUID = 1670292311303097735L;

    /**
     * Usual constructor. 
     * @param name Name of this DecideRule.
     */
    public HasViaDecideRule(String name) {
        super(name);
        setDescription("HasViaDecideRule. Applies configured decision " +
                "to any URI that has a 'via'.");
    }

    /**
     * Evaluate whether given object is over the threshold number of
     * hops.
     * 
     * @param object
     * @return true if the mx-hops is exceeded
     */
    protected boolean evaluate(Object object) {
        try {
            CandidateURI curi = (CandidateURI)object;
            return curi.getVia() != null; 
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard
            return false; 
        }
    }
}
