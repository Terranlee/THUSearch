/* AcceptRule
*
* $Id: PredicatedDecideRule.java 4914 2007-02-18 21:53:01Z gojomo $
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

/**
 * Rule which applies the configured decision only if a 
 * test evaluates to true. Subclasses override evaluate()
 * to establish the test. 
 *
 * @author gojomo
 */
public abstract class PredicatedDecideRule extends ConfiguredDecideRule {
    public PredicatedDecideRule(String name) {
        super(name);
        setDescription("ABSTRACT: Should not appear as choice.");
    }

    public Object decisionFor(Object object) {
        if(evaluate(object)) {
            return super.decisionFor(object);
        }
        return PASS;
    }

    protected abstract boolean evaluate(Object object);
}
