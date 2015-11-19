/* Criteria
 *
 * $Id: Criteria.java 3704 2005-07-18 17:30:21Z stack-sf $
 *
 * Created on Apr 8, 2004
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
package org.archive.crawler.settings.refinements;

import org.archive.net.UURI;


/**
 * Superclass for the refinement criteria.
 *
 * @author John Erik Halse.
 */
public interface Criteria {
    /**
     * Check if a uri is within the bounds of this criteria.
     *
     * @param uri the UURI to check.
     * @return true if the curi is within the bounds.
     */
    public boolean isWithinRefinementBounds(UURI uri);

    /**
     * Returns the name of the Criteria type.
     * @return the name of the Criteria type
     */
    public String getName();

    /**
     * Returns a description of the Criteria's current settings.
     * @return a description of the Criteria's current settings.
     */
    public String getDescription();
}
