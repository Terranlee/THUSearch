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
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.deciderules;


import javax.management.AttributeNotFoundException;

import org.archive.crawler.framework.CrawlScope;

/**
 * DecidingScope: a Scope which makes its accept/reject decision based on 
 * whatever DecideRules have been set up inside it.
 * @author gojomo
 */
public class DecidingScope extends CrawlScope {

    private static final long serialVersionUID = -2942787757512964906L;

    //private static Logger logger =
    //    Logger.getLogger(DecidingScope.class.getName());
    public static final String ATTR_DECIDE_RULES = "decide-rules";

    public DecidingScope(String name) {
        super(name);
        setDescription(
            "DecidingScope. A Scope that applies one or " +
            "more DecideRules to determine whether a URI is accepted " +
            "or rejected (returns false).");
        addElementToDefinition(new DecideRuleSequence(
            ATTR_DECIDE_RULES));
    }
    
    protected DecideRule getDecideRule(Object o) {
        try {
            return (DecideRule)getAttribute(o, ATTR_DECIDE_RULES);
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean innerAccepts(Object o) {
        // would like to use identity test '==' here, but at some
        // step string is being copied, and 'legal-type' mechanism
        // doesn't enforce/maintain identity
        return getDecideRule(o).decisionFor(o).equals(DecideRule.ACCEPT);
    }
    
    /**
     * Note that configuration updates may be necessary. Pass to
     * constituent rules.
     */
    public void kickUpdate() {
        super.kickUpdate();
        // TODO: figure out if there's any way to reconcile this with
        // overrides/refinement rules
        getDecideRule(null).kickUpdate();
    }
}
