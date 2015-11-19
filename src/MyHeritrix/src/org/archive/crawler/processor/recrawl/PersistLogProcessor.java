/* PersistLogProcessor.java
 * 
 * Created on Feb 18, 2005
 *
 * Copyright (C) 2007 Internet Archive.
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
package org.archive.crawler.processor.recrawl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.io.CrawlerJournal;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;



/**
 * Log CrawlURI attributes from latest fetch for consultation by a later 
 * recrawl. Log must be imported into alternate data structure in order
 * to be consulted. 
 * 
 * @author gojomo
 * @version $Date: 2006-09-25 20:19:54 +0000 (Mon, 25 Sep 2006) $, $Revision: 4654 $
 */
public class PersistLogProcessor extends PersistProcessor implements CrawlStatusListener {
    private static final long serialVersionUID = 1678691994065439346L;
    
    protected CrawlerJournal log;

    /** setting for log filename */
    public static final String ATTR_LOG_FILENAME = "log-filename";
    /** default log filename */ 
    public static final String DEFAULT_LOG_FILENAME = "persistlog.txtser.gz";
    
    /**
     * Usual constructor
     * 
     * @param name
     */
    public PersistLogProcessor(String name) {
        super(name, "PersistLogProcessor. Logs CrawlURI attributes " +
                "from latest fetch for consultation by a later recrawl.");
        
        addElementToDefinition(new SimpleType(ATTR_LOG_FILENAME,
                "Filename to which to log URI persistence information. " +
                "Interpreted relative to job logs directory. " +
                "Default is 'persistlog.txtser.gz'. ", 
                DEFAULT_LOG_FILENAME));
    }

    @Override
    protected void initialTasks() {
        // do not create persist log if processor is disabled
        if (!isEnabled()) {
            return;
        }

        // Add this class to crawl state listeners to note checkpoints
        getController().addCrawlStatusListener(this);
        try {
            File logFile = FileUtils.maybeRelative(getController().getLogsDir(),
                    (String) getUncheckedAttribute(null, ATTR_LOG_FILENAME));
            log = new CrawlerJournal(logFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected void finalTasks() {
        if (log != null) {
            log.close();
        }
    }

    @Override
    protected void innerProcess(CrawlURI curi) {
        if(shouldStore(curi)) {
            log.writeLine(persistKeyFor(curi), " ", new String(Base64.encodeBase64(IoUtils
                    .serializeToByteArray(curi.getPersistentAList()))));      
        }
    }

    public void crawlCheckpoint(File checkpointDir) throws Exception {
        // rotate log
        log.checkpoint(checkpointDir);
    }

    public void crawlEnded(String sExitMessage) {
        // ignored
        
    }

    public void crawlEnding(String sExitMessage) {
        // ignored
        
    }

    public void crawlPaused(String statusMessage) {
        // ignored
        
    }

    public void crawlPausing(String statusMessage) {
        // ignored
        
    }

    public void crawlResuming(String statusMessage) {
        // ignored
        
    }

    public void crawlStarted(String message) {
        // ignored
    }
}