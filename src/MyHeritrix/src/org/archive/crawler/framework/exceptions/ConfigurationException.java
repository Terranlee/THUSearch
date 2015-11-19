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

import org.archive.crawler.framework.exceptions.InitializationException;

/** ConfigurationExceptions should be thrown when a configuration file
 *   is missing data, or contains uninterpretable data, at runtime.  Fatal
 *   errors (that should cause the program to exit) should be thrown as
 *   FatalConfigurationExceptions.
 *
 *   You may optionally note the
 *
 * @author Parker Thompson
 *
 */
public class ConfigurationException extends InitializationException {

    private static final long serialVersionUID = -9078913414698851380L;

    // optionally store the file name and element so the catcher
    // can report the information and/or take other actions based on it
    protected String file = null;
    protected String element = null;

    /**
     * default constructor
     */
    public ConfigurationException() {
        super();
    }

    /** Create a ConfigurationException
     * @param message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    /** Create a ConfigurationException
     * @param cause
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    /** Create ConfigurationException
     * @param message
     * @param filename
     * @param elementname
     */
    public ConfigurationException(String message, String filename, String elementname){
        super(message);
        file = filename;
        element = elementname;
    }

    /**  Create ConfigurationException
     * @param message
     * @param cause
     * @param filename
     * @param elementname
     */
    public ConfigurationException(String message, Throwable cause, String filename, String elementname){
        super(message, cause);
        file = filename;
        element = elementname;
    }

    /** Create ConfigurationException
     * @param cause
     * @param filename
     * @param elementname
     */
    public ConfigurationException(Throwable cause, String filename, String elementname){
        super(cause);
        file = filename;
        element = elementname;
    }

    /** Store the name of the configuration file that was being parsed
     *  when this exception occured.
     * @param name
     */
    public void setFile(String name){
        file = name;
    }

    /**
     * @return name of configuration file being parsed when this exception occurred
     */
    public String getFile(){
        return file;
    }

    /** Set the name of the element that was being parsed
     *   when this exception occured.
     * @param target
     */
    public void setElement(String target){
        element = target;
    }
    /**
     * @return name of the element being parsed when this exception occurred
     */
    public String getElement(){
        return element;
    }

}
