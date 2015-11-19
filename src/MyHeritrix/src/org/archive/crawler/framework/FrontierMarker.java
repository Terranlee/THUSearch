/* URIFrontierMarker
 *
 * $Id: FrontierMarker.java 2593 2004-09-30 02:16:06Z gojomo $
 *
 * Created on Feb 25, 2004
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
package org.archive.crawler.framework;


/**
 * A marker is a pointer to a place somewhere inside a frontier's list of
 * pending URIs. URIFrontiers use them to allow outside classes (UI for
 * example) to hold (effectively) pointers into the abstract list of pending
 * URIs inside the frontier. If the crawl is not paused (i.e. running) the
 * marker will instantly become out of date.
 *
 * @author Kristinn Sigurdsson
 */
public interface FrontierMarker {

    /**
     * Returns the regular expression that this marker uses.
     * @return the regular expression that this marker uses
     */
    public String getMatchExpression();

    /**
     * Returns the number of the next match after the marker.
     * Alternatively this can be viewed as n-1, where n is the number of items
     * found before the marker.
     * @return the number of the next match after the marker
     */
    public long getNextItemNumber();

    /**
     * Returns false if no more URIs can be found matching the expression
     * beyond those already covered. True otherwise.
     * @return Are there any more matches.
     */
    public boolean hasNext();
}
