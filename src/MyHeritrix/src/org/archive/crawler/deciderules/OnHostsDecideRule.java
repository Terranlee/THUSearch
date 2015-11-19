/* OnHostsDecideRule
*
* $Id: OnHostsDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Apr 5, 2005
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


import org.archive.util.SurtPrefixSet;


/**
 * Rule applies configured decision to any URIs that
 * are on one of the hosts in the configured set of
 * hosts, filled from the seed set. 
 *
 * @author gojomo
 */
public class OnHostsDecideRule extends SurtPrefixedDecideRule {

    private static final long serialVersionUID = -7566348189389792625L;

    //private static final Logger logger =
    //    Logger.getLogger(OnHostsDecideRule.class.getName());
    /**
     * Usual constructor. 
     * @param name
     */
    public OnHostsDecideRule(String name) {
        super(name);
        setDescription(
                 "OnHostsDecideRule. Makes the configured decision " +
                 "for any URI which is on one of the hosts in the " +
                 "configured set of hostnames (derived from the seed" +
                 "list).");
        // disable direct setting of SURTs-related options
       getElementFromDefinition(ATTR_SEEDS_AS_SURT_PREFIXES).setTransient(true);
       getElementFromDefinition(ATTR_SURTS_SOURCE_FILE).setTransient(true);
       // leaving surts-dump as option helpful for debugging/learning, for now
       //getElementFromDefinition(ATTR_SURTS_DUMP_FILE).setTransient(true);
    }

    /**
     * Patch the SURT prefix set so that it only includes host-enforcing prefixes
     * 
     * @see org.archive.crawler.deciderules.SurtPrefixedDecideRule#readPrefixes()
     */
    protected void readPrefixes() {
        buildSurtPrefixSet();
        surtPrefixes.convertAllPrefixesToHosts();
        dumpSurtPrefixSet();
    }

	protected String prefixFrom(String uri) {
		return SurtPrefixSet.convertPrefixToHost(super.prefixFrom(uri));
	}
}
