/* Copyright (C) 2003 Internet Archive.
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
 *
 * Created on Jul 29, 2003
 *
 */
package org.archive.crawler.framework.exceptions;

/** InitializationExceptions should be thrown when there is a problem with
 *   the crawl's initialization, such as file creation problems, etc.  In the event
 *   that a more specific exception can be thrown (such as a ConfigurationException
 *   in the event that there is a configuration-specific problem) it should be.
 *
 * @author Parker Thompson
 *
 */
public class InitializationException extends Exception {

    private static final long serialVersionUID = -3482635476140606185L;

    public InitializationException() {
        super();
    }

    /**
     * @param message
     */
    public InitializationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public InitializationException(Throwable cause) {
        super(cause);
    }

}
