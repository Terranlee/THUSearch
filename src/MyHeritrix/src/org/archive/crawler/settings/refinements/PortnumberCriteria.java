/* PortnumberCriteria
 *
 * $Id: PortnumberCriteria.java 3704 2005-07-18 17:30:21Z stack-sf $
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
 * A refinement criterion that checks if a URI matches a specific port number.
 * <p/>
 * If the port number is not known it will try to use the default port number
 * for the URI's scheme.
 *
 * @author John Erik Halse
 */
public class PortnumberCriteria implements Criteria {
    private int portNumber = 0;

    /**
     * Create a new instance of PortnumberCriteria.
     */
    public PortnumberCriteria() {
        super();
    }

    /**
     * Create a new instance of PortnumberCriteria.
     *
     * @param portNumber the port number for this criteria.
     */
    public PortnumberCriteria(String portNumber) {
        setPortNumber(portNumber);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#isWithinRefinementBounds(org.archive.crawler.datamodel.UURI, int)
     */
    public boolean isWithinRefinementBounds(UURI uri) {
        int port = uri.getPort();
        if (port < 0) {
            if (uri.getScheme().equals("http")) {
                port = 80;
            } else if (uri.getScheme().equals("https")) {
                port = 443;
            }
        }

        return (port == portNumber)? true: false;
    }

    /**
     * Get the port number that is to be checked against a URI.
     *
     * @return Returns the portNumber.
     */
    public String getPortNumber() {
        return String.valueOf(portNumber);
    }
    /**
     * Set the port number that is to be checked against a URI.
     *
     * @param portNumber The portNumber to set.
     */
    public void setPortNumber(String portNumber) {
        this.portNumber = Integer.parseInt(portNumber);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getName()
     */
    public String getName() {
        return "Port number criteria";
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getDescription()
     */
    public String getDescription() {
        return "Accept URIs on port " + getPortNumber();
    }
}
