/* RejectRule
*
* $Id: RejectDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
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
 * Rule which answers REJECT to everything evaluated.
 *
 * @author gojomo
 */
public class RejectDecideRule extends DecideRule {

    private static final long serialVersionUID = -6621307860412933732L;

    public RejectDecideRule(String name) {
        super(name);
        setDescription("RejectDecideRule: always gives REJECT decision. " +
        "Useful to establish an initial default.");
    }

    public Object decisionFor(Object object) {
        return REJECT;
    }
    
    public Object singlePossibleNonPassDecision(Object object) {
        return REJECT;
    }
}
