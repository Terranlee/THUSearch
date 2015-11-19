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
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;

/**
 * Base class for URI processing classes.
 *
 * <p> Each URI is processed by a user defined series of processors. This class
 * provides the basic infrastructure for these but does not actually do
 * anything. New processors can be easily created by subclassing this class.
 *
 * <p> Classes subclassing this one should not trap InterruptedExceptions.
 * They should be allowed to propagate to the ToeThread executing the processor.
 * Also they should immediately exit their main method (<tt>innerProcess()</tt>)
 * if the <tt>interrupted</tt> flag is set.
 *
 * @author Gordon Mohr
 *
 * @see org.archive.crawler.framework.ToeThread
 */
public class Processor extends ModuleType {

    private static final long serialVersionUID = 6248563827413710226L;

    /**
     * Key to use asking settings for decide-rules value.
     */
    public static final String ATTR_DECIDE_RULES = "decide-rules";
    /** local name for decide-rules */
    protected String attrDecideRules; 

    /**
     * Key to use asking settings for enabled value.
     */
    public final static String ATTR_ENABLED = "enabled";

    private Processor defaultNextProcessor = null;

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.framework.Processor");

    /**
     * @param name
     * @param description
     */
    public Processor(String name, String description) {
        super(name, description);
        addElementToDefinition(new SimpleType(ATTR_ENABLED,
            "Is processor enabled", new Boolean(true)));
        attrDecideRules = getName()+"#"+ATTR_DECIDE_RULES;
        addElementToDefinition(
            new DecideRuleSequence(attrDecideRules,
                "DecideRules which, if their final decision is REJECT, " +
                "prevent this Processor from running."));
    }

    /**
     * Perform processing on the given CrawlURI.
     *
     * @param curi
     * @throws InterruptedException
     */
    public final void process(CrawlURI curi) throws InterruptedException {
        // by default, arrange for curi to proceed to next processor
        curi.setNextProcessor(getDefaultNextProcessor(curi));

        // Check if this processor is enabled before processing
        try {
            if (!((Boolean) getAttribute(ATTR_ENABLED, curi)).booleanValue()) {
                return;
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }

        if(rulesAccept(curi)) {
            innerProcess(curi);
        } else {
            innerRejectProcess(curi);
        }
    }

    protected void checkForInterrupt() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("interrupted");
        }
    }

    /**
     * @param curi CrawlURI instance.
     * @throws InterruptedException
     */
    protected void innerRejectProcess(CrawlURI curi)
    throws InterruptedException {
        // by default do nothing
    }

    /**
     * Classes subclassing this one should override this method to perform
     * their custom actions on the CrawlURI.
     *
     * @param curi The CrawlURI being processed.
     * @throws InterruptedException
     */
    protected void innerProcess(CrawlURI curi)
    throws InterruptedException {
        // by default do nothing
    }

    /**
     * Classes subclassing this one should override this method to perform
     * processor specific actions.
     * <p>
     *
     * This method is garanteed to be called after the crawl is set up, but
     * before any URI-processing has occured.
     */
    protected void initialTasks () {
        // by default do nothing
    }

    /**
     * Classes subclassing this one should override this method to perform
     * processor specific actions.
     *
     */
    protected void finalTasks () {
        // by default do nothing
    }

    protected DecideRule getDecideRule(Object o) {
        try {
            return (DecideRule)getAttribute(o, attrDecideRules);
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean rulesAccept(Object o) {
        return rulesAccept(getDecideRule(o),o);
    }

    protected boolean rulesAccept(DecideRule rule, Object o) {
        return rule.decisionFor(o) != DecideRule.REJECT;
    }
    /**
     * Returns the next processor for the given CrawlURI in the processor chain.
     * @param curi The CrawlURI that we want to find the next processor for.
     * @return The next processor for the given CrawlURI in the processor chain.
     */
    public Processor getDefaultNextProcessor(CrawlURI curi) {
        return defaultNextProcessor;
    }

    /** Set the default next processor in the chain.
     *
     * @param nextProcessor the default next processor in the chain.
     */
    public void setDefaultNextProcessor(Processor nextProcessor) {
        defaultNextProcessor = nextProcessor;
    }

    /** 
     * Get the controller object.
     *
     * @return the controller object.
     */
    public CrawlController getController() {
        return getSettingsHandler().getOrder().getController();
    }

    public Processor spawn(int serialNum) {
        Processor newInst = null;
        try {
            Constructor co =
                getClass().getConstructor(new Class[] { String.class });
            newInst =
                (Processor) co.newInstance(new Object[] {
                    getName() + serialNum
                    });
            getParent().setAttribute(newInst);
            newInst.setTransient(true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newInst;
    }

    /**
     * Compiles and returns a report (in human readable form) about the status
     * of the processor.  The processor's name (of implementing class) should
     * always be included.
     * <p>
     * Examples of stats declared would include:<br>
     * * Number of CrawlURIs handled.<br>
     * * Number of links extracted (for link extractors)<br>
     * etc.
     *
     * @return A human readable report on the processor's state.
     */
    public String report(){
        return ""; // Default behavior.
    }
    
    /**
     * @param curi CrawlURI to examine.
     * @return True if content to process -- content length is > 0 
     * -- and links have not yet been extracted.
     */
    protected boolean isContentToProcess(CrawlURI curi) {
        return !curi.hasBeenLinkExtracted() && curi.getContentLength() > 0;
    }
    
    /**
     * @param curi CrawlURI to examine.
     * @return True if {@link #isContentToProcess(CrawlURI)} and
     * the CrawlURI represents a successful http transaction.
     */
    protected boolean isHttpTransactionContentToProcess(CrawlURI curi) {
        return isContentToProcess(curi) &&
            curi.isHttpTransaction() &&
            curi.isSuccess();
    }
    
    /**
     * @param contentType Found content type.
     * @param expectedPrefix String to find at start of contenttype: e.g.
     * <code>text/html</code>.
     * @return True if passed content-type begins with
     * expected mimetype.
     */
    protected boolean isExpectedMimeType(String contentType,
            String expectedPrefix) {
        return contentType != null &&
            contentType.toLowerCase().startsWith(expectedPrefix);
    }

    public void kickUpdate() {
        // by default do nothing
    }
    
    public boolean isEnabled() {
        return ((Boolean)getUncheckedAttribute(null, ATTR_ENABLED)).booleanValue();
    }
}
