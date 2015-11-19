/* InvalidJobFileException.java
 *
 * $Id: InvalidJobFileException.java 4666 2006-09-26 17:53:28Z paul_jack $
 *
 * Created on Mar 8, 2004
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
package org.archive.crawler.admin;


/**
 * An exception that is thrown when a program encounters a jobfile that is
 * corrupt or otherwise incomplete or invalid.
 * @author Kristinn Sigurdsson
 */
public class InvalidJobFileException extends Exception {

    private static final long serialVersionUID = -5162130672800789699L;

    public InvalidJobFileException(String message){
        super(message);
    }
}
