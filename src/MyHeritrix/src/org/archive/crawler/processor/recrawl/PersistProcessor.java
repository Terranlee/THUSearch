/* PersistProcessor.java
 * 
 * Created on Feb 17, 2005
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.io.CrawlerJournal;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.OneLineSimpleLogger;
import org.archive.util.SURT;
import org.archive.util.bdbje.EnhancedEnvironment;
import org.archive.util.iterator.LineReadingIterator;

import st.ata.util.AList;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredIterator;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;

/**
 * Superclass for Processors which utilize BDB-JE for URI state
 * (including most notably history) persistence. Includes many static 
 * utility methods (including a main()). 
 * 
 * @author gojomo
 */
public abstract class PersistProcessor extends Processor {
    
    private static final long serialVersionUID = 1L;

    private static final Logger logger =
        Logger.getLogger(PersistProcessor.class.getName());

    /** name of history Database */
    public static final String URI_HISTORY_DBNAME = "uri_history";
    
    /**
     * @return DatabaseConfig for history Database
     */
    protected static DatabaseConfig historyDatabaseConfig() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        dbConfig.setDeferredWrite(true);
        return dbConfig;
    }

    /**
     * Usual constructor
     * 
     * @param name
     * @param string
     */
    public PersistProcessor(String name, String string) {
        super(name,string);
    }

    /**
     * Return a preferred String key for persisting the given CrawlURI's
     * AList state. 
     * 
     * @param curi CrawlURI
     * @return String key
     */
    public String persistKeyFor(CrawlURI curi) {
        // use a case-sensitive SURT for uniqueness and sorting benefits
        return SURT.fromURI(curi.getUURI().toString(),true);
    }

    /**
     * Whether the current CrawlURI's state should be persisted (to log or
     * direct to database)
     * 
     * @param curi CrawlURI
     * @return true if state should be stored; false to skip persistence
     */
    protected boolean shouldStore(CrawlURI curi) {
        // TODO: don't store some codes, such as 304 unchanged?
        return curi.isSuccess();
    }

    /**
     * Whether the current CrawlURI's state should be loaded
     * 
     * @param curi CrawlURI
     * @return true if state should be loaded; false to skip loading
     */
    protected boolean shouldLoad(CrawlURI curi) {
        // TODO: don't load some (prereqs?)
        return true;
    }

    /**
     * Copies entries from an existing environment db to a new one. If
     * historyMap is not provided, only logs the entries that would have been 
     * copied.
     * 
     * @param sourceDir existing environment database directory
     * @param historyMap new environment db (or null for a dry run)
     * @return number of records
     * @throws DatabaseException
     */
    private static int copyPersistEnv(File sourceDir, StoredSortedMap<String,AList> historyMap) 
    throws DatabaseException {
        int count = 0;

        // open the source env history DB, copying entries to target env
        EnhancedEnvironment sourceEnv = setupCopyEnvironment(sourceDir,true);
        StoredClassCatalog sourceClassCatalog = sourceEnv.getClassCatalog();
        Database sourceHistoryDB = sourceEnv.openDatabase(
                null, URI_HISTORY_DBNAME, historyDatabaseConfig());
        StoredSortedMap<String,AList> sourceHistoryMap = new StoredSortedMap<String,AList>(sourceHistoryDB,
                new StringBinding(), new SerialBinding<AList>(sourceClassCatalog,
                        AList.class), true);

        Iterator<Entry<String,AList>> iter = sourceHistoryMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String,AList> item = iter.next(); 
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(item.getKey() + " " + item.getValue().toPrettyString());
            }
            
            if (historyMap != null) {
                historyMap.put(item.getKey(), item.getValue());
            }
            count++;
        }
        StoredIterator.close(iter);
        sourceHistoryDB.close();
        sourceEnv.close();
        
        return count;
    }

    /**
     * Populates an environment db from a persist log. If historyMap is
     * not provided, only logs the entries that would have been populated.
     * 
     * @param persistLogReader
     *            persist log
     * @param historyMap
     *            new environment db (or null for a dry run)
     * @return number of records
     * @throws UnsupportedEncodingException
     * @throws DatabaseException
     */
    private static int populatePersistEnvFromLog(BufferedReader persistLogReader, StoredSortedMap<String,AList> historyMap) 
    throws UnsupportedEncodingException, DatabaseException {
        int count = 0;

        Iterator<String> iter = new LineReadingIterator(persistLogReader);
        while (iter.hasNext()) {
            String line = iter.next(); 
            if (line.length() == 0) {
                continue;
            }
            String[] splits = line.split(" ");
            if (splits.length != 2) {
                logger.severe("bad line: " + line);
                continue;
            }

            AList alist = (AList) IoUtils.deserializeFromByteArray(Base64.decodeBase64(splits[1].getBytes("UTF-8")));

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(splits[0] + " " + alist.toPrettyString());
            }

            if (historyMap != null) try {
                historyMap.put(splits[0], alist);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "caught exception after loading " + count + 
                        " urls from the persist log (perhaps crawl was stopped by user?)", e);
                IOUtils.closeQuietly(persistLogReader);

                // seems to finish most cleanly when we return rather than throw something
                return count;
            }
            
            count++;
        }
        IOUtils.closeQuietly(persistLogReader);
        
        return count;
    }
    
    /**
     * Populates a new environment db from an old environment db or a persist
     * log. If path to new environment is not provided, only logs the entries 
     * that would have been populated.
     * 
     * @param sourcePath
     *            source of old entries: can be a path to an existing
     *            environment db, or a URL or path to a persist log
     * @param envFile
     *            path to new environment db (or null for a dry run)
     * @return number of records
     * @throws DatabaseException
     * @throws IOException
     */
    public static int populatePersistEnv(String sourcePath, File envFile)
        throws DatabaseException, IOException {
        int count = 0;
        StoredSortedMap<String,AList> historyMap = null;
        EnhancedEnvironment targetEnv = null;
        StoredClassCatalog classCatalog = null;
        Database historyDB = null;

        if (envFile != null) {
            // set up target environment
            if (!envFile.exists()) {
                envFile.mkdirs();
            }
            targetEnv = setupCopyEnvironment(envFile);
            classCatalog = targetEnv.getClassCatalog();
            historyDB = targetEnv.openDatabase(null, URI_HISTORY_DBNAME, 
                    historyDatabaseConfig());
            historyMap = new StoredSortedMap<String,AList>(historyDB, 
                    new StringBinding(), new SerialBinding<AList>(classCatalog,
                        AList.class), true);
        }

        try {
            count = copyPersistSourceToHistoryMap(null, sourcePath, historyMap);
        } finally {
            // in finally block so that we unlock the target env even if we
            // failed to populate it
            if (envFile != null) {
                logger.info(count + " records imported from " + sourcePath + " to BDB env " + envFile);
                historyDB.sync();
                historyDB.close();
                targetEnv.close();
            } else {
                logger.info(count + " records found in " + sourcePath);
            }
        }

        return count;
    }

    /**
     * Populates a given StoredSortedMap (history map) from an old 
     * environment db or a persist log. If a map is not provided, only 
     * logs the entries that would have been populated.
     * 
     * @param sourcePath
     *            source of old entries: can be a path to an existing
     *            environment db, or a URL or path to a persist log
     * @param historyMap
     *            map to populate (or null for a dry run)
     * @return number of records
     * @throws DatabaseException
     * @throws IOException
     */
    public static int copyPersistSourceToHistoryMap(File context,
            String sourcePath,
            StoredSortedMap<String, AList> historyMap)
            throws DatabaseException, IOException, MalformedURLException,
            UnsupportedEncodingException {
        int count;
        // delegate depending on the source
        File sourceFile = FileUtils.maybeRelative(context, sourcePath);
        if (sourceFile.isDirectory()) {
            count = copyPersistEnv(sourceFile, historyMap);
        } else {
            BufferedReader persistLogReader = null;
            if (sourceFile.isFile()) {
                persistLogReader = CrawlerJournal.getBufferedReader(sourceFile);
            } else {
                URL sourceUrl = new URL(sourcePath);
                persistLogReader = CrawlerJournal.getBufferedReader(sourceUrl);
            }
            
            count = populatePersistEnvFromLog(persistLogReader, historyMap);
        }
        return count;
    }

    /**
     * Utility main for importing a log into a BDB-JE environment or moving a
     * database between environments (2 arguments), or simply dumping a log
     * to stderr in a more readable format (1 argument). 
     * 
     * @param args command-line arguments
     * @throws DatabaseException
     * @throws IOException
     */
    public static void main(String[] args) throws DatabaseException, IOException {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new OneLineSimpleLogger());
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);

        if (args.length == 2) {
            logger.setLevel(Level.INFO);
            populatePersistEnv(args[0], new File(args[1]));
        } else if (args.length == 1) {
            logger.setLevel(Level.FINE);
            populatePersistEnv(args[0], null);
        } else {
            System.out.println("Arguments: ");
            System.out.println("    source [target]");
            System.out.println(
                "...where source is either a txtser log file or BDB env dir");
            System.out.println(
                "and target, if present, is a BDB env dir. ");
            return;
        }
    }

    public static EnhancedEnvironment setupCopyEnvironment(File env) throws DatabaseException {
        return setupCopyEnvironment(env, false);
    }
    
    public static EnhancedEnvironment setupCopyEnvironment(File env, boolean readOnly) throws DatabaseException {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setReadOnly(readOnly); 
        try {
            return new EnhancedEnvironment(env, envConfig);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("problem with specified environment "+env+"; is it already open?", iae);
        }
    }
}
