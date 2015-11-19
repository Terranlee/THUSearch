/* ConfiguredDecideRule
*
* $Id: ConfiguredDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
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

import org.archive.crawler.settings.SimpleType;


/**
 * Rule which can be configured to ACCEPT or REJECT at
 * operator's option.  
 *
 * @author gojomo
 */
public class ConfiguredDecideRule extends DecideRule {

    private static final long serialVersionUID = -7084695808452312555L;

    public final static String ATTR_DECISION = "decision";
    public final static String[] ALLOWED_TYPES = new String[] {ACCEPT, REJECT};
    
    public ConfiguredDecideRule(String name) {
        super(name);
        setDescription("FRAMEWORK: Should not appear as choice");
        addElementToDefinition(new SimpleType(ATTR_DECISION,
            "Decision applied if this rule is triggered. " +
            "Otherwise PASS.", ACCEPT, ALLOWED_TYPES));
    }

    public Object decisionFor(Object object) {
        return singlePossibleNonPassDecision(object);
    }
    
    public Object singlePossibleNonPassDecision(Object object) {
        Object decision = getUncheckedAttribute(object, ATTR_DECISION);
        // We're not always getting back from the settings system the
        // interned versions of ACCEPT and REJECT.  Guard against this.
        Object result = PASS;
        for (int i = 0; i < ALLOWED_TYPES.length; i++) {
            if (ALLOWED_TYPES[i] == decision ||
                    ALLOWED_TYPES[i].equals(decision)) {
                result = ALLOWED_TYPES[i];
                break;
            }
        }
        return result;
    }
}
