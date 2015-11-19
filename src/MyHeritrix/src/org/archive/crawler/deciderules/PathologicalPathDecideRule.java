/* PathologicalPathDecideRule
*
* $Id: PathologicalPathDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
*
* Created on Apr 1, 2005
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

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;



/**
 * Rule REJECTs any URI which contains an excessive number of identical, 
 * consecutive path-segments (eg http://example.com/a/a/a/boo.html == 3 '/a' 
 * segments)
 *
 * @author gojomo
 */
public class PathologicalPathDecideRule extends MatchesRegExpDecideRule {

    private static final long serialVersionUID = -1803997581321178499L;

    private static final Logger logger =
        Logger.getLogger(PathologicalPathDecideRule.class.getName());

    public static final String ATTR_REPETITIONS = "max-repetitions";

    /**
     * Default maximum repetitions.
     * Default access so accessible by unit test.
     */
    static final Integer DEFAULT_REPETITIONS = new Integer(2);

    protected String constructedRegexp;
    
    /** Constructs a new PathologicalPathFilter.
     *
     * @param name the name of the filter.
     */
    public PathologicalPathDecideRule(String name) {
        super(name);
        setDescription("PathologicalPathDecideRule. This rule" +
                " is used to avoid crawler traps by adding a constraint on" +
                " how many times a path-segment pattern in the URI may be" +
                " repeated. A URI will be REJECTed if the same path-segment" +
                " repeats more than '" + ATTR_REPETITIONS + "' in a row.");

        // make default REJECT (overriding superclass) & always-default
        Type type = addElementToDefinition(new SimpleType(ATTR_DECISION,
                "Decision to be applied", REJECT, ALLOWED_TYPES));
        type.setTransient(true);
        
        // disable direct setting of regexp from superclass
        type = getElementFromDefinition(ATTR_REGEXP);
        type.setTransient(true);
        
        type = addElementToDefinition(new SimpleType(ATTR_REPETITIONS,
                "Number of times the pattern should be allowed to occur. " +
                "This rule returns its decision (usually REJECT) if a " +
                "path-segment is repeated more than number of times.",
                DEFAULT_REPETITIONS));
        // overriding would require reconstruction of regexp every test
        type.setOverrideable(false); 
    }

    /** 
     * Construct the regexp string to be matched against the URI.
     * @param o an object to extract a URI from.
     * @return the regexp pattern.
     */
    protected String getRegexp(Object o) {
        if (constructedRegexp == null) {
            // race no concern: assignment is atomic, happy with any last value
            constructedRegexp = constructRegexp();
        }
        return constructedRegexp;
    }
    
    protected String constructRegexp() {
        int rep = 0;
        try {
            rep = ((Integer) getAttribute(null, ATTR_REPETITIONS)).intValue();
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return (rep == 0) ? null : ".*?/(.*?/)\\1{" + rep + ",}.*";
    }
    
    
    /**
     * Repetitions may have changed; refresh constructedRegexp
     * 
     * @see org.archive.crawler.deciderules.DecideRule#kickUpdate()
     */
    public void kickUpdate() {
        super.kickUpdate();
        constructedRegexp = constructRegexp();
    }
}
