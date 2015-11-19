/* SurtPrefixedDecideRule
*
* $Id: SurtPrefixedDecideRule.java 6704 2009-11-25 01:38:55Z gojomo $
*
* Created on Apr 5, 2005
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
package org.archive.crawler.deciderules;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.scope.SeedListener;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.util.SurtPrefixSet;



/**
 * Rule applies configured decision to any URIs that, when 
 * expressed in SURT form, begin with one of the prefixes
 * in the configured set. 
 * 
 * The set can be filled with SURT prefixes implied or
 * listed in the seeds file, or another external file. 
 *
 * The "also-check-via" option to implement "one hop off" 
 * scoping derives from a contribution by Shifra Raffel
 * of the California Digital Library. 
 * 
 * @author gojomo
 */
public class SurtPrefixedDecideRule extends PredicatedDecideRule 
        implements SeedListener {

    private static final long serialVersionUID = 2075790126085405015L;

    //private static final Logger logger =
    //    Logger.getLogger(SurtPrefixedDecideRule.class.getName());
    
    public static final String ATTR_SURTS_SOURCE_FILE = "surts-source-file";
    public static final String ATTR_SEEDS_AS_SURT_PREFIXES =
        "seeds-as-surt-prefixes";
    public static final String ATTR_SURTS_DUMP_FILE = "surts-dump-file";
    
    private static final Boolean DEFAULT_SEEDS_AS_SURT_PREFIXES =
        new Boolean(true);

    /**
     * Whether every config change should trigger a 
     * rebuilding of the prefix set.
     */
    public static final String 
        ATTR_REBUILD_ON_RECONFIG = "rebuild-on-reconfig";
    public static final Boolean
        DEFAULT_REBUILD_ON_RECONFIG = Boolean.TRUE;
    
    /**
     * Whether the 'via' of CrawlURIs should also be checked
     * to see if it is prefixed by the set of SURT prefixes
     */
    public static final String 
        ATTR_ALSO_CHECK_VIA = "also-check-via";
    public static final Boolean
        DEFAULT_ALSO_CHECK_VIA = Boolean.FALSE;
    
    protected SurtPrefixSet surtPrefixes = null;

    /**
     * Usual constructor. 
     * @param name
     */
    public SurtPrefixedDecideRule(String name) {
        super(name);
        setDescription("SurtPrefixedDecideRule. Makes the configured decision "
                + "for any URI which, when expressed in SURT form, begins "
                + "with any of the established prefixes (from either seeds "
                + "specification or an external file).");
        addElementToDefinition(new SimpleType(ATTR_SURTS_SOURCE_FILE,
                "Source file from which to infer SURT prefixes. Any URLs " +
                "in file will be converted to the implied SURT prefix, and " +
                "literal SURT prefixes may be listed on lines beginning " +
                "with a '+' character.",
                ""));
        addElementToDefinition(new SimpleType(ATTR_SEEDS_AS_SURT_PREFIXES,
                "Should seeds also be interpreted as SURT prefixes.",
                DEFAULT_SEEDS_AS_SURT_PREFIXES));
        Type t = addElementToDefinition(new SimpleType(ATTR_SURTS_DUMP_FILE,
                "Dump file to save SURT prefixes actually used: " +
                "Useful debugging SURTs.", ""));
        t.setExpertSetting(true);
        t = addElementToDefinition(new SimpleType(ATTR_ALSO_CHECK_VIA,
                "Whether to also make the configured decision if a " +
                "URI's 'via' URI (the URI from which it was discovered) " +
                "in SURT form begins with any of the established prefixes. " +
                "For example, can be used to ACCEPT URIs that are 'one hop " +
                "off' URIs fitting the SURT prefixes. Default is false.",
                DEFAULT_ALSO_CHECK_VIA));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        t = addElementToDefinition(new SimpleType(ATTR_REBUILD_ON_RECONFIG,
                "Whether to rebuild the internal structures from source " +
                "files (including seeds if appropriate) every time any " +
                "configuration change occurs. If true, " +
                "rule is rebuilt from sources even when (for example) " +
                "unrelated new domain overrides are set. Rereading large" +
                "source files can take a long time.", 
                DEFAULT_REBUILD_ON_RECONFIG));
        t.setOverrideable(false);
        t.setExpertSetting(true);
    }

    /**
     * Evaluate whether given object's URI is covered by the SURT prefix set
     * 
     * @param object Item to evaluate.
     * @return true if item, as SURT form URI, is prefixed by an item in the set
     */
    protected boolean evaluate(Object object) {
        if ( (object instanceof CandidateURI) && 
                ((Boolean) getUncheckedAttribute(null, ATTR_ALSO_CHECK_VIA))
                    .booleanValue()) {
            if(evaluate(((CandidateURI)object).getVia())) {
                return true;
            }
        }
        String candidateSurt;
        candidateSurt = SurtPrefixSet.getCandidateSurt(object);
        if (candidateSurt == null) {
            return false;
        }
        return getPrefixes().containsPrefixOf(candidateSurt);
    }

    /**
     * Synchronized get of prefix set to use
     * 
     * @return SurtPrefixSet to use for check
     */
    private synchronized SurtPrefixSet getPrefixes() {
        if (surtPrefixes == null) {
            readPrefixes();
        }
        return surtPrefixes;
    }

    protected void readPrefixes() {
        buildSurtPrefixSet();
        dumpSurtPrefixSet();
    }
    
    /**
     * Dump the current prefixes in use to configured dump file (if any)
     */
    protected void dumpSurtPrefixSet() {
        // dump surts to file, if appropriate
        String dumpPath = (String)getUncheckedAttribute(null,
            ATTR_SURTS_DUMP_FILE);
        if (dumpPath.length() > 0) {
            File dump = new File(dumpPath);
            if (!dump.isAbsolute()) {
                dump = new File(getSettingsHandler().getOrder().getController()
                    .getDisk(), dumpPath);
            }
            try {
                OutputStreamWriter fw = 
                    new OutputStreamWriter(new FileOutputStream(dump),"UTF-8");
                try {
                    surtPrefixes.exportTo(fw);
                } finally {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Construct the set of prefixes to use, from the seed list (
     * which may include both URIs and '+'-prefixed directives).
     */
    protected void buildSurtPrefixSet() {
        SurtPrefixSet newSurtPrefixes = new SurtPrefixSet();
        FileReader fr = null;

        // read SURTs from file, if appropriate
        String sourcePath = (String)getUncheckedAttribute(null,
                ATTR_SURTS_SOURCE_FILE);
        if (sourcePath.length() > 0) {
            File source = new File(sourcePath);
            if (!source.isAbsolute()) {
                source = new File(getSettingsHandler().getOrder()
                    .getController().getDisk(), sourcePath);
            }
            try {
                fr = new FileReader(source);
                try {
                    newSurtPrefixes.importFromMixed(fr, true);
                } finally {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        
        // interpret seeds as surts, if appropriate
        boolean deduceFromSeeds = ((Boolean)getUncheckedAttribute(null,
                ATTR_SEEDS_AS_SURT_PREFIXES)).booleanValue();
        if(deduceFromSeeds) {
            try {
                fr = new FileReader(getSeedfile());
                try {
                    newSurtPrefixes.importFromMixed(fr, deduceFromSeeds);
                } finally {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        surtPrefixes = newSurtPrefixes;
    }

    /**
     * Re-read prefixes after an update.
     * 
     * @see org.archive.crawler.framework.CrawlScope#kickUpdate()
     */
    public synchronized void kickUpdate() {
        super.kickUpdate();
        if (((Boolean) getUncheckedAttribute(null, ATTR_REBUILD_ON_RECONFIG))
                .booleanValue()) {
            readPrefixes();
        }
        // TODO: make conditional on file having actually changed,
        // perhaps by remembering mod-time
    }

    /**
     * Dig through everything to get the crawl-global seeds file. 
     * Add self as listener while at it. 
     * 
     * @return Seed list file
     */
    protected File getSeedfile() {
        CrawlScope scope =
            getSettingsHandler().getOrder().getController().getScope();
        scope.addSeedListener(this);
        return scope.getSeedfile();
    }

    public synchronized void addedSeed(final CandidateURI curi) {
        SurtPrefixSet newSurtPrefixes = (SurtPrefixSet) surtPrefixes.clone();
        newSurtPrefixes.add(prefixFrom(curi.toString()));
        surtPrefixes = newSurtPrefixes;
    }
    
    protected String prefixFrom(String uri) {
    	return SurtPrefixSet.prefixFromPlain(uri);
    }
}
