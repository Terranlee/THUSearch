/* Label
*
* $Id: Label.java 4545 2006-08-26 00:33:38Z stack-sf $
*
* Created on July 26, 2006.
*
* Copyright (C) 2006 Internet Archive.
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
package org.archive.util.anvl;

class Label extends SubElement {
	public static final char COLON = ':';
	
    private Label() {
        this(null);
    }
    
    public Label(final String s) {
        super(s);
    }
    
    @Override
    protected void checkCharacter(char c, String srcStr, int index) {
    	super.checkCharacter(c, srcStr, index);
    	if (c == COLON) {
    		throw new IllegalArgumentException("Label cannot contain " + COLON);
    	}
    }
}