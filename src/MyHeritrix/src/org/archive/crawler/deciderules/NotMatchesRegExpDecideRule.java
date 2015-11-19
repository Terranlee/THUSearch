/* NotMatchesRegExpDecideRule
*
* $Id: NotMatchesRegExpDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Apr 4, 2005
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
 * Rule applies configured decision to any URIs which do *not*
 * match the supplied regexp.
 *
 * @author gojomo
 */
public class NotMatchesRegExpDecideRule extends MatchesRegExpDecideRule {

    private static final long serialVersionUID = -2085313401991694306L;
    //private static final Logger logger =
    //    Logger.getLogger(NotMatchesRegExpDecideRule.class.getName());


    /**
     * Usual constructor. 
     * @param name
     */
    public NotMatchesRegExpDecideRule(String name) {
        super(name);
        setDescription("NotMatchesRegExpDecideRule. Applies the configured " +
        "decision to URIs *not* matching the supplied regular expression.");
    }

    /**
     * Evaluate whether given object's string version does not match 
     * configured regexp (by reversing the superclass's answer).
     * 
     * @param object Object to make decision about.
     * @return true if the regexp is not matched
     */
    protected boolean evaluate(Object object) {
        return ! super.evaluate(object);
    }
}
