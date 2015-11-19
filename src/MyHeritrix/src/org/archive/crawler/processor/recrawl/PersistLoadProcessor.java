/* PersistLoadProcessor.java
 * 
 * Created on Feb 13, 2005
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

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

import st.ata.util.AList;

import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.DatabaseException;

/**
 * Store CrawlURI attributes from latest fetch to persistent storage for
 * consultation by a later recrawl. 
 * 
 * @author gojomo
 * @version $Date: 2006-09-25 20:19:54 +0000 (Mon, 25 Sep 2006) $, $Revision: 4654 $
 */
public class PersistLoadProcessor extends PersistOnlineProcessor {
    private static final long serialVersionUID = -1917169316015093131L;
    private static final Logger logger =
        Logger.getLogger(PersistLoadProcessor.class.getName());
    
    /** file (log) or directory (state/env) from which to preload history **/
    public static final String ATTR_PRELOAD_SOURCE = "preload-source";

    /**
     * Usual constructor
     * 
     * @param name
     */
    public PersistLoadProcessor(String name) {
        super(name, "PersistLoadProcessor. Loads CrawlURI attributes " +
                "from a previous crawl for current consultation.");
        Type e;
        e = addElementToDefinition(new SimpleType(ATTR_PRELOAD_SOURCE,
                "Source for preloaded persist information. This can be " +
                "a URL or path to a persist log, or a path to an old " +
                "state directory.", ""));
        e.setOverrideable(false);
        e.setExpertSetting(false);
    }
    
    @Override
    protected StoredSortedMap<String,AList> initStore() {
        StoredSortedMap<String,AList> historyMap = super.initStore();
        
        // Preload, if a 'preload-source' file-path/URI/dir-path specified
        String preloadSource = 
            (String) getUncheckedAttribute(null, ATTR_PRELOAD_SOURCE);
        if (StringUtils.isNotBlank(preloadSource)) {
            try {
                int count = PersistProcessor.copyPersistSourceToHistoryMap(
                        getController().getDisk(), preloadSource, historyMap);
                logger.info("Loaded deduplication information for " + count + " previously fetched urls from " + preloadSource);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Problem loading " + preloadSource + ", proceeding without deduplication! " + ioe);
            } catch(DatabaseException de) {
                logger.log(Level.WARNING, "Problem loading " + preloadSource + ", proceeding without deduplication! " + de);
            }
        }
        return historyMap;
    }

    @Override
    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if(shouldLoad(curi)) {
            AList prior = (AList) store.get(persistKeyFor(curi));
            if(prior!=null) {
                // merge in keys
                Iterator iter = prior.getKeys();
                curi.getAList().copyKeysFrom(iter, prior, false);
            }
        }
    }
}