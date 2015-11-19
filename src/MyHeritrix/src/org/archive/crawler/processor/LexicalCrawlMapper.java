/* LexicalCrawlMapper
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.RegexpLineIterator;


/**
 * A simple crawl splitter/mapper, dividing up CandidateURIs/CrawlURIs
 * between crawlers by diverting some range of URIs to local log files
 * (which can then be imported to other crawlers). 
 * 
 * May operate on a CrawlURI (typically early in the processing chain) or
 * its CandidateURI outlinks (late in the processing chain, after 
 * LinksScoper), or both (if inserted and configured in both places). 
 * 
 * <p>Uses lexical comparisons of classKeys to map URIs to crawlers. The
 * 'map' is specified via either a local or HTTP-fetchable file. Each
 * line of this file should contain two space-separated tokens, the
 * first a key and the second a crawler node name (which should be
 * legal as part of a filename). All URIs will be mapped to the crawler
 * node name associated with the nearest mapping key equal or subsequent 
 * to the URI's own classKey. If there are no mapping keys equal or 
 * after the classKey, the mapping 'wraps around' to the first mapping key.
 * 
 * <p>One crawler name is distinguished as the 'local name'; URIs mapped to
 * this name are not diverted, but continue to be processed normally.
 * 
 * <p>For example, assume a SurtAuthorityQueueAssignmentPolicy and
 * a simple mapping file:
 * 
 * <pre>
 *  d crawlerA
 *  ~ crawlerB
 * </pre>
 * <p>All URIs with "com," classKeys will find the 'd' key as the nearest
 * subsequent mapping key, and thus be mapped to 'crawlerA'. If that's
 * the 'local name', the URIs will be processed normally; otherwise, the
 * URI will be written to a diversion log aimed for 'crawlerA'. 
 * 
 * <p>If using the JMX importUris operation importing URLs dropped by
 * a {@link LexicalCrawlMapper} instance, use <code>recoveryLog</code> style.
 * 
 * @author gojomo
 * @version $Date: 2006-09-26 20:38:48 +0000 (Tue, 26 Sep 2006) $, $Revision: 4667 $
 */
public class LexicalCrawlMapper extends CrawlMapper {
    private static final long serialVersionUID = 1L;
    
    /** where to load map from */
    public static final String ATTR_MAP_SOURCE = "map-source";
    public static final String DEFAULT_MAP_SOURCE = "";
    
    /**
     * Mapping of classKey ranges (as represented by their start) to 
     * crawlers (by abstract name/filename)
     */
    TreeMap<String, String> map = new TreeMap<String, String>();

    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public LexicalCrawlMapper(String name) {
        super(name, "LexicalCrawlMapper. Maps URIs to a named " +
                "crawler by a lexical comparison of the URI's " +
                "classKey to a supplied ranges map.");
        addElementToDefinition(new SimpleType(ATTR_MAP_SOURCE,
            "Path (or HTTP URL) to map specification file. Each line " +
            "should include 2 whitespace-separated tokens: the first a " +
            "key indicating the end of a range, the second the crawler " +
            "node to which URIs in the key range should be mapped.",
            DEFAULT_MAP_SOURCE));
    }

    /**
     * Look up the crawler node name to which the given CandidateURI 
     * should be mapped. 
     * 
     * @param cauri CandidateURI to consider
     * @return String node name which should handle URI
     */
    protected String map(CandidateURI cauri) {
        // get classKey, via frontier to generate if necessary
        String classKey = getController().getFrontier().getClassKey(cauri);
        SortedMap tail = map.tailMap(classKey);
        if(tail.isEmpty()) {
            // wraparound
            tail = map;
        }
        // target node is value of nearest subsequent key
        return (String) tail.get(tail.firstKey());
    }

    protected void initialTasks() {
        super.initialTasks();
        try {
            loadMap();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve and parse the mapping specification from a local path or
     * HTTP URL. 
     * 
     * @throws IOException
     */
    protected void loadMap() throws IOException {
        map.clear();
        String mapSource = (String) getUncheckedAttribute(null,ATTR_MAP_SOURCE);
        Reader reader = null;
        if(!mapSource.startsWith("http://")) {
            // file-based source
            File source = new File(mapSource);
            if (!source.isAbsolute()) {
                source = new File(getSettingsHandler().getOrder()
                        .getController().getDisk(), mapSource);
            }
            reader = new FileReader(source);
        } else {
            URLConnection conn = (new URL(mapSource)).openConnection();
            reader = new InputStreamReader(conn.getInputStream());
        }
        reader = new BufferedReader(reader);
        Iterator iter = 
            new RegexpLineIterator(
                    new LineReadingIterator((BufferedReader) reader),
                    RegexpLineIterator.COMMENT_LINE,
                    RegexpLineIterator.TRIMMED_ENTRY_TRAILING_COMMENT,
                    RegexpLineIterator.ENTRY);
        while (iter.hasNext()) {
            String[] entry = ((String) iter.next()).split("\\s+");
            map.put(entry[0],entry[1]);
        }
        reader.close();
    }
}