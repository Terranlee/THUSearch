/* RegularExpressionCriteria
 *
 * $Id: RegularExpressionCriteria.java 3704 2005-07-18 17:30:21Z stack-sf $
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
import org.archive.util.TextUtils;


/**
 * A refinement criteria that test if a URI matches a regular expression.
 *
 * @author John Erik Halse
 */
public class RegularExpressionCriteria implements Criteria {
    private String regexp = "";

    /**
     * Create a new instance of RegularExpressionCriteria.
     */
    public RegularExpressionCriteria() {
        super();
    }

    /**
     * Create a new instance of RegularExpressionCriteria initializing it with
     * a regular expression.
     *
     * @param regexp the regular expression for this criteria.
     */
    public RegularExpressionCriteria(String regexp) {
        setRegexp(regexp);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#isWithinRefinementBounds(org.archive.crawler.datamodel.UURI, int)
     */
    public boolean isWithinRefinementBounds(UURI uri) {
        return (uri == null || uri == null)?
            false: TextUtils.matches(regexp, uri.toString());
    }

    /**
     * Get the regular expression to be matched against a URI.
     *
     * @return Returns the regexp.
     */
    public String getRegexp() {
        return regexp;
    }
    /**
     * Set the regular expression to be matched against a URI.
     *
     * @param regexp The regexp to set.
     */
    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getName()
     */
    public String getName() {
        return "Regular expression criteria";
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getDescription()
     */
    public String getDescription() {
        return "Accept URIs that match the following regular expression: "
            + getRegexp();
    }
}
