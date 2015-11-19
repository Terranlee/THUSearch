/* WaitEvaluator
 * 
 * $Id: WaitEvaluator.java 4654 2006-09-25 20:19:54Z paul_jack $
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
package org.archive.crawler.postprocessor;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;
import org.archive.crawler.settings.SimpleType;

/**
 * A processor that determines when a URI should be revisited next. Does
 * <b>not</b> account for DNS and robots.txt expiration. That should be 
 * handled seperately by the Frontiers.
 *
 * @author Kristinn Sigurdsson
 */
public class WaitEvaluator extends Processor
implements AdaptiveRevisitAttributeConstants {
    
    private static final long serialVersionUID = 7452762726125458413L;

    Logger logger = Logger.getLogger(WaitEvaluator.class.getName());
    
    /** Default wait time after initial visit. */
    public final static String ATTR_INITIAL_WAIT_INTERVAL =
        "initial-wait-interval-seconds";
    protected final static Long DEFAULT_INITIAL_WAIT_INTERVAL =
        new Long(86400); // 1 day
    /** Maximum wait between visits */
    public final static String ATTR_MAX_WAIT_INTERVAL =
        "max-wait-interval-seconds";
    protected final static Long DEFAULT_MAX_WAIT_INTERVAL =
        new Long(2419200); // 4 weeks
    /** Minimum wait between visits */
    public final static String ATTR_MIN_WAIT_INTERVAL =
        "min-wait-interval-seconds";
    protected final static Long DEFAULT_MIN_WAIT_INTERVAL =
        new Long(3600); // 1 hour
    /** Factor increase on wait when unchanged */
    public final static String ATTR_UNCHANGED_FACTOR = "unchanged-factor";
    protected final static Double DEFAULT_UNCHANGED_FACTOR = new Double(1.5); 
    /** Factor decrease on wait when changed */
    public final static String ATTR_CHANGED_FACTOR = "changed-factor";
    protected final static Double DEFAULT_CHANGED_FACTOR = new Double(1.5); 
    /** Fixed wait time for 'unknown' change status. I.e. wait time for URIs 
     *  whose content change detection is not available. */
    public final static String ATTR_DEFAULT_WAIT_INTERVAL =
        "default-wait-interval-seconds";
    protected final static Long DEFAULT_DEFAULT_WAIT_INTERVAL =
        new Long(259200); // 3 days
    /** Indicates if the amount of time the URI was overdue should be added
     *  to the wait time before the new wait time is calculated.  */
    public final static String ATTR_USE_OVERDUE_TIME = "use-overdue-time";
    protected final static Boolean DEFAULT_USE_OVERDUE_TIME = new Boolean(false); 

    /**
     * Constructor
     * 
     * @param name The name of the module
     */
    public WaitEvaluator(String name) {
        this(name, 
                "Evaluates how long to wait before fetching a URI again. " +
                "Typically, this processor should be in the post processing " +
                "chain. It will pass if another wait evaluator has already " +
                "processed the CrawlURI.",
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
    public WaitEvaluator(String name, String description,
            Long default_inital_wait_interval,
            Long default_max_wait_interval,
            Long default_min_wait_interval,
            Double default_unchanged_factor,
            Double default_changed_factor){
        super(name, description);
        
        addElementToDefinition(new SimpleType(ATTR_INITIAL_WAIT_INTERVAL,
                "The initial wait time between revisits. Will then be " +
                "updated according to crawler experiance. I.e. shorter " +
                "wait, visit more often, if document has changed between " +
                "visits, and vica versa.",
                default_inital_wait_interval));
        addElementToDefinition(new SimpleType(ATTR_MAX_WAIT_INTERVAL,
                "The maximum settable wait time between revisits. Once a " +
                "URIs wait time reaches this value, it will not grow " +
                "further, regardless of subsequent visits that discover " +
                "no changes. Note that this does not ensure that the URI " +
                "does not wait any longer, since the crawler might be " +
                "'behind,' forcing a URI to wait until other URIs, " +
                "scheduled for earlier are completed..",
                default_max_wait_interval));
        addElementToDefinition(new SimpleType(ATTR_MIN_WAIT_INTERVAL,
                "The minum settable wait time between revisits. Once a " +
                "URIs wait time reaches this value, it will not be shortened " +
                "further, regardlesss of subsequent visits that discover " +
                "changes.",
                default_min_wait_interval));
        addElementToDefinition(new SimpleType(ATTR_DEFAULT_WAIT_INTERVAL,
                "Fixed wait time for 'unknown' change status. I.e. wait time " +
                "for URIs whose content change detection is not available.",
                DEFAULT_DEFAULT_WAIT_INTERVAL));
        addElementToDefinition(new SimpleType(ATTR_UNCHANGED_FACTOR,
                "The factor by which a URIs wait time is increased when a " +
                "revisit reveals an unchanged document. A value of 1 will " +
                "leave it unchanged, a value of 2 will double it etc.",
                default_unchanged_factor));
        addElementToDefinition(new SimpleType(ATTR_CHANGED_FACTOR,
                "The factor by which a URIs wait time is decreased when a " +
                "revisit reveals a changed document. A value of 1 will leave " +
                "it unchanged, a value of two will half it etc.",
                default_changed_factor));
        addElementToDefinition(new SimpleType(ATTR_USE_OVERDUE_TIME,
                "Indicates if the amount of time the URI was overdue should " +
                "be added to the wait time before the new wait time is " +
                "calculated.",
                DEFAULT_USE_OVERDUE_TIME));

        // Register persistent CrawlURI items 
        CrawlURI.addAlistPersistentMember(A_WAIT_INTERVAL);
    }

    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        
        if(curi.isSuccess()==false){
            // If the URI was not crawled successfully, we can not reevaluate
            // the wait interval.
            return;
        }
        
        if(curi.containsKey(A_WAIT_REEVALUATED) && 
                ((Boolean)curi.getObject(A_WAIT_REEVALUATED)).booleanValue()){
            // This CrawlURIs wait interval has already been reevaluted during
            // this processing round.
            return;
        }
            
        long min;
        try {
            min = ((Long)getAttribute(curi, ATTR_MIN_WAIT_INTERVAL)).
                longValue() * 1000;
        } catch (AttributeNotFoundException e1) {
            min = DEFAULT_MIN_WAIT_INTERVAL.longValue();
            logger.fine("Unable to load minimum wait interval for " + 
                    curi.toString());
        }

        long max;
        try {
            max = ((Long)getAttribute(curi, ATTR_MAX_WAIT_INTERVAL)).
                longValue() * 1000;
        } catch (AttributeNotFoundException e1) {
            max = DEFAULT_MAX_WAIT_INTERVAL.longValue();
            logger.fine("Unable to load maximum wait interval for " + 
                    curi.toString());
        }

        
        long waitInterval;
        if (!curi.containsKey(A_CONTENT_STATE_KEY) ||
                curi.getInt(A_CONTENT_STATE_KEY) == CONTENT_UNKNOWN) {
            try {
                waitInterval = ((Long)getAttribute(curi,
                        ATTR_DEFAULT_WAIT_INTERVAL)).longValue() * 1000;
            } catch (AttributeNotFoundException e1) {
                waitInterval = DEFAULT_DEFAULT_WAIT_INTERVAL.longValue();
                logger.fine("Unable to load default wait interval for "
                        + curi.toString());
            }
        } else {
            /* Calculate curi's time of next processing */ 
            waitInterval = DEFAULT_INITIAL_WAIT_INTERVAL.longValue()*1000;

            // Retrieve wait interval
            if(curi.containsKey(A_WAIT_INTERVAL)){
                waitInterval =  curi.getLong(A_WAIT_INTERVAL); 

                // Should override time be taken into account?
                boolean useOverrideTime = DEFAULT_USE_OVERDUE_TIME.booleanValue();
                try {
                    useOverrideTime = ((Boolean)getAttribute(
                            curi,ATTR_USE_OVERDUE_TIME)).booleanValue();
                } catch (AttributeNotFoundException e1) {
                    useOverrideTime = DEFAULT_USE_OVERDUE_TIME.booleanValue();
                    logger.fine("Unable to load use-overdue-time for " + 
                            curi.toString());
                }
                
                if(useOverrideTime){
                    waitInterval += curi.getLong(A_FETCH_OVERDUE);
                }

                // Revise the wait interval
                if(curi.getInt(A_CONTENT_STATE_KEY) == CONTENT_CHANGED){
                    // Had changed. Decrease wait interval time.
                    double factor;
                    try {
                        factor = ((Double)getAttribute(
                                curi,ATTR_CHANGED_FACTOR)).doubleValue();
                    } catch (AttributeNotFoundException e2) {
                        factor = DEFAULT_CHANGED_FACTOR.doubleValue();
                        logger.fine("Unable to load changed factor for " + 
                                curi.toString());
                    }
                    waitInterval = (long)(waitInterval / factor);
                } else if(curi.getInt(A_CONTENT_STATE_KEY) ==
                        CONTENT_UNCHANGED) {
                    // Had not changed. Increase wait interval time
                    double factor;
                    try {
                        factor = ((Double)getAttribute(
                                curi,ATTR_UNCHANGED_FACTOR)).doubleValue();
                    } catch (AttributeNotFoundException e2) {
                        factor = DEFAULT_UNCHANGED_FACTOR.doubleValue();
                        logger.fine("Unable to load unchanged factor for " + 
                                curi.toString());
                    }
                    waitInterval = (long)(waitInterval*factor);
                }
            } else {
                // If wait element not found, use initial wait interval 
                try {
                    waitInterval = ((Long)getAttribute(
                            curi,ATTR_INITIAL_WAIT_INTERVAL)).longValue()*1000;
                } catch (AttributeNotFoundException e1) {
                    // If this fails use default (already set) and log error.
                    logger.fine("Unable to load initial wait interval for " + 
                            curi.toString());
                }        
            }
        }
        
        if(waitInterval < min){
            waitInterval = min;
        } else if(waitInterval > max){
            waitInterval = max;
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("URI " + curi.toString() + ", change: "
                    + curi.getInt(A_CONTENT_STATE_KEY) + " new wait interval: "
                    + waitInterval);
        }
        // Update wait interval
        curi.putLong(A_WAIT_INTERVAL,waitInterval);
        curi.putObject(A_WAIT_REEVALUATED,new Boolean(true));
    }
}
