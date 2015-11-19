/* RefinedScope
*
* $Id: RefinedScope.java 4651 2006-09-25 18:31:13Z paul_jack $
*
* Created on Jul 16, 2004
*
* Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.scope;


import org.archive.crawler.framework.Filter;

/**
 * Superclass for Scopes which make use of "additional focus"
 * to add items by pattern, or want to swap in alternative
 * transitive filter. 
 * 
 * @author gojomo
 */
public abstract class RefinedScope extends ClassicScope {
    public static final String ATTR_TRANSITIVE_FILTER = "transitiveFilter";
    public static final String ATTR_ADDITIONAL_FOCUS_FILTER =
        "additionalScopeFocus";

    Filter additionalFocusFilter;
    Filter transitiveFilter;

    @SuppressWarnings("deprecation")
    public RefinedScope(String name) {
        super(name);

        this.additionalFocusFilter = (Filter) addElementToDefinition(
                new org.archive.crawler.filter.FilePatternFilter(
                        ATTR_ADDITIONAL_FOCUS_FILTER));
        this.transitiveFilter = (Filter) addElementToDefinition(
                new org.archive.crawler.filter.TransclusionFilter(
                        ATTR_TRANSITIVE_FILTER));
    }

    /**
     * @param o
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(Object o) {
        return this.transitiveFilter.accepts(o);
    }
    
    protected boolean additionalFocusAccepts(Object o) {
        return additionalFocusFilter.accepts(o);
    }
}
