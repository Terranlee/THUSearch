/* Constants
 * 
 * $Id: AdaptiveRevisitAttributeConstants.java 5796 2008-03-25 21:53:04Z gojomo $
 * 
 * Created on 26.11.2004
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
package org.archive.crawler.frontier;

import org.archive.crawler.datamodel.CoreAttributeConstants;

/**
 * Defines static constants for the Adaptive Revisiting module defining data
 * keys in the CrawlURI AList. 
 *
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.datamodel.CoreAttributeConstants
 */
public interface AdaptiveRevisitAttributeConstants
extends CoreAttributeConstants {

    /** Designates a field in the CrawlURIs AList for the content digest of
     *  an earlier visit. */
    public static final String A_LAST_CONTENT_DIGEST = "last-content-digest";
    public static final String A_TIME_OF_NEXT_PROCESSING = 
        "time-of-next-processing";
    public static final String A_WAIT_INTERVAL = "wait-interval";
    public static final String A_NUMBER_OF_VISITS = "number-of-visits";
    public static final String A_NUMBER_OF_VERSIONS = "number-of-versions";
    public static final String A_FETCH_OVERDUE = "fetch-overdue";
    
    public static final String A_LAST_ETAG = "last-etag";
    public static final String A_LAST_DATESTAMP = "last-datestamp";
    
    public static final String A_WAIT_REEVALUATED = "wait-reevaluated";
    
    /** Mark a URI to be dropped from revisit handling. Used for custom 
     * processors that want to implement more selective revisiting. 
     * Actual effect depends on whether an alreadyIncluded structure
     * is used. If an alreadyIncluded is used, dropping the URI from 
     * revisit handling means it won't be visited again. If an
     * alreadyIncluded is not used, this merely drops one discovery of 
     * the URI, and it may be rediscovered and thus revisited that way.
     */
    public static final String A_DISCARD_REVISIT = "no-revisit";
    
    /** No knowledge of URI content. Possibly not fetched yet, unable
     *  to check if different or an error occurred on last fetch attempt. */
    public static final int CONTENT_UNKNOWN = -1;
    
    /** URI content has not changed between the two latest, successfully
     *  completed fetches. */
    public static final int CONTENT_UNCHANGED = 0;
    
    /** URI content had changed between the two latest, successfully completed
     *  fetches. By definition, content has changed if there has only been one
     *  successful fetch made. */
    public static final int CONTENT_CHANGED = 1;

    /**
     * Key to use getting state of crawluri from the CrawlURI alist.
     */
    public static final String A_CONTENT_STATE_KEY = "ar-state";
}
