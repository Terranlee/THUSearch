/* LowercaseRule
 * 
 * Created on Oct 5, 2004
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
package org.archive.crawler.url.canonicalize;




/**
 * Lowercases the URL.
 * @author stack
 * @version $Date: 2006-09-25 20:27:35 +0000 (Mon, 25 Sep 2006) $, $Revision: 4655 $
 */
public class LowercaseRule
extends BaseRule {

    private static final long serialVersionUID = -4732482198714929052L;
    
    private static final String DESCRIPTION = "Lowercase the URL. " +
        "Use this rule to lowercase the url.  Operates on all schemes.";

    public LowercaseRule(String name) {
        super(name, DESCRIPTION);
    }

    public String canonicalize(String url, Object context) {
        return url.toLowerCase();
    }
}
