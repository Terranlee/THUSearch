/* ContentBasedWaitEvaluator
 * 
 * $Id: ContentBasedWaitEvaluator.java 4654 2006-09-25 20:19:54Z paul_jack $
 * 
 * Created on 1.4.2005
 *
 * Copyright (C) 2005 Kristinn Sigurdsson
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

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;

/**
 * A WaitEvaluator that compares the CrawlURIs content type to a configurable
 * regular expression. If it matches, then the wait evaluation is performed.
 * Otherwise the processor passes on the CrawlURI, doing nothing. 
 *
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.postprocessor.WaitEvaluator
 */
public class ContentBasedWaitEvaluator extends WaitEvaluator {
    
    private static final long serialVersionUID = 1623347208782997347L;

    /** The regular expression that we limit this evaluator to. */
    public final static String ATTR_CONTENT_REGEXPR =
        "content-regular-expression";
    protected final static String DEFAULT_CONTENT_REGEXPR = "^.*$"; //Everything

    /**
     * Constructor
     * 
     * @param name The name of the module
     */
    public ContentBasedWaitEvaluator(String name) {
        this(name,"Evaluates how long to wait before fetching a URI again. " +
                "Only handles CrawlURIs whose content type matches the " +
                "regular expression set. " +
                "Typically, this processor should be in the post processing " +
                "chain. It will pass if another wait evaluator has already " +
                "processed the CrawlURI.", DEFAULT_CONTENT_REGEXPR,
                DEFAULT_INITIAL_WAIT_INTERVAL,
                DEFAULT_MAX_WAIT_INTERVAL,
                DEFAULT_MIN_WAIT_INTERVAL,
                DEFAULT_UNCHANGED_FACTOR,
                DEFAULT_CHANGED_FACTOR);
    }

    /**
     * Constructor
     * 
     * @param name The name of the module
     * @param description Description of the module
     * @param default_inital_wait_interval The default value for initial wait
     *           time
     * @param default_max_wait_interval The maximum value for wait time
     * @param default_min_wait_interval The minimum value for wait time
     * @param default_unchanged_factor The factor for changing wait times of
     *           unchanged documents (will be multiplied by this value)
     * @param default_changed_factor The factor for changing wait times of
     *           changed documents (will be divided by this value)
     */
    public ContentBasedWaitEvaluator(String name, String description,
            String defaultRegExpr,
            Long default_inital_wait_interval,
            Long default_max_wait_interval,
            Long default_min_wait_interval,
            Double default_unchanged_factor,
            Double default_changed_factor){
        super(name,description,
                default_inital_wait_interval,
                default_max_wait_interval,
                default_min_wait_interval,
                default_unchanged_factor,
                default_changed_factor);

        addElementToDefinition(new SimpleType(ATTR_CONTENT_REGEXPR,
                "Only URIs whose content type matches this regular " +
                "expression will be evaluated.",
                defaultRegExpr));

    }
    
    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        // Check if content type is available and if it matches the reg.expr.
        String content_type = curi.getContentType();
        if(content_type==null){
            // No content type, exit
            return;
        }
        String regexpr;
        try {
            regexpr = (String)getAttribute(curi,ATTR_CONTENT_REGEXPR);
        } catch (AttributeNotFoundException e) {
            logger.warning("Regular expression for content type not found");
            return;
        }

        if(TextUtils.matches(regexpr, content_type) == false){
            // Content type does not match reg.expr. Exit
            return;
        }
        // Ok, it matches, invoke parent method.

        super.innerProcess(curi);
    }
}
