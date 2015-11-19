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
 * LocalizedError.java
 * Created on Oct 28, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * @author gojomo
 *
 */
public class LocalizedError {

//    private String message;
    public Throwable exception;
//    private String processorName;

    /**
     * @param processorName
     * @param ex
     * @param message
     */
    public LocalizedError(String processorName, Throwable ex, String message) {
//        this.processorName = processorName;
        this.exception = ex;
//        this.message = message;
    }

}
