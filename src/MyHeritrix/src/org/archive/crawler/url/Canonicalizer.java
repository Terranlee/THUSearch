/* Canonicalizer
 * 
 * Created on Oct 7, 2004
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
package org.archive.crawler.url;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.settings.MapType;
import org.archive.net.UURI;

/**
 * URL canonicalizer.
 * @author stack
 * @version $Date: 2006-09-26 20:38:48 +0000 (Tue, 26 Sep 2006) $, $Revision: 4667 $
 */
public class Canonicalizer {
    private static Logger logger =
        Logger.getLogger(Canonicalizer.class.getName());
    
    /**
     * Constructor.
     * This class can't be constructed.
     * Shutdown.
     */
    private Canonicalizer() {
        super();
    }
    
    /**
     * Convenience method that is passed a settings object instance pulling
     * from it what it needs to canonicalize.
     * @param uuri UURI to canonicalize.
     * @param order A crawlorder instance.
     * @return Canonicalized string of uuri else uuri if an error.
     */
    public static String canonicalize(UURI uuri, CrawlOrder order) {
        MapType rules = null;
        String canonical = uuri.toString();
        try {
            rules = (MapType)order.getAttribute(uuri, CrawlOrder.ATTR_RULES);
            canonical = Canonicalizer.canonicalize(uuri, rules.iterator(uuri));
        } catch (AttributeNotFoundException e) {
            logger.warning("Failed canonicalization of " + canonical +
                ": " + e);
        }
        return canonical;
    }

    /**
     * Run the passed uuri through the list of rules.
     * @param uuri Url to canonicalize.
     * @param rules Iterator of canonicalization rules to apply (Get one
     * of these on the url-canonicalizer-rules element in order files or
     * create a list externally).  Rules must implement the Rule interface.
     * @return Canonicalized URL.
     */
    public static String canonicalize(UURI uuri, Iterator rules) {
        String before = uuri.toString();
        //String beforeRule = null;
        String canonical = before;
        for (; rules.hasNext();) {
            CanonicalizationRule r = (CanonicalizationRule)rules.next();
            //if (logger.isLoggable(Level.FINER)) {
            //    beforeRule = canonical;
            //}
            if (!r.isEnabled(uuri)) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Rule " + r.getName() + " is disabled.");
                }
                continue;
            }
            canonical = r.canonicalize(canonical, uuri);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Rule " + r.getName() + " " + before + " => " +
                        canonical);
            }
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.fine(before + " => " + canonical);
        }
        return canonical;
    }
}
