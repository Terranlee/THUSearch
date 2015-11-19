/* AcceptDecideRule
*
* $Id: PrerequisiteAcceptDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Mar 3, 2005
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


/**
 * Rule which ACCEPTs all 'prerequisite' URIs (those with a 'P' in
 * the last hopsPath position). Good in a late position to ensure
 * other scope settings don't lock out necessary prerequisites.
 *
 * @author gojomo
 */
public class PrerequisiteAcceptDecideRule extends AcceptDecideRule {

    private static final long serialVersionUID = 2762042167111186142L;

    public PrerequisiteAcceptDecideRule(String name) {
        super(name);
        setDescription("PrerequisiteAcceptDecideRule. ACCEPTs " +
                "all CrawlURIs discovered via a prerequisite " +
                "'link'.");
    }

    public Object decisionFor(Object object) {        
        try {
            String hopsPath = ((CandidateURI)object).getPathFromSeed();
            if (hopsPath != null && hopsPath.length() > 0 &&
                    hopsPath.charAt(hopsPath.length()-1) == Link.PREREQ_HOP) {
                return ACCEPT;
            }
        } catch (ClassCastException e) {
           // Do nothing
        }
        return PASS;
    }
}
