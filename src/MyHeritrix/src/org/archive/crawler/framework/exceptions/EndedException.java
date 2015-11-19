/*
 * Created on Aug 11, 2004
 *
 */
package org.archive.crawler.framework.exceptions;

/**
 * Indicates a crawl has ended, either due to operator
 * termination, frontier exhaustion, or any other reason. 
 * 
 * @author gojomo
 */
public class EndedException extends Exception {
    // TODO: subclass for various kinds of ends?
    
    private static final long serialVersionUID = -4638427249822262643L;

    /**
     * Constructs a new <code>EndedException</code>.
     * 
     * @param message  describes why the crawl ended
     */
    public EndedException(String message) {
        super(message);
    }

}
