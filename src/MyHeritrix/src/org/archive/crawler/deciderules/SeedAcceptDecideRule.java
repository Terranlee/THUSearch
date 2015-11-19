/* SeedAcceptDecideRule
*
* $Id: SeedAcceptDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Sep 13, 2005
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


/**
 * Rule which ACCEPTs all 'seed' URIs (those for which 
 * isSeed is true). Good in a late position to ensure
 * other scope settings don't lock out explicitly added
 * seeds.
 *
 * @author gojomo
 */
public class SeedAcceptDecideRule extends AcceptDecideRule {

    private static final long serialVersionUID = 2167939872761313683L;

    public SeedAcceptDecideRule(String name) {
        super(name);
        setDescription("SeedAcceptDecideRule. ACCEPTs " +
                "all CrawlURIs that were explicitly added " +
                "as seeds -- even if earlier scope rules " +
                "rejected them.");
    }

    public Object decisionFor(Object object) {        
        try {
            if (((CandidateURI)object).isSeed()) {
                return ACCEPT;
            }
        } catch (ClassCastException e) {
           // Do nothing
        }
        return PASS;
    }
}
