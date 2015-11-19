/* PrefixSet.java
 *
 * $Id: PrefixSet.java 4947 2007-03-01 04:47:24Z gojomo $
 *
 * Created April 29, 2008
 *
 * Copyright (C) 2008 Internet Archive.
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

package org.archive.util;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility class for maintaining sorted set of string prefixes.
 * Redundant prefixes are coalesced into the shorter prefix. 
 */
public class PrefixSet extends TreeSet<String> {
    private static final long serialVersionUID = -6054697706348411992L;

    public PrefixSet() {
        super();
    }

    /**
     * Test whether the given String is prefixed by one
     * of this set's entries. 
     * 
     * @param s
     * @return True if contains prefix.
     */
    public boolean containsPrefixOf(String s) {
        SortedSet sub = headSet(s);
        // because redundant prefixes have been eliminated,
        // only a test against last item in headSet is necessary
        if (!sub.isEmpty() && s.startsWith((String)sub.last())) {
            return true; // prefix substring exists
        } // else: might still exist exactly (headSet does not contain boundary)
        return contains(s); // exact string exists, or no prefix is there
    }
    
    /** 
     * Maintains additional invariant: if one entry is a 
     * prefix of another, keep only the prefix. 
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(String s) {
        SortedSet<String> sub = headSet(s);
        if (!sub.isEmpty() && s.startsWith((String)sub.last())) {
            // no need to add; prefix is already present
            return false;
        }
        boolean retVal = super.add(s);
        sub = tailSet(s+"\0");
        while(!sub.isEmpty() && ((String)sub.first()).startsWith(s)) {
            // remove redundant entries
            sub.remove(sub.first());
        }
        return retVal;
    }
    
}