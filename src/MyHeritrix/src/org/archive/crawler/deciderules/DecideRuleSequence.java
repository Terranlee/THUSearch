/* RuleSequence
*
* $Id: DecideRuleSequence.java 4912 2007-02-18 21:11:08Z gojomo $
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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.MapType;

/**
 * RuleSequence represents a series of Rules, which are applied in turn
 * to give the final result.  Rules return {@link DecideRule#ACCEPT}, 
 * {@link DecideRule#REJECT}, or {@link DecideRule#PASS}.  The final result
 * of a DecideRuleSequence is that of the last rule decision made, either
 * ACCEPT or REJECT (PASS is used by rules that do not have an opinion
 * on a particular processing pass).
 *
 * @author gojomo
 */
public class DecideRuleSequence extends DecideRule {

    private static final long serialVersionUID = 8918111430698683110L;

    private static final Logger logger =
        Logger.getLogger(DecideRuleSequence.class.getName());

    public static final String ATTR_RULES = "rules";
    
    public DecideRuleSequence(String name) {
        this(name,"DecideRuleSequence. Multiple DecideRules applied in " +
            "order with last non-PASS the resulting 'decision'.");
    }
    public DecideRuleSequence(String name, String description) {
        super(name);
        setDescription(description);
        
        addElementToDefinition(new MapType(ATTR_RULES,
                "This is a list of DecideRules to be applied in sequence.", 
                DecideRule.class));
    }

    public Object decisionFor(Object object) {
        Object runningAnswer = PASS;
        for(Iterator iter = getRules(object).iterator(object);
                iter.hasNext();) {
            DecideRule r = (DecideRule)iter.next();
            if(runningAnswer==r.singlePossibleNonPassDecision(object)) {
                // there's no chance this rule will change the decision;
                continue;
            }
            Object answer = r.decisionFor(object);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rule " + r.getName() + " of " + this.getName() +
                    " decided " + answer + " on " + object);
            }
            if (answer != PASS) {
                runningAnswer = answer;
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Decision of " + this.getName() + " was " +
                runningAnswer);
        }
        return runningAnswer;
    }

    protected MapType getRules(Object o) {
        MapType rules = null;
        try {
            rules = (MapType)getAttribute(o, ATTR_RULES);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getLocalizedMessage());
        }
        return rules;
    }
    
    /* kick-update all constituent rules
     * (non-Javadoc)
     * @see org.archive.crawler.deciderules.DecideRule#kickUpdate()
     */
    public void kickUpdate() {
        for(Iterator iter = getRules(null).iterator(null);
                iter.hasNext();) {
            DecideRule r = (DecideRule)iter.next();
            r.kickUpdate();
        }
    }
}
