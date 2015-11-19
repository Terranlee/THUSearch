/* InvalidURIFrontierMarkerException
 *
 * $Id: InvalidFrontierMarkerException.java 4664 2006-09-25 23:59:43Z paul_jack $
 *
 * Created on Feb 29, 2004
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
package org.archive.crawler.framework.exceptions;

/**
 * An exception that is thrown when there is an attempt to use a
 * <code>URIFrontierMarker</code> that has become invalid.
 * @author Kristinn Sigurdsson
 */
public class InvalidFrontierMarkerException extends Exception {

    private static final long serialVersionUID = -26552188686310984L;

    public InvalidFrontierMarkerException(){
        super("URIFrontierMarker does not match URIFrontier internal state.");
    }
}
