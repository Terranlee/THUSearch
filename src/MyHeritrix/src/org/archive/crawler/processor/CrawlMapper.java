/* CrawlMapper
 * 
 * Created on Sep 30, 2005
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
package org.archive.crawler.processor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.ArchiveUtils;
import org.archive.util.fingerprint.ArrayLongFPCache;

import st.ata.util.FPGenerator;

/**
 * A simple crawl splitter/mapper, dividing up CandidateURIs/CrawlURIs
 * between crawlers by diverting some range of URIs to local log files
 * (which can then be imported to other crawlers). 
 * 
 * May operate on a CrawlURI (typically early in the processing chain) or
 * its CandidateURI outlinks (late in the processing chain, after 
 * LinksScoper), or both (if inserted and configured in both places). 
 * 
 * <p>Applies a map() method, supplied by a concrete subclass, to
 * classKeys to map URIs to crawlers by name. 
 *
 * <p>One crawler name is distinguished as the 'local name'; URIs mapped to
 * this name are not diverted, but continue to be processed normally.
 *
 * <p>If using the JMX importUris operation importing URLs dropped by
 * a {@link CrawlMapper} instance, use <code>recoveryLog</code> style.
 * 
 * @author gojomo
 * @version $Date: 2007-06-07 21:34:56 +0000 (Thu, 07 Jun 2007) $, $Revision: 5199 $
 */
public abstract class CrawlMapper extends Processor implements FetchStatusCodes {
    /**
     * PrintWriter which remembers the File to which it writes. 
     */
    private class FilePrintWriter extends PrintWriter {
        File file; 
        public FilePrintWriter(File file) throws FileNotFoundException {
            super(new BufferedOutputStream(new FileOutputStream(file)));
            this.file = file; 
        }
        public File getFile() {
            return file;
        }
    }
    
    /** whether to map CrawlURI itself (if status nonpositive) */
    public static final String ATTR_CHECK_URI = "check-uri";
    public static final Boolean DEFAULT_CHECK_URI = Boolean.TRUE;
    
    /** whether to map CrawlURI's outlinks (if CandidateURIs) */
    public static final String ATTR_CHECK_OUTLINKS = "check-outlinks";
    public static final Boolean DEFAULT_CHECK_OUTLINKS = Boolean.TRUE;

    /** decide rules to determine if an outlink is subject to mapping */ 
    public static final String ATTR_MAP_OUTLINK_DECIDE_RULES = "decide-rules";

    /** name of local crawler (URIs mapped to here are not diverted) */
    public static final String ATTR_LOCAL_NAME = "local-name";
    public static final String DEFAULT_LOCAL_NAME = ".";
    
    /** where to log diversions  */
    public static final String ATTR_DIVERSION_DIR = "diversion-dir";
    public static final String DEFAULT_DIVERSION_DIR = "diversions";

    /** rotate logs when change occurs within this # of digits of timestamp  */
    public static final String ATTR_ROTATION_DIGITS = "rotation-digits";
    public static final Integer DEFAULT_ROTATION_DIGITS = new Integer(10); // hourly
    
    /**
     * Mapping of target crawlers to logs (PrintWriters)
     */
    HashMap<String,PrintWriter> diversionLogs
     = new HashMap<String,PrintWriter>();

    /**
     * Truncated timestamp prefix for diversion logs; when
     * current time doesn't match, it's time to close all
     * current logs. 
     */
    String logGeneration = "";
    
    /** name of the enclosing crawler (URIs mapped here stay put) */
    protected String localName;
    
    protected ArrayLongFPCache cache;
    
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public CrawlMapper(String name, String description) {
        super(name, description);
        addElementToDefinition(new SimpleType(ATTR_LOCAL_NAME,
            "Name of local crawler node; mappings to this name " +
            "result in normal processing (no diversion).",
            DEFAULT_LOCAL_NAME));
        addElementToDefinition(new SimpleType(ATTR_DIVERSION_DIR,
            "Directory to write diversion logs.",
            DEFAULT_DIVERSION_DIR));
        addElementToDefinition(new SimpleType(ATTR_CHECK_URI,
            "Whether to apply the mapping to a URI being processed " +
            "itself, for example early in processing (while its " +
            "status is still 'unattempted').",
            DEFAULT_CHECK_URI));
        addElementToDefinition(new SimpleType(ATTR_CHECK_OUTLINKS,
            "Whether to apply the mapping to discovered outlinks, " +
            "for example after extraction has occurred. ",
            DEFAULT_CHECK_OUTLINKS));
        addElementToDefinition(new DecideRuleSequence(
                ATTR_MAP_OUTLINK_DECIDE_RULES));
        addElementToDefinition(new SimpleType(ATTR_ROTATION_DIGITS,
                "Number of timestamp digits to use as prefix of log " +
                "names (grouping all diversions from that period in " +
                "a single log). Default is 10 (hourly log rotation).",
                DEFAULT_ROTATION_DIGITS));
    }


    protected void innerProcess(CrawlURI curi) {
        String nowGeneration = 
            ArchiveUtils.get14DigitDate().substring(
                        0,
                        ((Integer) getUncheckedAttribute(null,
                                ATTR_ROTATION_DIGITS)).intValue());
        if(!nowGeneration.equals(logGeneration)) {
            updateGeneration(nowGeneration);
        }
        
        if (curi.getFetchStatus() <= 0 // unfetched/unsuccessful
                && ((Boolean) getUncheckedAttribute(null, ATTR_CHECK_URI))
                        .booleanValue()) {
            // apply mapping to the CrawlURI itself
            String target = map(curi);
            if(!localName.equals(target)) {
                // CrawlURI is mapped to somewhere other than here
                curi.setFetchStatus(S_BLOCKED_BY_CUSTOM_PROCESSOR);
                curi.addAnnotation("to:"+target);
                curi.skipToProcessorChain(getController().
                        getPostprocessorChain());
                divertLog(curi,target);
            } else {
                // localName means keep locally; do nothing
            }
        }
        
        if ((Boolean) getUncheckedAttribute(null, ATTR_CHECK_OUTLINKS)) {
            // consider outlinks for mapping
            Iterator<CandidateURI> iter = curi.getOutCandidates().iterator();
            while(iter.hasNext()) {
                CandidateURI cauri = iter.next();
                if (decideToMapOutlink(cauri)) {
                    // apply mapping to the CandidateURI
                    String target = map(cauri);
                    if(!localName.equals(target)) {
                        // CandidateURI is mapped to somewhere other than here
                        iter.remove();
                        divertLog(cauri,target);
                    } else {
                        // localName means keep locally; do nothing
                    }
                }
            }
        }
    }
    
    protected boolean decideToMapOutlink(CandidateURI cauri) {
        boolean rejected = getMapOutlinkDecideRule(cauri).decisionFor(cauri)
                .equals(DecideRule.REJECT);
        return !rejected;
    }

    protected DecideRule getMapOutlinkDecideRule(Object o) {
        try {
            return (DecideRule)getAttribute(o, ATTR_MAP_OUTLINK_DECIDE_RULES);
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * Close and mark as finished all existing diversion logs, and
     * arrange for new logs to use the new generation prefix.
     * 
     * @param nowGeneration new generation (timestamp prefix) to use
     */
    protected synchronized void updateGeneration(String nowGeneration) {
        // all existing logs are of a previous generation
        Iterator iter = diversionLogs.values().iterator();
        while(iter.hasNext()) {
            FilePrintWriter writer = (FilePrintWriter) iter.next();
            writer.close();
            writer.getFile().renameTo(
                    new File(writer.getFile().getAbsolutePath()
                            .replaceFirst("\\.open$", ".divert")));
        }
        diversionLogs.clear();
        logGeneration = nowGeneration;
    }

    /**
     * Look up the crawler node name to which the given CandidateURI 
     * should be mapped. 
     * 
     * @param cauri CandidateURI to consider
     * @return String node name which should handle URI
     */
    protected abstract String map(CandidateURI cauri);

    
    /**
     * Note the given CandidateURI in the appropriate diversion log. 
     * 
     * @param cauri CandidateURI to append to a diversion log
     * @param target String node name (log name) to receive URI
     */
    protected synchronized void divertLog(CandidateURI cauri, String target) {
        if(recentlySeen(cauri)) {
            return;
        }
        PrintWriter diversionLog = getDiversionLog(target);
        cauri.singleLineReportTo(diversionLog);
        diversionLog.println();
    }
    
    /**
     * Consult the cache to determine if the given URI
     * has been recently seen -- entering it if not. 
     * 
     * @param cauri CandidateURI to test
     * @return true if URI was already in the cache; false otherwise 
     */
    private boolean recentlySeen(CandidateURI cauri) {
        long fp = FPGenerator.std64.fp(cauri.toString());
        return ! cache.add(fp);
    }

    /**
     * Get the diversion log for a given target crawler node node. 
     * 
     * @param target crawler node name of requested log
     * @return PrintWriter open on an appropriately-named 
     * log file
     */
    protected PrintWriter getDiversionLog(String target) {
        FilePrintWriter writer = (FilePrintWriter) diversionLogs.get(target);
        if(writer == null) {
            String divertDirPath = (String) getUncheckedAttribute(null,ATTR_DIVERSION_DIR);
            File divertDir = new File(divertDirPath);
            if (!divertDir.isAbsolute()) {
                divertDir = new File(getSettingsHandler().getOrder()
                        .getController().getDisk(), divertDirPath);
            }
            divertDir.mkdirs();
            File divertLog = 
                new File(divertDir,
                         logGeneration+"-"+localName+"-to-"+target+".open");
            try {
                writer = new FilePrintWriter(divertLog);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            diversionLogs.put(target,writer);
        } 
        return writer;
    }

    protected void initialTasks() {
        super.initialTasks();
        localName = (String) getUncheckedAttribute(null, ATTR_LOCAL_NAME);
        cache = new ArrayLongFPCache();
    }
}