/* ChangeEvaluator
 * 
 * $Id: ChangeEvaluator.java 5792 2008-03-24 00:12:21Z gojomo $
 * 
 * Created on 11.11.2004
 *
 * Copyright (C) 2004 Kristinn Sigurdsson.
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
package org.archive.crawler.extractor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;
import org.archive.util.Base32;

/**
 * This processor compares the CrawlURI's current 
 * {@link org.archive.crawler.datamodel.CrawlURI#getContentDigest() content digest}
 * with the one from a previous crawl. If they are equal, then further 
 * processing is skipped (going straight to the post processor chain) and the
 * CrawlURI is marked appropriately.
 *
 * @author Kristinn Sigurdsson
 */
public class ChangeEvaluator extends Processor
implements AdaptiveRevisitAttributeConstants {

    private static final long serialVersionUID = 5547590621493534632L;
    private static final Logger logger =
        Logger.getLogger(ChangeEvaluator.class.getName());

    /**
     * Constructor
     * @param name The name of the module
     */
    public ChangeEvaluator(String name) {
        super(name, "Compares CrawlURI's current " +
                "content digest with digest from previous crawl. If " +
                "equal, further processing is skipped (going " +
                "straight to the post processor chain) and the CrawlURI is " +
                "marked appropriately. Should be located at the start of " +
                "the Extractor chain.");

        // Register persistent CrawlURI items 
        CrawlURI.addAlistPersistentMember(A_LAST_CONTENT_DIGEST);
        CrawlURI.addAlistPersistentMember(A_NUMBER_OF_VISITS);
        CrawlURI.addAlistPersistentMember(A_NUMBER_OF_VERSIONS);
    }

    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if (curi.isSuccess() == false) {
            // Early return. No point in doing comparison on failed downloads.
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Not handling " + curi.toString()
                        + ", did not " + "succeed.");
            }
            return;
        }
        
        // If a mid fetch filter aborts the HTTP fetch because the headers
        // predict no change, we can skip the whole comparing hashes.
        if (!curi.containsKey(A_CONTENT_STATE_KEY) ||
                curi.getInt(A_CONTENT_STATE_KEY) != CONTENT_UNCHANGED) {
            String currentDigest = null;
            Object digest = curi.getContentDigest();
            if (digest != null) {
                currentDigest = Base32.encode((byte[])digest);
            }
    
            String oldDigest = null;
            if (curi.containsKey(A_LAST_CONTENT_DIGEST)) {
                oldDigest = curi.getString(A_LAST_CONTENT_DIGEST);
            }
    
            // Compare the String representation of the byte arrays.
            if (currentDigest == null && oldDigest == null) {
                // Both are null, can't do a thing
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("On " + curi.toString()
                            + " both digest are null");
                }
                // NOTE! RETURN!
                return;
            }
            
            if (currentDigest != null && oldDigest != null 
                    && currentDigest.equals(oldDigest)) { 
                // If equal, we have just downloaded a duplicate.
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("On " + curi.toString()
                            + " both digest are " + "equal. Old: " + oldDigest
                            + ", new: " + currentDigest);
                }
                curi.putInt(A_CONTENT_STATE_KEY, CONTENT_UNCHANGED);
                // TODO: In the future processors should take note of the content
                // state, removing the need for the following 'skip'
                curi.skipToProcessorChain(getController().getPostprocessorChain());
                // Since this is an unchanged page, no need to reschedule all of it's links.
                curi.clearOutlinks();
                // Make not in log
                curi.addAnnotation("unchanged");
                // Set content size to zero, we are not going to 'write it to disk'
                curi.setContentSize(0);
            } else {
                // Document has changed
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("On " + curi.toString()
                            + " digest are not " + "equal. Old: "
                            + (oldDigest == null? "null": oldDigest)
                            + ", new: "
                            + (currentDigest == null? "null": currentDigest));
                }
                // currentDigest may be null, that probably means a failed download
                curi.putInt(A_CONTENT_STATE_KEY, CONTENT_CHANGED);
                curi.putString(A_LAST_CONTENT_DIGEST, currentDigest); 
            }
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("On " + curi.toString()
                        + " content state was " + "already set as UNCHANGED.");
            }
            // Just like matching digests, there is no need to continue processing.
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            // Since this is an unchanged page, no need to reschedule all of it's links.
            curi.clearOutlinks();
        }
        
        // Update visit and version counters
        int visits = 1;
        if(curi.containsKey(A_NUMBER_OF_VISITS)) {
            visits = curi.getInt(A_NUMBER_OF_VISITS) + 1;
        }
        curi.putInt(A_NUMBER_OF_VISITS, visits);

        // Update versions.
        if(curi.getInt(A_CONTENT_STATE_KEY) == CONTENT_CHANGED) {
            int versions = 1;
            if(curi.containsKey(A_NUMBER_OF_VERSIONS)) {
                versions = curi.getInt(A_NUMBER_OF_VERSIONS) + 1;
            }
            curi.putInt(A_NUMBER_OF_VERSIONS,versions);
        }
    }
}
