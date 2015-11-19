/* ImageWaitEvaluator
 * 
 * $Id: ImageWaitEvaluator.java 4667 2006-09-26 20:38:48Z paul_jack $
 * 
 * Created on 1.4.2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.postprocessor;

/**
 * A specialized ContentBasedWaitEvaluator. Comes preset with a regular 
 * expression that matches text documents. <code>^image/.*$</code>
 *
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.postprocessor.ContentBasedWaitEvaluator
 */
public class ImageWaitEvaluator extends ContentBasedWaitEvaluator {

    private static final long serialVersionUID = -2762377129860398333L;

    protected final static Long DEFAULT_INITIAL_WAIT_INTERVAL =
        new Long(172800); // 2 days

    protected final static String DEFAULT_CONTENT_REGEXPR = "^image/.*$"; //Text

    /**
     * Constructor
     * 
     * @param name The name of the module
     */
    public ImageWaitEvaluator(String name) {
        super(name,"Evaluates how long to wait before fetching a URI again. " +
                "Only handles CrawlURIs whose content type indicates a " +
                "image document (^image/.*$). " +
                "Typically, this processor should be in the post processing " +
                "chain. It will pass if another wait evaluator has already " +
                "processed the CrawlURI.", 
                DEFAULT_CONTENT_REGEXPR,
                DEFAULT_INITIAL_WAIT_INTERVAL,
                DEFAULT_MAX_WAIT_INTERVAL,
                DEFAULT_MIN_WAIT_INTERVAL,
                DEFAULT_UNCHANGED_FACTOR,
                DEFAULT_CHANGED_FACTOR);
    }


}
