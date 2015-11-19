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
 * CrawlScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.scope.SeedFileIterator;
import org.archive.crawler.scope.SeedListener;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.net.UURI;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;

/**
 * A CrawlScope instance defines which URIs are "in"
 * a particular crawl.
 *
 * It is essentially a Filter which determines, looking at
 * the totality of information available about a
 * CandidateURI/CrawlURI instamce, if that URI should be
 * scheduled for crawling.
 *
 * Dynamic information inherent in the discovery of the
 * URI -- such as the path by which it was discovered --
 * may be considered.
 *
 * Dynamic information which requires the consultation
 * of external and potentially volatile information --
 * such as current robots.txt requests and the history
 * of attempts to crawl the same URI -- should NOT be
 * considered. Those potentially high-latency decisions
 * should be made at another step.
 *
 * @author gojomo
 *
 */
public class CrawlScope extends Filter {

    private static final long serialVersionUID = -3321533224526211277L;

    private static final Logger logger =
        Logger.getLogger(CrawlScope.class.getName());
    public static final String ATTR_NAME = "scope";
    public static final String ATTR_SEEDS = "seedsfile";
    
    /**
     * Whether every configu change should trigger a 
     * rereading of the original seeds spec/file.
     */
    public static final String 
        ATTR_REREAD_SEEDS_ON_CONFIG = "reread-seeds-on-config";
    public static final Boolean
        DEFAULT_REREAD_SEEDS_ON_CONFIG = Boolean.TRUE;
    
    protected Set<SeedListener> seedListeners = new HashSet<SeedListener>();

    /** Constructs a new CrawlScope.
     *
     * @param name the name is ignored since it always have to be the value of
     *        the constant ATT_NAME.
     */
    public CrawlScope(String name) {
        // 'name' is never used.
        super(ATTR_NAME, "Crawl scope");
        Type t;
        t = addElementToDefinition(new SimpleType(ATTR_SEEDS,
                "File from which to extract seeds.", "seeds.txt"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        t = addElementToDefinition(new SimpleType(ATTR_REREAD_SEEDS_ON_CONFIG,
                "Whether to reread the seeds specification, whether it has " +
                "changed or not, every time any configuration change occurs. " +
                "If true, seeds are reread even when (for example) new " +
                "domain overrides are set. Rereading the seeds can take a " +
                "long time with large seed lists.", 
                DEFAULT_REREAD_SEEDS_ON_CONFIG));
        t.setOverrideable(false);
        t.setExpertSetting(true);

    }

    /** Default constructor.
     */
    public CrawlScope() {
        this(ATTR_NAME);
    }

    /**
     * Initialize is called just before the crawler starts to run.
     *
     * The settings system is up and initialized so can be used.  This
     * initialize happens after {@link #earlyInitialize(CrawlerSettings)}.
     *
     * @param controller Controller object.
     */
    public void initialize(CrawlController controller) {
        // by default do nothing (subclasses override)
    }

    public String toString() {
        return "CrawlScope<" + getName() + ">";
    }

    /**
     * Refresh seeds.
     *
     */
    public void refreshSeeds() {
        // by default do nothing (subclasses which cache should override)
    }

    /**
     * @return Seed list file or null if problem getting settings file.
     */
    public File getSeedfile() {
        File file = null;
        try {
            file = getSettingsHandler().getPathRelativeToWorkingDirectory(
                (String)getAttribute(ATTR_SEEDS));
            if (!file.exists() || !file.canRead()) {
                throw new IOException("Seeds file " +
                    file.getAbsolutePath() + " does not exist or unreadable.");
            }
        } catch (IOException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
        } catch (AttributeNotFoundException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
        } catch (MBeanException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
            e.printStackTrace();
        } catch (ReflectionException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
            e.printStackTrace();
        }

        return file;
    }

    /** Check if a URI is in the seeds.
     *
     * @param o the URI to check.
     * @return true if URI is a seed.
     */
    protected boolean isSeed(Object o) {
        return o instanceof CandidateURI && ((CandidateURI) o).isSeed();
    }

    /**
     * @param a First UURI of compare.
     * @param b Second UURI of compare.
     * @return True if UURIs are of same host.
     */
    protected boolean isSameHost(UURI a, UURI b) {
        boolean isSameHost = false;
        if (a != null && b != null) {
            // getHost can come back null.  See
            // "[ 910120 ] java.net.URI#getHost fails when leading digit"
            try {
                if (a.getReferencedHost() != null && b.getReferencedHost() != null) {
                    if (a.getReferencedHost().equals(b.getReferencedHost())) {
                        isSameHost = true;
                    }
                }
            }
            catch (URIException e) {
                logger.severe("Failed compare of " + a + " " + b + ": " +
                    e.getMessage());
            }
        }
        return isSameHost;
    }



    /* (non-Javadoc)
     * @see org.archive.crawler.settings.ModuleType#listUsedFiles(java.util.List)
     */
    public void listUsedFiles(List<String> list){
        // Add seed file
        try {
            File file = getSettingsHandler().getPathRelativeToWorkingDirectory(
                    (String)getAttribute(ATTR_SEEDS));
            list.add(file.getAbsolutePath());
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReflectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Take note of a situation (such as settings edit) where
     * involved reconfiguration (such as reading from external
     * files) may be necessary.
     */
    public void kickUpdate() {
        // TODO: further improve this so that case with hundreds of
        // thousands or millions of seeds works better without requiring
        // this specific settings check 
        if (((Boolean) getUncheckedAttribute(null, ATTR_REREAD_SEEDS_ON_CONFIG))
                .booleanValue()) {
            refreshSeeds();
            getSettingsHandler().getOrder().getController().getFrontier().loadSeeds();
        }
    }

    /**
     * Gets an iterator over all configured seeds. Subclasses
     * which cache seeds in memory can override with more
     * efficient implementation. 
     *
     * @return Iterator, perhaps over a disk file, of seeds
     */
    public Iterator<UURI> seedsIterator() {
        return seedsIterator(null);
    }
    
    /**
     * Gets an iterator over all configured seeds. Subclasses
     * which cache seeds in memory can override with more
     * efficient implementation. 
     *
     * @param ignoredItemWriter optional writer to get ignored seed items report
     * @return Iterator, perhaps over a disk file, of seeds
     */
    public Iterator<UURI> seedsIterator(Writer ignoredItemWriter) {
        BufferedReader br;
        try {
            br = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(getSeedfile()),
                    "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new SeedFileIterator(br,ignoredItemWriter);
    }
    
    /**
     * Convenience method to close SeedFileIterator, if appropriate.
     * 
     * @param iter Iterator to check if SeedFileIterator needing closing
     */
    protected void checkClose(Iterator iter) {
        if(iter instanceof SeedFileIterator) {
            ((SeedFileIterator)iter).close();
        }
    }
    
    /**
     * Add a new seed to scope. By default, simply appends
     * to seeds file, though subclasses may handle differently.
     *
     * <p>This method is *not* sufficient to get the new seed 
     * scheduled in the Frontier for crawling -- it only 
     * affects the Scope's seed record (and decisions which
     * flow from seeds). 
     *
     * @param curi CandidateUri to add
     * @return true if successful, false if add failed for any reason
     */
    public boolean addSeed(final CandidateURI curi) {
        File f = getSeedfile();
        if (f != null) {
            try {
                OutputStreamWriter fw = 
                    new OutputStreamWriter(new FileOutputStream(f, true),"UTF-8");
                // Write to new (last) line the URL.
                fw.write("\n");
                fw.write("# Heritrix added seed ");
                fw.write((curi.getVia() != null) 
                            ? "redirect from " + curi.getVia() 
                            : "(JMX)");
                fw.write(" " + ArchiveUtils.get17DigitDate() + ".\n");
                fw.write(curi.toString());
                fw.flush();
                fw.close();
                Iterator iter = seedListeners.iterator();
                while(iter.hasNext()) {
                    ((SeedListener)iter.next()).addedSeed(curi);
                }
                return true;
            } catch (IOException e) {
                DevUtils.warnHandle(e, "problem writing new seed");
            }
        }
        return false; 
    }
    
    public void addSeedListener(SeedListener sl) {
        seedListeners.add(sl);
    }
}
