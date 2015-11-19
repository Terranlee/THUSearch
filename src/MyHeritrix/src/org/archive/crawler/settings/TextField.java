/* TextType
 *
 * $Id: TextField.java 3799 2005-09-01 18:13:23Z stack-sf $
 *
 * Created on Mar 26, 2004
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
package org.archive.crawler.settings;

import java.io.Serializable;

import org.archive.util.TextUtils;

/** Class to hold values for text fields.
 *
 * Objects of this class could be used instead of {@link java.lang.String} to
 * hold text strings with newlines in it. SimpleTypes with values wrapped in
 * objects of this class will show up in the UI as multiline text areas.
 *
 * @author John Erik Halse
 *
 */
public class TextField implements CharSequence, Serializable {
    private static final long serialVersionUID = -2853908867414076703L;
    private String value;

    /** Constructs a new TextField object.
     *
     * @param value the string represented by this TextField.
     */
    public TextField(String value) {
        this.value = TextUtils.replaceAll("\r\n", value, "\n").trim();
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#length()
     */
    public int length() {
        return value.length();
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index) {
        return value.charAt(index);
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    public boolean equals(Object obj) {
        return obj instanceof TextField && value.equals(obj);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return value.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return value;
    }
}
