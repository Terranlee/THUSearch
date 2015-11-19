/* CachedBdbMap
 * 
 * $Id: CachedBdbMap.java 6816 2010-04-12 21:49:16Z gojomo $
 * 
 * Created on Mar 24, 2004
 *
 * Copyright (C) 2009 Internet Archive.
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
package org.archive.util;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.framework.CrawlController;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DeadlockException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * A BDB JE backed hashmap. It extends the normal BDB JE map implementation by
 * holding a cache of soft referenced objects. That is objects are not written
 * to disk until they are not referenced by any other object and therefore can be
 * Garbage Collected.
 * <p/>
 * BDB Java Edition is actually a btree.  Flush to disk can be forced by sync().
 * <p/>
 * To ensure that changes/mutations to values in this map are coherent and
 * consistent at the application level, it is assumed that the application
 * level only mutates values that are in this map and does not retain references
 * to values longer than necessary.  This allows mappings to be persisted
 * during GC without explicit transactions or write operations.
 * <p/>
 * There are two styles of CachedBdbMap usage:
 * <p/>
 * 1. single threaded (or externally synchronized) activity that uses any 
 * Map and ConcurrentMap methods available and specifically requires remove().
 * <p/>
 * 2. concurrent, high volume, accretive-only activity that uses
 * {@link #putIfAbsent}, but not the {@link #put},  the {@link #replace}
 * or 1 arg {@link #remove} methods.
 * The concurrent {@link #replace} methods can be used if application level logic
 * can rule out surprise unmapping of values between threads.
 * This usage style does not require locking memMap and diskMap together
 * to guarantee cache coherence and consistency.
 * <p/>
 * Both styles rely on an internal expunge operation (or the explicit
 * {@link #sync()}) to save changes to values.
 * The single threaded case can also use {@link #put} on top of an
 * existing entry as long as no thread retains the previous value instance.
 * 
 * @author John Erik Halse
 * @author stack
 * @author gojomo
 * @author paul baclace (conversion to ConcurrentMap)
 *  
 *  @deprecated use ObjectIdentityBdbCache instead
 */
public class CachedBdbMap<K,V> extends AbstractMap<K,V> 
implements ConcurrentMap<K,V>, ObjectIdentityCache<K,V>, Serializable, Closeable {
    private static final long serialVersionUID = -8655539411367047332L;

    private static final Logger logger =
        Logger.getLogger(CachedBdbMap.class.getName());

    /** The database name of the class definition catalog.*/
    private static final String CLASS_CATALOG = "java_class_catalog";

    /** Number of attempts to tolerate for put().
     * In BDB, simple write lock contention (non-deadlock) arrives as
     * DeadlockException even though we are not using explicit transactions.
     * For details see discussion at
     * http://forums.oracle.com/forums/thread.jspa?threadID=521898&tstart=-1
     * Value here should be a function of
     * setLockTimeout() in {@link CrawlController#setupBdb()}
     * and CrawlController.getToeCount(); since CrawlController is not
     * accessible from this util class, a value suitable for average
     * disk performance and 100 TOE threads is use.
     * (In practice, {@link #put} contention of statistics is where
     * timeouts were found.)
     */
    private static final int BDB_LOCK_ATTEMPT_TOLERANCE = 24;

    /**
     * A map of BDB JE Environments so that we reuse the Environment for
     * databases in the same directory.
     */
    private static final Map<String,DbEnvironmentEntry> dbEnvironmentMap = 
        new HashMap<String,DbEnvironmentEntry>();

    /** The BDB JE environment used for this instance.
     */
    private transient DbEnvironmentEntry dbEnvironment;

    /** The BDB JE database used for this instance. */
    protected transient Database db;

    /** The Collection view of the BDB JE database used for this instance. */
    protected transient StoredSortedMap diskMap;

    /** The softreferenced cache of diskMap.
     * Policy is: a memMap value is always correct *if present*. 
     * The diskMap value is only correct if there is no memMap value 
     * (and any phantom memMap values have been persisted.)
     *   That is, if memMap has a value, then that value is the most 
     * up to date.  If memMap does not hold a value, then diskMap has the 
     * most up to date value.  
     * <p/>
     * For diskMap size monitoring and concurrency support, the following 
     * invariant about keys is maintained: If a key is present in memMap,
     * it must also present in diskMap (although the value in diskMap may
     * not be the most recent, as above policy describes). 
     * <p/>
     * The key presence invariant and the value coherence policy are maintained 
     * by using the "happens after" sense in the Java memory model by
     * transitivity between diskMap and memMap.
     * <p/>
     * The clients of this class "only see one value instance" at a time so 
     * that multiple, independent get(k) calls return the same object instance.
     * <p/>
     * Strategy Notes about using ConcurrentMap semantics to 
     * implement the desired policy (see method comments for details):
     * <p/>
     * <pre>
     * 1. Swap in/First insert: diskMap, 
     *     then memMap // putIfAbsent() assures atomicity.
     * 2. Value mutation: only one value instance per key in memMap 
     *     is maintained so identity compare of value works and all clients 
     *     operate on the same object.
     *     This implies that methods which change the instance 
     *     (replace() methods) are not compatible with this design and
     *     should only be used if instance of CachedBdbMap is used 
     *     by a single thread.
     *     Because content of referent is mutated, mapping is not changed;
     *    2.1. BDB JE operation assumption: calling get(k) twice returns
     *     values that are equals(), but not necessarily == by identity
     *     ( diskMap.get(k).equals(diskMap.get(k)) is true, but 
     *      diskMap.get(k) != diskMap.get(k) (might be, but not guaranteed)).
     * 3. Swap out/flush: diskMap update, then memMap remove: 
     *     diskMap.put(k,v2); // exclusive only if performed when 
     *     processing ref queue during expunge;
     *     Avoid expunge races with a countdown latch in SoftEntry;
     *     memMap.remove(k,v2); if memMap race lost, then no harm done
     *     (notice that (k,v2) could match in either diskMap or memMap,
     *     or both, or neither).
     *    3.1. Only expunge does an update of an existing diskMap entry for 
     *      concurrent methods.
     *    3.2. CachedBdbMap.get() must be able to return referent or otherwise
     *      effect a swap-in if get() is invoked during a swap out of a
     *      particular key-value entry.
     * 4. sync() to disk: synchronized sync() // fully locked, no races.
     * </pre>
     * <p/>
     * See the "find or create" style cache 
     * {@link ServerCache#getServerFor(CandidateURI)} for the major 
     * high-concurrency client of this class in terms of number of objects 
     * stored and performance impact.
     */
    protected transient ConcurrentHashMap<K,SoftEntry<V>> memMap;

    protected transient ReferenceQueue<V> refQueue;

    /** The number of objects in the diskMap StoredMap. 
     *  (Package access for unit testing.) */
    protected AtomicInteger diskMapSize = new AtomicInteger(0);

    // The following atomic counters are used for reporting 
    //  code branches that can occur.
    // Some of these are frequent while others  are rare or depend on
    //  application level and GC behavior.
    // If problems occur, these provide diagnostics such as whether usage went
    //  outside the bounds of the above described expected "two styles".
    
    /** count of expunge already done */
    transient private AtomicInteger expungeStatsNullPhantom;

    /** count of {@link #putIfAbsent} / {@link #remove} races,
     * if they occur */
    transient private AtomicInteger expungeStatsTransientCond;
    
    /** count of {@link #putIfAbsent} / {@link #remove} races,
     * if they occur */
    transient private AtomicInteger expungeStatsTransientCond2;
    
    /** count of {@link #putIfAbsent} / {@link #remove} races,
     * if they occur */
    transient private AtomicInteger expungeStatsTransientCond3;
    
    /** count of {@link SoftEntry#awaitExpunge()) */
    transient private AtomicInteger expungeStatsAwaitExpunge;

    /** count of expunge already done to see if they occur */
    transient private AtomicInteger expungeStatsNullValue;
    
    /** count of expunge of entries not in memMap to see if they occur */
    transient private AtomicInteger expungeStatsNotInMap;
    
    /** static count {@link SoftEntry#awaitExpunge()) timeouts to see if
     * they occur */
    final static private AtomicInteger expungeStatsAwaitTimeout 
            = new AtomicInteger(0);
    
    /** static count of swap-in timeouts */
    final static private AtomicInteger expungeStatsSwapIn 
            = new AtomicInteger(0);
    
    /** static count of swap-in re-tries timeouts to see if they occur */
    final static private AtomicInteger expungeStatsSwapInRetry 
            = new AtomicInteger(0);

    /** static count of swap-in re-tries timeouts to see if they occur */
    final static private AtomicInteger expungeStatsSwapInRetry2 
            = new AtomicInteger(0);
    
    /** static count of expunge put() to BDB (implies disk) */
    final static private AtomicInteger expungeStatsDiskPut 
            = new AtomicInteger(0);

    /** transient count of expunge  get mem check condition */
    transient private AtomicInteger expungeStatsGetMemCheck;

    /** transient count of expunge  via poll */
    transient private AtomicInteger expungeStatsViaPoll;
    
    /** count of one arg {@link #remove } use */
    transient private AtomicInteger useStatsRemove1Used;
    
    /** count of two arg {@link #remove} use */
    transient private AtomicInteger useStatsRemove2Used;
    
    /** count of {@link #replace} (2 or 3 arg) use */
    transient private AtomicInteger useStatsReplaceUsed;
    
    /** count of {@link #put} use */
    transient private AtomicInteger useStatsPutUsed;
    
    /** count of {@link #putIfAbsent} use */
    transient private AtomicInteger useStatsPutIfUsed;
    
    /** count of {@link #sync()} use */
    transient private AtomicInteger useStatsSyncUsed;
    
    /**
     * Count of times we got an object from in-memory cache.
     */
    private AtomicLong cacheHit = new AtomicLong(0);

    /**
     * Count of times the {@link CachedBdbMap#get} method was called.
     */
    private AtomicLong countOfGets = new AtomicLong(0);

    /**
     * Count of every time we went to the disk-based map AND we found an
     * object (Doesn't include accesses that came back null).
     */
    private AtomicLong diskHit = new AtomicLong(0);
    
    /**
     * Name of bdbje db.
     */
    private String dbName = null;

    /**
     * Reference to the Reference#referent Field.
     */
    protected static Field referentField;
    static {
        // We need access to the referent field in the PhantomReference.
        // For more on this trick, see
        //
        // http://www.javaspecialists.co.za/archive/Issue098.html and for
        // discussion:
        // http://www.theserverside.com/tss?service=direct/0/NewsThread/threadViewer.markNoisy.link&sp=l29865&sp=l146901
        try {
            referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /** internal behavior characterization flag.
     * log a warning when operations are performed that could violate the
     *   "only one reference" design rule.  The problem to avoid is
     *   mutating an unmapped value instance that would not be persisted.
     *  (warning is emitted at most once per instance)
     */
    final private boolean LOG_ERROR_ON_DESIGN_VIOLATING_METHODS=true;

    /**
     * Simple structure to keep needed information about a DB Environment.
     */
    protected static class DbEnvironmentEntry {
        Environment environment;
        StoredClassCatalog classCatalog;
        int openDbCount = 0;
        File dbDir;
    }
    
    /**
     * Shutdown default constructor.
     */
    private CachedBdbMap() {
        super();
    }
    
    /**
     * Constructor.
     * 
     * You must call
     * {@link #initialize(Environment, Class, Class, StoredClassCatalog)}
     * to finish construction. Construction is two-stepped to support
     * reconnecting a deserialized CachedBdbMap with its backing bdbje
     * database.
     * 
     * @param dbName Name of the backing db this instance should use.
     */
    public CachedBdbMap(final String dbName) {
        this();
        this.dbName = dbName;
    }

    /**
     * A constructor for creating a new CachedBdbMap.
     * 
     * Even though the put and get methods conforms to the Collections interface
     * taking any object as key or value, you have to submit the class of the
     * allowed key and value objects here and will get an exception if you try
     * to put anything else in the map.
     * 
     * <p>This constructor internally calls
     * {@link #initialize(Environment, Class, Class, StoredClassCatalog)}.
     * Do not call initialize if you use this constructor.
     * 
     * @param dbDir The directory where the database will be created.
     * @param dbName The name of the database to back this map by.
     * @param keyClass The class of the objects allowed as keys.
     * @param valueClass The class of the objects allowed as values.
     * 
     * @throws DatabaseException is thrown if the underlying BDB JE database
     *             throws an exception.
     */
    public CachedBdbMap(final File dbDir, final String dbName,
            final Class<K> keyClass, final Class<V> valueClass)
    throws DatabaseException {
        this(dbName);
        this.dbEnvironment = getDbEnvironment(dbDir);
        this.dbEnvironment.openDbCount++;
        initialize(dbEnvironment.environment, valueClass,
            dbEnvironment.classCatalog);
        if (logger.isLoggable(Level.INFO)) {
            // Write out the bdb configuration.
            EnvironmentConfig cfg = this.dbEnvironment.environment.getConfig();
            logger.info("BdbConfiguration: Cache percentage "  +
                cfg.getCachePercent() + ", cache size " + cfg.getCacheSize() +
                ", Map size: " + size() + " cfg=" + cfg);
        }
    }
    
    /**
     * Call this method when you have an instance when you used the
     * default constructor or when you have a deserialized instance that you
     * want to reconnect with an extant bdbje environment.  Do not
     * call this method if you used the
     * {@link #CachedBdbMap(File, String, Class, Class)} constructor.
     * @param env
     * @param keyClass
     * @param valueClass
     * @param classCatalog
     * @throws DatabaseException
     */
    public synchronized void initialize(final Environment env, 
            final Class<? super V> valueClass, final StoredClassCatalog classCatalog)
    throws DatabaseException {
        initializeInstance();
        this.db = openDatabase(env, this.dbName);
        this.diskMap = createDiskMap(this.db, classCatalog, String.class,
            valueClass);
    }
    
    /**
     * Do any instance setup.
     * This method is used by constructors and when deserializing an instance.
     */
    protected void initializeInstance() {
        // ToDo: initial capacity should be related to number of seeds, max depth, max docs
        this.memMap = new ConcurrentHashMap<K,SoftEntry<V>>(
                                                            8192, // initial capacity
                                                            0.9f, // acceptable load factor
                                                            64 // est. number of concurrent threads
                                                            ); 
        this.refQueue = new ReferenceQueue<V>();
        initTransientStats(); 
        canary = new SoftReference<LowMemoryCanary>(new LowMemoryCanary());
    }
    

    protected void initTransientStats() {
        expungeStatsNullPhantom = new AtomicInteger(0);
        expungeStatsTransientCond = new AtomicInteger(0);
        expungeStatsTransientCond2 = new AtomicInteger(0);
        expungeStatsTransientCond3 = new AtomicInteger(0);
        expungeStatsGetMemCheck = new AtomicInteger(0);
        expungeStatsViaPoll = new AtomicInteger(0);
        expungeStatsAwaitExpunge = new AtomicInteger(0);
        expungeStatsNullValue = new AtomicInteger(0);
        expungeStatsNotInMap = new AtomicInteger(0);
        useStatsRemove1Used = new AtomicInteger(0);    
        useStatsRemove2Used = new AtomicInteger(0);
        useStatsReplaceUsed = new AtomicInteger(0);
        useStatsPutUsed = new AtomicInteger(0);
        useStatsPutIfUsed = new AtomicInteger(0);
        useStatsSyncUsed  = new AtomicInteger(0);
    }

    @SuppressWarnings("unchecked")
    protected StoredSortedMap createDiskMap(Database database,
            StoredClassCatalog classCatalog, Class keyClass, Class valueClass) {
        EntryBinding keyBinding = TupleBinding.getPrimitiveBinding(keyClass);
        if(keyBinding == null) {
            keyBinding = new SerialBinding(classCatalog, keyClass);
        }
        EntryBinding valueBinding = TupleBinding.getPrimitiveBinding(valueClass);
        if(valueBinding == null) {
            valueBinding = new SerialBinding(classCatalog, valueClass);
        }
        // BDB JE is btree only, so Sorted Map is no additional work
        return new StoredSortedMap(database, keyBinding, valueBinding, true);
    }

    /**
     * Conditionally await for an entry to be expunged.
     * If the given entry is in the process of being expunged, this
     * will block until it is complete and then return true.
     * If the entry should be expunged, this will try to do the expunge. 
     *  If the entry is not being expunged,
     * then this will return immediately with a value of false.
     * If this thread is interrupted, the condition is cleared and 
     * the method returns immediately.
     * <p/>
     * For transitive "happens before" correctness invariant to be true,
     * these operations must occur in the following order in one thread:
     *  <pre>
     *  1. if (entry.get() == null) // cleared by GC
     *  2. if (entry.startExpunge()) // thread has exclusive expunge
     *  3. diskMap.put(k, entry.getPhantom().doctoredGet())
     *  4. memMap.remove(k, entry)
     *  5. entry.clearPhantom()
     * 
     *  Concurrent threads interested in the same entry will execute:
     *  
     *  1. if (entry.get() == null) // cleared by GC
     *  2. if (! entry.startExpunge()) // thread cannot do expunge
     *  3. if (entry.expungeStarted()) 
     *  4. entry.awaitExpunge();
     * </pre>
     * @param entry
     * @return true if entry was expunged, cleared, and no longer in memMap.
     */
    private boolean isExpunged(final SoftEntry<V> entry, K key) {
        boolean ret = false;
        
        V val = entry.get();
        if (val == null) { 
            if (entry.startExpunge()) { // try do the expunge now, if needed
                // got exclusive expunge access
                expungeStaleEntry(entry, true);
                return true;
            } else if (entry.expungeStarted()) {  // might need to wait for expunge
                expungeStatsAwaitExpunge.incrementAndGet();
                expungeStaleEntries();
                // preserve interrupted status
                boolean wasInterrupted = Thread.interrupted();
                try {
                    ret = entry.awaitExpunge();
                    if(ret) {
                        verifyExpunge(entry, key); 
                    }
                    return ret;
                } catch (InterruptedException ex) { // timeout (stats already incr.)
                    return true; // be lively 
                } finally {
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                logger.log(
                    Level.WARNING,
                    "could not start expunge, and expunge was never started and val is null",
                    new Exception());
                return false;
            }
        } else { // entry not collected, not expunged
            return false;
        }
    }
    
    /**
     * Verify that expunction has taken place; for debugging. Only 
     * called after awaitExpunge() reports an entry has been expunged.
     * Detects whether memMap.get() returns the same entry by identity;
     * (entries are not recycled.)
     * @param entry entry that should be gone from memMap
     */
    private void verifyExpunge(SoftEntry<V> entry, K key) {
        if(memMap.get(key)==entry) {
            logger.log(Level.WARNING,"reported-expunged entry at key '"+key+"' still present: "+entry,new Exception()); 
            dumpExtraStats();
            // try fixup
            // FIXME: this is just papering over/recovering from a state
            // which should not be reached; remove this code when true bug
            // discovered
            memMapRemoveOrWarn(key, entry);
        }
    }
    
    /**
     * Remove from memMap -- but warn if remove does not have 
     * expected effect. For debugging purposes. 
     * 
     * @param key key to remvoe
     * @param entry expected item to remove
     */
    private void memMapRemoveOrWarn(Object key, SoftEntry<V> entry) {
        boolean removed = memMap.remove(key, entry);
        if(!removed) {
            logger.log(Level.WARNING,"memMap.remove() ineffective",new Exception());
        } else {
            // at this point: memMap no longer contains the given entry
        }
    }

    /**
     * Get the database environment for a physical directory where data will be
     * stored.
     * <p>
     * If the environment already exist it will be reused, else a new one will
     * be created.
     * 
     * @param dbDir The directory where BDB JE data will be stored.
     * @return a datastructure containing the environment and a default database
     *         for storing class definitions.
     */
    private DbEnvironmentEntry getDbEnvironment(File dbDir) {
        if (dbEnvironmentMap.containsKey(dbDir.getAbsolutePath())) {
            return (DbEnvironmentEntry) dbEnvironmentMap.get(dbDir
                    .getAbsolutePath());
        }
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        
//        // We're doing the caching ourselves so setting these at the lowest
//        // possible level.
//        envConfig.setCachePercent(1);
        DbEnvironmentEntry env = new DbEnvironmentEntry();
        try {
            env.environment = new Environment(dbDir, envConfig);
            env.dbDir = dbDir;
            dbEnvironmentMap.put(dbDir.getAbsolutePath(), env);
            
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false);
            dbConfig.setAllowCreate(true);
            dbConfig.setDeferredWrite(true);
            
            Database catalogDb = env.environment.openDatabase(null,
                    CLASS_CATALOG, dbConfig);
            
            env.classCatalog = new StoredClassCatalog(catalogDb);
        } catch (DatabaseException e) {
            e.printStackTrace();
            //throw new FatalConfigurationException(e.getMessage());
        }
        return env;
    }

    protected Database openDatabase(final Environment environment,
            final String dbName) throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        dbConfig.setDeferredWrite(true);
        return environment.openDatabase(null, dbName, dbConfig);
    }

    public synchronized void close() {
        // Close out my bdb db.
        if (this.db != null) {
            try {
                this.db.sync();
                this.db.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            } finally {
                this.db = null;
            }
        }
        if (dbEnvironment != null) {
            dbEnvironment.openDbCount--;
            if (dbEnvironment.openDbCount <= 0) {
                try {
                    dbEnvironment.classCatalog.close();
                    dbEnvironment.environment.close();
                } catch (DatabaseException de) {
                    de.printStackTrace();
                }
                dbEnvironmentMap.remove(dbEnvironment.dbDir.getAbsolutePath());
                dbEnvironment = null;
            }
        }
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * The keySet of the diskMap is all relevant keys. 
     * 
     * @see java.util.Map#keySet()
     */
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        return diskMap.keySet();
    }
    
    public Set<Map.Entry<K,V>> entrySet() {
        // Would require complicated implementation to 
        // maintain identity guarantees, so skipping
        throw new UnsupportedOperationException();
    }
    
    /**
     * ObjectIdentityCache get-or-atomic-create method.
     */
    public V getOrUse(K key, Supplier<V> supplierOrNull) {
        V val = get(key); 
        if(val!=null || supplierOrNull == null) {
            return val; 
        }
        val = supplierOrNull.get();
        V prevVal = putIfAbsent(key, val);
        if(prevVal!=null) {
            return prevVal;
        }
        return val; 
    }

    public V get(final Object object) {
        K key = toKey(object);
        countOfGets.incrementAndGet();
        expungeStaleEntries();
        if (countOfGets.get() % 10000 == 0) {
            dumpExtraStats();
            logCacheSummary();
        }

        // check mem cache
        V val = _getMem(key);
        if (val != null) { // cache hit
            return val;
        }
        
        // check backing diskMap
        V valDisk = _getDisk(key); // disk io can occur here
        return valDisk;
    }
    
    private V _getDisk(K key) {
        V v = diskMapGet(key); // disk io can occur here
        if (v != null) {
            diskHit.incrementAndGet();
            //
            //     cache in memMap (swap in)
            //
            expungeStatsSwapIn.incrementAndGet();
            SoftEntry<V> newEntry = new SoftEntry<V>(key, v, refQueue);
            do {
                SoftEntry<V> existingEntry =
                        (SoftEntry<V>) memMap.putIfAbsent(key, newEntry); // unchecked cast
                if (existingEntry != null) {
                    V prevValue = (V) existingEntry.get();
                    if (prevValue != null) {
                        // another thread beat this thread to memMap insert for swap-in
                        newEntry.clearPhantom(); // instance not needed
                        return prevValue;
                    } else { // SoftEntry referent has been collected
                        // another thread beat this thread in memMap insert for swap-in
                        //   AND existingEntry was cleared before we could see it.
                        // This condition requires 2 competing threads working on
                        //   the same entry, and a full GC to be interleaved.
                        // This is like a simultaneous collision between 
                        //   2 cars and an airplane.  
                        // Wait for expunge to finish
                        
                        if (isExpunged(existingEntry, key)) {
                            // existingEntry is no longer in memMap
                            expungeStatsSwapInRetry.incrementAndGet();
                            continue;  // re-try insert into memMap
                        } else {
                            expungeStatsSwapInRetry2.incrementAndGet();
                            try {
                                // stuck memMap entry
                                Thread.sleep(0L, 10000);
                            } catch (InterruptedException ignored) {
                            }
                            logger.log(Level.WARNING,"Swap-In Retry", new Exception());
                            continue;  // re-try insert into memMap
                        }
                    }
                }
                return v;
            } while (true);
        }
        return null;
    }

    private V _getMem(K key) {
        do {
            SoftEntry<V> entry = memMap.get(key);
            if (entry != null) {
                V val = entry.get(); // get & hold, so not cleared pre-return
                if (val != null) {
                    cacheHit.incrementAndGet();
                    return val;
                }
                // null SoftEntry.get() means entry is being GC-ed
                //  need to wait for expunge to finish and then retry
                //  which will lead to a diskMap.get()
                if (isExpunged(entry, key)) {
                    // entry has been removed, lookup again
                    continue; // tail recursion optimization
                } else {
                    expungeStatsGetMemCheck.incrementAndGet();
                    logger.log(Level.WARNING,"entry not expunged AND value is null", new Exception());
                    return null;
                }
            }
            return null;
        } while (true);
    }

    /**
     * Info to log, if at FINE level
     */
    private void logCacheSummary() {
        if (logger.isLoggable((Level.FINE))) {
            logger.fine(composeCacheSummary());
        }
    }
    
    private String composeCacheSummary() {
        long totalHits = cacheHit.get() + diskHit.get();
        if (totalHits < 1) {
            return "";
        }
        long cacheHitPercent 
                = (cacheHit.get() * 100) / totalHits;
        StringBuilder sb = new StringBuilder(120);
        try {
            String sname = this.db.getDatabaseName();
            sb.append("DB name:").append(sname).append(", ");
        } catch (DatabaseException ignore) {
        }
        sb.append(" Cache Hit: ")
          .append(cacheHitPercent)
          .append("%, Not in map: ")
          .append((countOfGets.get() - totalHits))
          .append(", Total number of gets: ")
          .append(countOfGets.get());
        return sb.toString();
    }
    
    /** Map.put() implementation. 
     * <p/>
     * Warning:  This method violates the "only expose one value instance" 
     *   design rule for this class.  Multiple instances means that it is
     *   undetermined as to which instance will be saved to disk last.
     * This method can be safely used when only one thread has access to 
     * this instance of CachedBdbMap.
     * <p/>If possible, use {@link #putIfAbsent} instead.
     * 
     * <pre>
     * Preconditions: (diskMap.containsKey(), memMap.containsKey()) are:
     * (*,*) put() cannot assure "only expose one value instance", so
     *     all cases are the same: put to diskMap then memMap; clear any 
     *     pre-existing SoftEntry to prevent expunge of old value.
     * 
     * PostConditions:
     * (T,T) both memMap and diskMap will have entries for given key; if 
     *     null is returned, then this is the first time the key has an
     *     entry, otherwise the previous value in diskMap will not be 
     *     updated until expunge. 
     * </pre>
     * 
     * @param key
     * @param value
     * @return previous value or null.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value) {
        V prevMemValue = null;
        V prevDiskValue = null;
        int pu = useStatsPutUsed.incrementAndGet();
        if (pu == 1 && LOG_ERROR_ON_DESIGN_VIOLATING_METHODS) {
            logger.log(Level.WARNING,"design violating put() used on dbName=" + dbName, new Exception());
        }
        expungeStaleEntries();
        int attemptTolerance = BDB_LOCK_ATTEMPT_TOLERANCE;
        while (--attemptTolerance > 0) {
            try {
                prevDiskValue = (V)diskMap.put(key,value);// unchecked cast, disk io
                break;
            } catch (Exception e) {
                if (e instanceof com.sleepycat.util.RuntimeExceptionWrapper
                        && e.getCause() instanceof DeadlockException
                        && attemptTolerance > 0) {
                    if (attemptTolerance == BDB_LOCK_ATTEMPT_TOLERANCE - 1) {
                        // emit once on first retry
                        logger.log(Level.WARNING,"BDB implicit transaction timeout while "
                                + "waiting to acquire lock, retrying; "
                                + " dbName=" + dbName + " key=" + key, new Exception());
                    }
                    continue;
                } else {
                    logger.warning("unexpected exception: " + e);
                    throw new RuntimeException(e);
                }
            }
        }
        if (prevDiskValue == null) {
             diskMapSize.incrementAndGet();
        }
        SoftEntry<V> prevEntry = memMap.put(key, new SoftEntry<V>(key, value, refQueue));
        if (prevEntry != null) {
            prevMemValue = prevEntry.get();
            // prevent previous SoftEntry from being expunged
            prevEntry.clearPhantom();
        }
         
        if (prevMemValue != null) {
            return prevMemValue;
        }
        return prevDiskValue;
    }

    /** Replace entry for key only if currently mapped to given value.
     * To maintain the illusion of an invisible cache,
     * if the memMap has no mapping, the diskMap entry, if any, must
     * be swapped-in to memMap in order to see if the given oldValue
     * matches. 
     * 
     * <p/>Possible disk io to swap in from diskMap.
     * <p/>
     * Warning:  This method violates the "only expose one value instance" 
     *   design rule for this class.  Multiple instances means that it is
     *   undetermined as to which instance will be saved to disk last.
     * This method can be safely used when only one thread has access to 
     * this instance of CachedBdbMap.
     * <pre>
     * Preconditions: (diskMap.containsKey(), memMap.containsKey()) are:
     * (F,F) nothing to replace. 
     *    PostCondition: (F,F) no replace.
     * (T,F) value must be swapped-in, do replace(), 
     *    PostCondition: (T,T) if
     *    replace occurred or not (value might have been replaced), diskMap
     *    need not be updated because expunge will do that.
     * (*,T) normal swapped-in condition (we can assume that the method which
     *    put a mapping to memMap took care of creating one for diskMap), 
     *    memMap value is most recent and expunge can take care of 
     *    updating diskMap, so we only do replace() on memMap; 
     *    PostCondition: (*,T).
     * </pre>
     * @return true if the replace was peformed (only if the previous mapping
     *   of key was to given oldValue).
     */
    @SuppressWarnings("unchecked")
    public boolean replace(K key, V oldValue, V newValue) {
        int ru = useStatsReplaceUsed.incrementAndGet();
        if (ru == 1 && LOG_ERROR_ON_DESIGN_VIOLATING_METHODS) {
            // warn on non-accretive use
            logger.log(Level.WARNING,"design violating replace(,,) used on dbName=" + dbName,new Exception());
        }
        
        expungeStaleEntries();
        
        //  make the ref wrappers
        SoftEntry<V> newEntry = new SoftEntry<V>(key, newValue, refQueue);
        SoftEntry<V> oldEntry = new SoftEntry<V>(key, oldValue, refQueue);

        try {
            boolean memReplaced = memMap.replace(key, oldEntry, newEntry);
            if (memReplaced) { // case (*,T)
                newEntry = null; // skip neutralize in finally
                return true;
            } else { // case (?,F), does diskMap have a mapping?
                boolean diskReplaced = diskMap.replace(key, oldValue, newValue);
                if (diskReplaced) { // case (T,F)
                    // swap-in 
                    memMap.put(key, newEntry);
                    newEntry = null; // skip neutralize in finally
                    return true;
                } else { // case (F,F)
                    return false;  // nothing to replace
                }
            }
        } finally {
            // neutralize any unused ref wrappers
            if (oldEntry != null) {
                oldEntry.clearPhantom();
            }
            if (newEntry != null) {
                newEntry.clearPhantom();
            }
        }
    }

    /** Replace entry for key only if currently mapped to some value.
     * To maintain the illusion of an invisible cache,
     * the diskMap value must be read to see if oldValue
     * matches. The replace() operation is performed on memCache
     * and then on diskCache, without synchronizing over the pair of
     * operations.
     * <p/>Possible disk io to swap in from diskMap.
     * 
     * <p/>
     * Warning:  This method violates the "only expose one value instance" 
     *   design rule for this class.  Multiple instances means that it is
     *   undetermined as to which instance will be saved to disk last.
     * This method can be safely used when only one thread has access to 
     * this instance of CachedBdbMap.
     * <pre>
     * Preconditions: (diskMap.containsKey(), memMap.containsKey()) are:
     * (F,F) nothing to replace. PostCondition: (F,F) no change.
     * (T,F) value must be swapped-in, do replace(), PostCondition: (T,T) if
     *    replace occurred or not (value might have been replaced), diskMap
     *    need not be updated because expunge will do that.
     * (*,T) normal swapped-in condition (we can assume that the method which
     *    put a mapping to memMap took care of creating one for diskMap), 
     *    memMap value is most recent and expunge can take care of updating 
     *    diskMap, so we only do replace() on memMap.
     * </pre>
     * @return previous value if the replace was peformed on either memMap
     *   or diskMap, the previous value in memMap is returned if non-null,
     *   otherwise the previous from diskMap is returned, if no match in 
     *   either map, then null is returned.
     */
    @SuppressWarnings("unchecked")
    //@Override
    public V replace(K key, V value) {
        int ru = useStatsReplaceUsed.incrementAndGet();
        if (ru == 1 && LOG_ERROR_ON_DESIGN_VIOLATING_METHODS) {
            // warn on non-accretive use
            logger.log(Level.WARNING,"design violating replace(,) used on dbName=" + dbName, new Exception());
        }
        SoftEntry<V> newEntry = new SoftEntry<V>(key, value, refQueue);
        V prevMemValue =  null;
        SoftEntry prevEntry = (SoftEntry)memMap.replace(key, newEntry); // unchecked cast
        if (prevEntry != null) { // case (*,T) AND matched
            prevMemValue = (V)prevEntry.get();
            // at this point an old softEntry and phantom is ready to GC but 
            //      it has an old value that expunge() will write to disk;
            //   hence the warning in comments above.
            return prevMemValue;
        } else { // case (?,F), no match in memMap, but does diskMap match?
            V prevDiskValue = (V)diskMap.replace(key, value); // unchecked cast
            if (prevDiskValue == null) { // case (F,F)
                // key not mapped previously
                newEntry.clearPhantom(); // instance not used
                return null; // nothing to replace
            } else { // case (T,F)
                // swap in 
                memMap.put(key, newEntry);
                return prevDiskValue;
            }
        }
    }

    /** A composite putIfAbsent() over memMap and diskMap.
     * If the specified key is not already associated with any value, 
     * associate it with the given value.
     * By modifying memMap and diskMap.
     * <p/>
     * diskMap is not updated every time a value is mutated because this
     * cached disk store attempts to be invisible to client code.  However,
     * This method will cause diskMap to be updated if there was no previous
     * because this was the case for v1.14.2 and earlier.  (It also means 
     * diskMap and memMap, both ConcurrentMaps, can be coherently maintained
     * from the perspective of a one-instance-only style cache with 
     * diskMap updated at expunge by always modifying diskMap first here.
     * The disk map size estimate is also reasonably correct if diskMap
     * always has any mapping that is in memMap.)
     * 
     * <p/> 
     * Swap in/First insert: diskMap.putIfAbsent(), 
     *     then memMap.putIfAbsent(), putIfAbsent() assures atomicity;
     * <pre>
     * Preconditions: (diskMap.containsKey(), memMap.containsKey(SoftEntry)) are:
     * (F,F) initial starting conditions: is absent, put to diskMap then memMap.
     * (F,T) transient remove(): await other thread to finish with memMap, 
     *     then proceed as (F,F) 
     *       OR (if remove not used) an unexpected data race occurred.
     * (T,F) reloadable from disk or in process of inserting first time:
     *     not absent.
     * (T,T) normal swapped in condition: not absent.
     * 
     * PostConditions:
     * (T,T) both memMap and diskMap will have entries for given key (if 
     *     null is returned, then the value given is the one mapped).
     * </pre>
     * @param key 
     * @param value the value to which the given key should map, but only 
     *     if there is existing mapping for key.
     * @return
     */
    @SuppressWarnings("unchecked")
    //@Override
    public V putIfAbsent(K key, V value) {
        expungeStaleEntries(); // possible disk io
        V existingDiskValue = null;
        int attemptTolerance = BDB_LOCK_ATTEMPT_TOLERANCE;
        while (--attemptTolerance > 0) {
            try {
                existingDiskValue = (V)diskMap.putIfAbsent(key,value);// unchecked cast, disk io
                break;
            } catch (Exception e) {
                if (e instanceof com.sleepycat.util.RuntimeExceptionWrapper
                        && e.getCause() instanceof DeadlockException
                        && attemptTolerance > 0) {
                    if (attemptTolerance == BDB_LOCK_ATTEMPT_TOLERANCE - 1) {
                        // emit once on first retry
                        logger.log(Level.WARNING,"BDB implicit transaction timeout while "
                                + "waiting to acquire lock, retrying; "
                                + " dbName=" + dbName + " key=" + key, new Exception());
                    }
                    continue;
                } else {
                    logger.warning("unexpected exception: " + e);
                    throw new RuntimeException(e);
                }
            }
        }
        if (existingDiskValue == null) { // insert to diskMap succeeded
            // case (F,*)
            diskMapSize.incrementAndGet();
            useStatsPutIfUsed.incrementAndGet();
            // next, put mapping into cache, if not already there
            SoftEntry<V> newEntry = new SoftEntry<V>(key, value, refQueue);
            SoftEntry<V> prevEntry =
                    (SoftEntry<V>) memMap.putIfAbsent(key, newEntry);
            if (prevEntry != null) {
                // case (F,T) transient remove(): await other thread to finish, 
                //      then proceed as (F,F).
                // diskMap insert succeeded but insert to memMap was skipped.  
                //  This branch occurs when a simultaneous 
                //  remove() is occurring.
                V prevMemValue = prevEntry.get();
                expungeStatsTransientCond.getAndIncrement();
                if (prevMemValue == null && isExpunged(prevEntry, key)) {
                    prevEntry = (SoftEntry<V>) memMap.putIfAbsent(key, newEntry);
                    if (prevEntry == null) {
                        // a concurrent expunge or remove() "happens before" this put
                        return null; // put succeeded
                    } else {
                        prevMemValue = prevEntry.get();
                    }
                }
                newEntry.clearPhantom(); // unused instance
                expungeStatsTransientCond2.getAndIncrement();
                // other thread won, return value it set
                return prevMemValue;
            } else {
                // case (F,F) 
                // both inserts to diskMap and then memMap succeeded
                return null; // put succeeded
            }
        } else { // case (T,*)  already exists in diskMap, so not absent.
            //  No new entry inserted, but current value must be returned.
            V retValue = get(key); // swap in, if necessary
            if (retValue == null) {
                // this can only  happen if there is a race with this.remove()
                // that just removed the key we inserted into diskMap.
                //   It is always possible that another thread could remove
                //  an entry before the calling client sees it, but this is 
                //  unlikely by design.  
                expungeStatsTransientCond3.getAndIncrement();
                logger.log(Level.WARNING,"unusual put/remove activity for key=" + key,new Exception());
            }
            return retValue;
        }
    }

    /** Helper for remove() methods.
     * Neutralize a map entry so that diskMap is not updated and remove
     * the entry from memMap.  This prevents expunge ("swap out") of 
     * the given entry.
     * @param prevEntry SoftEntry to remove from memMap.
     * @param key the key to match.
     */
    private void neutralizeMemMapEntry(SoftEntry<V> prevEntry, final K key) {
        do {
            if (prevEntry.expungeStarted()) {
                // GC or expunge already in progress.
                isExpunged(prevEntry, key);
                memMapRemoveOrWarn(key, prevEntry); // possibly redundant
                break;
            } else {
                // reserve expunge, then clear it
                if (prevEntry.startExpunge()) { // get exclusive access
                    memMapRemoveOrWarn(key, prevEntry);
                    prevEntry.clearPhantom(); // prevent expunge on old entry
                    break;
                } else {
                    // another thread started expunge
                    continue;
                }
            }
        } while (true);
    }
    
    /**
     * Note that a call to this method CLOSEs the underlying bdbje.
     * This instance is no longer of any use.  It must be re-initialized.
     * We close the db here because if this BigMap is being treated as a plain
     * Map, this is only opportunity for cleanup.
     */
    public synchronized void clear() {
        dumpExtraStats();
        this.memMap.clear();
        this.diskMap.clear();
        this.diskMapSize.set(0);
        close();
    }

    /** Remove mapping for the given key.
     * A mapping does not need to be swapped in to be removed.
     * Matching entry is removed from memMap and diskMap.
     * <pre>
     * Preconditions: (diskMap.containsKey(), memMap.containsKey(SoftEntry)) are:
     * (F,*) nothing to remove if diskMap has no entry.
     * (T,F) remove from diskMap, await expunge in memMap, if in progress.
     * (T,T) remove from diskMap, clear phantom to prevent expunge of memMap.
     * 
     * PostConditions:
     * (F,F) both memMap and diskMap will NOT have entries for given key (if 
     *     null is returned, then nothing was removed).
     * </pre>
     * @param key
     * @return old value or null if nothing removed.
     */
    @Override
    @SuppressWarnings("unchecked")
    public V remove(final Object key) { 
        V prevDiskValue = null;
        // lookup memMap so we can use remove(K,V) later
        SoftEntry<V> prevEntry = (SoftEntry<V>) memMap.get(key); // unchecked

        // map mutating operations always change diskMap first
        if ((prevDiskValue = (V) diskMap.remove(key)) != null) { // unchecked
            diskMapSize.decrementAndGet();
            int ru = useStatsRemove1Used.incrementAndGet();
            if (ru == 1) {
                // warn on non-accretive use
                logger.log(Level.WARNING,"remove() used for dbName=" + dbName, new Exception());
            }
        }
        // regardless of whether prevDiskValue is null, memMap remove
        //   must occur to ensure consistency.

        if (prevEntry == null) {
            // nothing to remove from memMap
            return prevDiskValue;
        }
        V prevValue = prevEntry.get();
        neutralizeMemMapEntry(prevEntry, (K) key);
        if (prevValue == null) {
            return prevDiskValue;
        } else {
            return prevValue;
        }
    }

    /** remove item matching both the key and value.  
     * Matching entry is removed from memMap and diskMap.
     * A mapping does not need to be swapped in to be removed.
     * If the entry matching key is swapped in, then value is used 
     * to find and remove the entry in memMap (the value is of type V), 
     * and diskMap.remove(K) is used to remove the persistent entry.
     * 
     * Otherwise, a diskMap.remove(K,V) is performed.
     * <pre>
     * Preconditions: (diskMap.containsKey() AND value.equals(V), 
     *        memMap.containsKey(SoftEntry) AND value.equals(V) AND
     *        swapped-in) are:
     * (F,F) nothing to do.
     * (F,T) diskMap.remove(K,V) fails, but memMap.remove(K,V) still possible
     * (T,F) remove from diskMap, await expunge in memMap, if in progress.
     * (T,T) remove from diskMap, clear phantom to prevent expunge of memMap.
     * 
     * PostConditions:
     * (F,F) both memMap and diskMap will NOT have entries for given key (if 
     *     false is returned, then nothing was removed).
     * </pre>
     * @param key key used for matching mapping to remove.
     * @param value value used for matching mapping to remove.
     * @return true if entry removed.
     */
    //@Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object key, Object value) { // ConcurrentMap version
        SoftEntry<V> entry = null;
        V memValue = null;
        
        // first get cache entry since we want to inhibit expunge
        //   if the remove succeeds on memMap
        entry = (SoftEntry<V>) memMap.get(key); // unchecked cast
        if (entry != null) {
            // entry found for key
            memValue = entry.get();
            if (memValue != null && memValue.equals(value)) {
                // case (*,T) AND swapped in
                // swapped-in value matches
                int r2u = useStatsRemove2Used.incrementAndGet();
                if (r2u == 1 && LOG_ERROR_ON_DESIGN_VIOLATING_METHODS) {
                    logger.log(
                        Level.WARNING,
                        "design violating remove(,) used for dbName="+ dbName, 
                        new Exception());
                }
                // map mutating operations always change diskMap first
                Object obj = diskMap.remove(key);
                if (obj != null) {
                    diskMapSize.decrementAndGet();
                }
                neutralizeMemMapEntry(entry, (K) key); // also removes entry from memMap
                return true;
            } else { // case (*,F) AND swapped in
                // if swapped in and value does not match, then
                //     diskMap is *not* checked since it's value is
                //     not necessarily up to date (when swapped in)
                return false;
            }
        }
        // already covered cases: swapped in
        //  cases: (*,F)  AND swapped out
        boolean diskMapFound = diskMap.remove(key, value);
        if (diskMapFound) { // case (T,F) AND swapped out
            int r2u = useStatsRemove2Used.incrementAndGet();
            if (r2u == 1 && LOG_ERROR_ON_DESIGN_VIOLATING_METHODS) {
                logger.log(
                    Level.WARNING, 
                    "design violating remove(,) used for dbName=" + dbName);
            }
            diskMapSize.decrementAndGet();
            return true;
        } else {
            return false; // case (F,F) AND swapped out
        }
    }

    public synchronized boolean containsKey(Object key) {
        if (quickContainsKey(key)) {
            return true;
        }
        return diskMap.containsKey(key);
    }

    private boolean quickContainsKey(Object key) {
        expungeStaleEntries(); // note: expunge is not quick
        return memMap.containsKey(key);
    }

    /**
     * This method is not supported.
     * @deprecated 
     * @param value
     * @return
     */
    public synchronized boolean containsValue(Object value) {
        if (quickContainsValue(value)) {
            return true;
        }
        return diskMap.containsValue(value);
    }

    private boolean quickContainsValue(Object value) {
        expungeStaleEntries(); // note: expunge is not quick
        // FIXME this isn't really right, as memMap is of SoftEntries
        return memMap.containsValue(value);
    }

    public int size() {
        return diskMapSize.get();
    }
    
    protected String getDatabaseName() {
        String name = "DbName-Lookup-Failed";
        try {
            if (this.db != null) {
                name = this.db.getDatabaseName();
            }
        } catch (DatabaseException e) {
            // Ignore.
        }
        return name;
    }
    
    /**
     * Sync in-memory map entries to backing disk store.
     * When done, the memory map will be cleared and all entries stored
     * on disk.
     */
    @SuppressWarnings("unchecked")
    public synchronized void sync() {
        String dbName = null;
        // Sync. memory and disk.
        useStatsSyncUsed.incrementAndGet();
        long startTime = 0;
        if (logger.isLoggable(Level.INFO)) {
            dbName = getDatabaseName();
            startTime = System.currentTimeMillis();
            logger.info(dbName + " start sizes: disk " + this.diskMapSize.get() +
                ", mem " + this.memMap.size());
        }
        expungeStaleEntries();
        LinkedList<SoftEntry<V>> stale = new LinkedList<SoftEntry<V>>(); 
        for (Iterator<K> i = this.memMap.keySet().iterator(); i.hasNext();) {
            K key = i.next();
            SoftEntry<V> entry = memMap.get(key);
            if (entry != null) {
                // Get & hold so not cleared pre-return.
                V value = entry.get();
                if (value != null) {
                    expungeStatsDiskPut.incrementAndGet();
                    this.diskMap.put(key, value); // unchecked cast
                } else {
                    stale.add(entry);
                }
            }
        }
        // for any entries above that had been cleared, ensure expunged
        for (SoftEntry<V> entry : stale) {
            expungeStaleEntry(entry, false);
        }   
        
        // force sync of deferred-writes
        try {
            this.db.sync();
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        if (logger.isLoggable(Level.INFO)) {
            logger.info(dbName + " sync took " +
                (System.currentTimeMillis() - startTime) + "ms. " +
                "Finish sizes: disk " +
                this.diskMapSize.get() + ", mem " + this.memMap.size());
            dumpExtraStats();
        }
    }

    /** log at INFO level, interesting stats if non-zero. */
    private void dumpExtraStats() {
        if (logger.isLoggable(Level.INFO)) {
            // emit if FINE or has interesting values
            if (logger.isLoggable(Level.FINE)  ||
                    (expungeStatsNullPhantom.get() 
                    + expungeStatsTransientCond.get() 
                    + expungeStatsTransientCond2.get() 
                    + expungeStatsTransientCond3.get() 
                    + expungeStatsAwaitExpunge.get() 
                    + expungeStatsNullValue.get() 
                    + expungeStatsNotInMap.get() 
                    + expungeStatsAwaitTimeout.get()
                    + expungeStatsSwapIn.get()
                    + expungeStatsSwapInRetry.get()
                    + expungeStatsSwapInRetry2.get()
                    + expungeStatsGetMemCheck.get()
                    + expungeStatsViaPoll.get()
                    + useStatsRemove1Used.get() 
                    + useStatsRemove2Used.get() 
                    + useStatsReplaceUsed.get()) > 0) {
                logger.info(composeCacheSummary() + "  "
                        + expungeStatsNullPhantom + "=NullPhantom  " 
                        + expungeStatsTransientCond + "=TransientCond  " 
                        + expungeStatsTransientCond2 + "=TransientCond2  " 
                        + expungeStatsTransientCond3 + "=TransientCond3  " 
                        + expungeStatsAwaitExpunge + "=AwaitExpunge  " 
                        + expungeStatsNullValue + "=NullValue  " 
                        + expungeStatsNotInMap+ "=NotInMap  " 
                        + expungeStatsAwaitTimeout+ "=AwaitTimeout  " 
                        + expungeStatsSwapIn+ "=SwapIn  " 
                        + diskHit+ "=diskHit  " 
                        + cacheHit+ "=cacheHit  " 
                        + countOfGets+ "=countOfGets  " 
                        + expungeStatsSwapInRetry+ "=SwapInRetry  " 
                        + expungeStatsSwapInRetry2+ "=SwapInRetry2  " 
                        + expungeStatsDiskPut + "=DiskPut  "
                        + expungeStatsGetMemCheck + "=GetMemCheck  "
                        + expungeStatsViaPoll + "=ViaPoll  "
                        + useStatsRemove1Used + "=Remove1Used  " 
                        + useStatsRemove2Used + "=Remove2Used  " 
                        + useStatsReplaceUsed + "=ReplaceUsed  " 
                        + useStatsPutUsed + "=PutUsed  " 
                        + useStatsPutIfUsed+ "=PutIfUsed  " 
                        + useStatsSyncUsed+ "=SyncUsed  " 
                        + diskMapSize.get()+ "=diskMapSize  " 
                        + memMap.size()+ "=memMap.size  " 
                        );
            }
        }
    }

    /** An incremental, poll-based expunger.
     * See #Expunger for dedicated thread expunger. 
     */
    @SuppressWarnings("unchecked")
    private void expungeStaleEntries() {
        int c = 0;
        long startTime = System.currentTimeMillis();
        for(SoftEntry<V> entry; (entry = (SoftEntry<V>)refQueuePoll()) != null;) {
            expungeStaleEntry(entry, false);
            expungeStatsViaPoll.incrementAndGet();
            c++;
        }
        if (c > 0 && logger.isLoggable(Level.FINER)) {
            long endTime = System.currentTimeMillis();
            try {
                logger.finer("DB: " + db.getDatabaseName() + ",  Expunged: "
                        + c + ", Diskmap size: " + diskMapSize.get()
                        + ", Cache size: " + memMap.size()
                        + ", in "+(endTime-startTime)+"ms");
            } catch (DatabaseException e) {
                logger.log(Level.FINER,"exception while logging",e);
            }
        }
    }
    
    /** Expunge an entry from memMap while updating diskMap.
     * Concurrent implementation.
     * 
     * This thread is the only thread performing an expunge of the given
     * entry (if it or the calling thread sees a true SoftEntry.startExpunge()),
     * so we can assume exclusive access to the given entry.
     * 
     *  3. Swap out/flush: diskMap update, then memMap remove: 
     *     diskMap.put(k,v2); // exclusive only if performed when processing ref queue
     *     memMap.remove(k,v2); if memMap race lost, then no harm done;
     *    3.1. Only expunge does an update of an existing diskMap entry (except
     *        for sync()).
     *  Possible disk io as data is written to BDB map.
     * <pre>
     * Preconditions: (diskMap.containsKey(K), memMap.containsKey(SoftEntry)) are:
     * (F,F) remove() or replace was used.
     * (F,T) transient while remove() or replace in progress.
     * (T,F) normal condition, memMap value is most recent
     * (T,T) normal condition, memMap value is cleared SoftReference and 
     *     the attached PhantomEntry doctoredGet() has most recent value.
     * 
     * PostConditions:
     * (T,F)  for K key, memMap will not have the given mapping and diskMap 
     *     will have a mapping to the value held by the phantom value 
     *     held by given entry, if entry was not already expunged. 
     * </pre>
     * The PostCondition only applies if exclusive expunge access is obtained.
     * If another thread is already doing the expunge of given entry, this
     * will not wait for that to finish.
     * 
     * @param entry a SoftEntry<V> obtained from refQueuePoll().
     * @param alreadyExclusive if true, then assume this thread already has
     *     SoftEntry.startExpunge()==true.
     */
    @SuppressWarnings("unchecked")
   private void expungeStaleEntry(SoftEntry<V> entry, final boolean alreadyExclusive) { // concurrent version
        // If phantom already null, its already expunged -- probably
        // because it was purged directly first from inside in
        // {@link #get(String)} and then it went on the poll queue and
        // when it came off inside in expungeStaleEntries, this method
        // was called again.
        //  OR, could be http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6837858
        //
        V phantomValue = null;
        // keep this ref on stack so it cannot be nulled in mid-operation
        PhantomEntry<V> phantom = (PhantomEntry<V>)entry.getPhantom(); // unchecked cast
        if (phantom == null) {
            expungeStatsNullPhantom.incrementAndGet(); 
            entry.clearPhantom(); // free up waiting threads, if any
            return; // nothing to do
        } else {
            // recover hidden value
            phantomValue = (V)phantom.doctoredGet(); // unchecked cast
        }
        if (alreadyExclusive || entry.startExpunge()) { // exclusive expunge
            try {
                // Still in memMap? 
                //  If the object that is in memMap is not the one passed here, then
                // memMap has been changed -- probably by a put on top of this entry.]
                if (memMap.get(phantom.getKey()) 
                        == entry) { // NOTE: intentional identity compare
                    // given instance entry still in memMap;
                    //   as long as CachedBDBMap.remove(), CachedBDBMap.put(), 
                    //   and CachedBDBMap.replace() are not used
                    //   by application, then only this thread is doing the expunge.
                    
                    // we have the key and if we have a phantom Value, then
                    //   the diskMap can be updated.
                    // diskMap MUST be updated *before* entry.clearPhantom().
                    //  Because this thread is the only one expunging the given entry.getKey() 
                    //    we can use diskMap.put().
                    if (phantomValue != null) {
                        diskMap.put(phantom.getKey(), phantomValue); // unchecked cast
                        expungeStatsDiskPut.incrementAndGet();
                    } else {
                        expungeStatsNullValue.incrementAndGet();
                    }
                    //  remove entry that matches key and value; 
                    //    since only this thread has the exclusive expunge job
                    //    for entry, we will either remove the exact object
                    //    or remove no object (non-interference).
                    memMapRemoveOrWarn(phantom.getKey(), entry);
                } else { // not in map (a soft assert failure)
                    this.expungeStatsNotInMap.incrementAndGet();
                }
            } finally { // an exclusive expunge must clear entry
                //  but only after diskMap and memMap are updated
                entry.clearPhantom();
            }
        }
    }
    
    private static class PhantomEntry<T> extends PhantomReference<T> {
        private final Object key;

        public PhantomEntry(Object key, T referent) {
            super(referent, null);
            this.key = key;
        }

        /**
         * @return Return the referent. The contract for {@link #get()}
         * always returns a null referent.  We've cheated and doctored
         * PhantomReference to return the actual referent value.  See notes
         * at {@link #referentField};
         */
        final public Object doctoredGet() {
            try {
                // Here we use the referentField saved off on static
                // initialization of this class to get at this References'
                // private referent field.
                return referentField.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return Returns the key.
         */
        final public Object getKey() {
            return this.key;
        }

        /** 
         * @param obj
         * @return true if the referents are equals().
         */
        @Override
        @SuppressWarnings("unchecked")
        final public boolean equals(Object obj) {
            if (obj == null) {
                return false;
    }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PhantomEntry<T> other = (PhantomEntry<T>) obj; // unchecked cast
            if (this.doctoredGet() != other.doctoredGet() 
                    && (this.doctoredGet() == null 
                        || !this.doctoredGet().equals(other.doctoredGet()))) {
                return false;
            }
            return true;
        }

        @Override
        final public int hashCode() {
            return this.key != null ? this.key.hashCode() : 0;
        }

    }

    /** 
     * SoftReference cache entry.
     * "Expunge" is the process of flushing entries in the cache (memMap) 
     * to diskMap after GC determines they are no longer references.
     * A PhantomReference is used to hold the key and value as a last
     * chance before GC hook that can effect the update of diskMap.
     * <p/>
     *   The coordinated process to do expunge is: 
     * <pre>
     *     SoftEntry se = map.get();
     *     if (se.startExpunge()) {
     *         ;// update diskMap here
     *         map.remove(); // remove SoftEntry after diskMap update
     *         se.clearPhantom(); // clear after remove mapping
     *     } else { // another thread does expunge
     *     }
     * </pre>
     *   The coordinated process to wait for another thread to do expunge: 
     * <pre>
     *     SoftEntry se = map.get();
     *     if (se.awaitExpunge()) {
     *         ;// wait occurred (if you care)
     *     } 
     *     ;// postcondition: se was removed from map
     * </pre>
     * Entries are not recycled.
     */
    private static class SoftEntry<T> extends SoftReference<T> {
        private PhantomEntry phantom;

        /** count down to expunged; if null, then expunge has not
         *   been started.  See {@link #startExpunge()} and
         *   {@link #expungeStarted()}.
         *  Effectively final (never goes back to null).
         */
        private CountDownLatch expunged = null;

        @SuppressWarnings("unchecked")
        public SoftEntry(Object key, T referent, ReferenceQueue<T> q) {
            super(referent, q);
            this.phantom = new PhantomEntry(key, referent); // unchecked cast
        }

        /**
         * Synchronized so this can be atomic and have
         * proper happens-before (inter-thread data sync) 
         * w.r.t. other methods of this instance regardless of 
         * what thread is the caller.
         * @return referent.
         */
        public T get() {
            // ensure visibility
            synchronized (this) {
                return super.get();
            }
        }

        /**
         * Get the phantom.
         *
         * Synchronized so this can be atomic and have
         * proper happens-before (inter-thread data sync) 
         * w.r.t. other methods of this instance regardless of 
         * what thread is the caller.
         * @return Returns the phantom reference.
         */
        final public synchronized PhantomEntry getPhantom() {
            return this.phantom;
        }
        
        /**
         * @return true if this entry is at end of lifecycle.
         */
        final public boolean isCleared() {
            return getPhantom() == null;
        }
        
        /** clear referent and end-of-life-cycle this instance.
         * Do not clearPhantom() until after diskMap updated and after
         * this entry is removed from memMap (because threads waiting
         * for expunge to finish will likely do a memMap.get() 
         * or putIfAbsent() and
         * there should be no residue so a fresh entry can be created
         * when the value is swapped in from diskMap.
         * <p/>Causes any threads that called or will later
         * call awaitExpunge() on this
         * instance to continue
         * 
         * <p/>Idempotent.
         * 
         * After this method returns, 
         *     {@link #clearPhantom()} will return true. 
         * 
         * Synchronized so this can be atomic and have
         * proper happens-before (inter-thread data sync) 
         * w.r.t. other methods of this instance regardless of 
         * what thread is the caller.
         */
        final public synchronized void clearPhantom() {
            if (this.phantom != null) {
                this.phantom.clear();
                this.phantom = null;
            }
            super.clear();
            if (expungeStarted()) {
                expunged.countDown();
            }
        }
    
        /**
         * Was expunge ever started on this instance?
         * (If instance was temporary and thrown away,
         * we pretend it was expunged.)
         * 
         * Synchronized so this can be atomic and have
         * proper happens-before (inter-thread data sync) 
         * w.r.t. other methods of this instance regardless of 
         * what thread is the caller.
         * No side-effects.
         * @return true if expunge was ever started on this entry,
         *     expunge may or may not be finished and #isCleared()
         *     may or may not be true, 
         *     see {@link #awaitExpunge()}
         * @see {@link CachedBdbMap#isExpunged(SoftEntry)}
         */
        final public synchronized boolean expungeStarted() {
            if (expunged != null) {
                return true;
            } else {
                return false;
            }
        }

        /** Can the calling thread perform the expunge on this?
         * Side Effect: if true is returned, sets up latch for any
         *    threads that want to await for expunge.
         * 
         * Synchronized so this can be atomic and have
         * proper happens-before (inter-thread data sync) 
         * w.r.t. other methods of this instance regardless of 
         * what thread is the caller.
         * @return true if calling thread has exclusive access to
         *     expunge this instance; do not call this unless
         *     going to do the expunge; true is only returned
         *     once per instance.
         */
        final public synchronized boolean startExpunge() {
            if (isCleared() || expungeStarted()) {
                return false;
            } else {
                expunged = new CountDownLatch(1);
                return true;
    }
        }
    
        /** If this entry is in the process of being expunged 
         * (when {@link #expungeStarted()} is true), then this will wait
         * until the expunge is finished.  If expunge was not started
         * or was finished, this method returns immediately.
         * 
         * To ensure liveliness, a multi sec timeout is used that
         * if triggered, throws InterruptedException.
         *
         * Effectively synchronized so this can be atomic and have
         * proper happens-before (inter-thread data sync) 
         * w.r.t. other methods of this instance regardless of 
         * what thread is the caller.
         * 
         * @throws java.lang.InterruptedException if timeout occurred.
         * @return true if expunge is completed (or timeout) or this 
         *    entry was never used, false if expunge is not happening.
         */
        final public boolean awaitExpunge() throws InterruptedException {
            if (isCleared()) {
                return true; // expunge completed, or entry was unused
            }
            if (expungeStarted()) {
                // much longer than time for diskMap.putIfAbsent(key,value)
                long plentyOfWriteLatencySec = 6L;
                if (expunged.await(plentyOfWriteLatencySec, TimeUnit.SECONDS)) {
                    return true; // finished
                } else { // timeout, just in case
                    expungeStatsAwaitTimeout.incrementAndGet();
                    logger.log(Level.WARNING, "timeout at " 
                            + plentyOfWriteLatencySec + " sec", new Exception());
                    throw new InterruptedException("timeout");
                }
            } else { 
                return false; // not started
            }
        }
 
        /**
         * 
         * @param obj
         * @return true if the references are equal, and if references differ,
         *     if phantoms are == or equals(), return true.
         */
        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (this == obj) { // same reference
                return true; 
            }
            final SoftEntry<T> other = (SoftEntry<T>) obj; // unchecked cast
            if (this.phantom != other.phantom 
                    && (this.phantom == null 
                        || !this.phantom.equals(other.phantom))) {
                return false;
            }
            return true;
        }
    
        @Override
        public int hashCode() {
            return this.phantom != null ? this.phantom.hashCode() : 0;
        }
        
        public String toString() {
            if (phantom != null) {
                return "SoftEntry(key=" + phantom.getKey().toString() + ")";
            } else {
                return "SoftEntry()";
            }
        }
    }

    
 
    
    @SuppressWarnings("unchecked")
    private K toKey(Object o) {
        return (K)o;
    }
    
    @SuppressWarnings("unchecked")
    private V diskMapGet(K k) {
        return (V)diskMap.get(k);
    }
    
    @SuppressWarnings("unchecked")
    private SoftEntry<?> refQueuePoll() {
        return (SoftEntry<?>)refQueue.poll();
    }
    
    //
    // Crude, probably unreliable/fragile but harmless mechanism to 
    // trigger expunge of cleared SoftReferences in low-memory 
    // conditions even without any of the other get/put triggers. 
    //
    
    protected transient SoftReference<LowMemoryCanary> canary;
    protected class LowMemoryCanary {
        /** When collected/finalized -- as should be expected in 
         *  low-memory conditions -- trigger an expunge and a 
         *  new 'canary' insertion. */
        public void finalize() {
            CachedBdbMap.this.expungeStaleEntries();
            // only install new canary if map still 'open' with db reference
            if(CachedBdbMap.this.db !=null) {
                CachedBdbMap.this.canary = 
                    new SoftReference<LowMemoryCanary>(new LowMemoryCanary());
            } else {
                CachedBdbMap.this.canary = null;
            }
        }
    }
}
