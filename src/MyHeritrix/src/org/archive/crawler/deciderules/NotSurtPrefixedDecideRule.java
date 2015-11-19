/* NotSurtPrefixedDecideRule
*
* $Id: NotSurtPrefixedDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
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


/**
 * Rule applies configured decision to any URIs that, when 
 * expressed in SURT form, do *not* begin with one of the prefixes
 * in the configured set. 
 * 
 * The set can be filled with SURT prefixes implied or
 * listed in the seeds file, or another external file. 
 *
 * @author gojomo
 */
public class NotSurtPrefixedDecideRule extends SurtPrefixedDecideRule {

    private static final long serialVersionUID = -7491388438128566377L;

    //private static final Logger logger =
    //    Logger.getLogger(NotSurtPrefixedDecideRule.class.getName());
    /**
     * Usual constructor. 
     * @param name
     */
    public NotSurtPrefixedDecideRule(String name) {
        super(name);
        setDescription(
                 "NotSurtPrefixedDecideRule. Makes the configured decision " +
                 "for any URI which, when expressed in SURT form, does *not* " +
                 "begin with the established prefixes (from either seeds " +
                 "specification or an external file).");
    }

    /**
     * Evaluate whether given object's URI is NOT in the SURT
     * prefix set -- simply reverse superclass's determination
     * 
     * @param object
     * @return true if regexp is matched
     */
    protected boolean evaluate(Object object) {
        return !super.evaluate(object);
    }
}
