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
 * OrFilter.java
 * Created on Nov 13, 2003
 *
 * $Header$
 */
package org.archive.crawler.deciderules;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.framework.Filter;

/**
 * DecidingFilter: a classic Filter which makes its accept/reject
 * decision based on whatever {@link DecideRule}s have been set up inside
 * it. 
 *
 * @author gojomo
 *
 */
public class DecidingFilter extends Filter {

    private static final long serialVersionUID = -7275555425381445477L;
    //private static final Logger logger =
    //    Logger.getLogger(DecidingFilter.class.getName());
    public static final String ATTR_DECIDE_RULES = "decide-rules";
    
    public DecidingFilter(String name, String description) {
        this(name);
        setDescription(description);
    }

    public DecidingFilter(String name) {
        super(name,
            "DecidingFilter. A filter that applies one or " +
            "more DecideRules " +
            "to determine whether a URI is accepted (returns true) or " +
            "rejected (returns false). Only a final decision of " +
            "ACCEPT returns true from the filter; either REJECT or " +
            "PASS returns false.");
        addElementToDefinition(
            new DecideRuleSequence(ATTR_DECIDE_RULES));
    }

    protected DecideRule getDecideRule(Object o) {
        try {
            return (DecideRule)getAttribute(o, ATTR_DECIDE_RULES);
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean innerAccepts(Object o) {
        return getDecideRule(o).decisionFor(o) == DecideRule.ACCEPT;
    }

    /**
     * Note that configuration updates may be necessary. Pass to
     * constituent filters.
     */
    public void kickUpdate() {
        // TODO: figure out if there's any way to reconcile this with
        // overrides/refinement filters
        getDecideRule(null).kickUpdate();
    }
}
