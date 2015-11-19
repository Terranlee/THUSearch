/* CachedBdbMapTest
 * 
 * Created on Apr 11, 2005
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
package org.archive.util;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.math.RandomUtils;

/**
 * @author stack
 * @version $Date: 2010-04-12 21:49:16 +0000 (Mon, 12 Apr 2010) $, $Revision: 6816 $
 */
@SuppressWarnings("deprecation")
public class CachedBdbMapTest extends TmpDirTestCase {
    File envDir; 
    private CachedBdbMap<String,HashMap<String,String>> cache;
    
    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        super.setUp();
        this.envDir = new File(getTmpDir(),"CachedBdbMapTest");
        this.envDir.mkdirs();
        this.cache = new CachedBdbMap(this.envDir,
            this.getClass().getName(), String.class, HashMap.class);
    }
    
    protected void tearDown() throws Exception {
        this.cache.close();
        FileUtils.deleteDir(this.envDir);
        super.tearDown();
    }
    
    @SuppressWarnings("unchecked")
    public void xestReadConsistencyUnderLoad() throws Exception {
        final CachedBdbMap<String,AtomicInteger> cbdbmap = 
            new CachedBdbMap(
                    this.envDir, 
                    this.getClass().getName(), 
                    Integer.class, 
                    AtomicInteger.class);
        try {
            final AtomicInteger level = new AtomicInteger(0);
            final int keyCount = 128 * 1024; // 128K  keys
            final int maxLevel = 64; 
            // initial fill
            for(int i=0; i < keyCount; i++) {
            	AtomicInteger prevVal = cbdbmap.putIfAbsent(""+i, new AtomicInteger(level.get()));
                assertNull("unexpected prior value", prevVal);
            }
            // backward checking that all values always at level or higher
            new Thread() {
                public void run() {
                    untilmax: while(true) {
                        for(int j=keyCount-1; j >= 0; j--) {
                            int targetValue = level.get(); 
                            if(targetValue>=maxLevel) {
                                break untilmax;
                            }
                            assertTrue("stale value revseq key "+j,cbdbmap.get(j).get()>=targetValue);
                            Thread.yield();
                        }
                    }
                }
            }.start();
            // random checking that all values always at level or higher
            new Thread() {
                public void run() {
                    untilmax: while(true) {
                        int j = RandomUtils.nextInt(keyCount);
                        int targetValue = level.get(); 
                        if(targetValue>=maxLevel) {
                            break untilmax;
                        }
                        assertTrue("stale value random key "+j,
                                cbdbmap.get(""+j).get()>=targetValue);
                        Thread.yield();
                    }
                }
            }.start();
            // increment all keys
            for(; level.get() < maxLevel; level.incrementAndGet()) {
                for(int k = 0; k < keyCount; k++) {
                    int foundValue = cbdbmap.get(""+k).getAndIncrement();
                    assertEquals("stale value preinc key "+k, level.get(), foundValue);
                }
                if(level.get() % 10 == 0) {
                    System.out.println("level to "+level.get());
                }
                Thread.yield(); 
            }
        } finally {
            cbdbmap.close();
        }
        // SUCCESS
    }
    
    public void testBackingDbGetsUpdated() {
        // Enable all logging. Up the level on the handlers and then
        // on the big map itself.
        Handler [] handlers = Logger.getLogger("").getHandlers();
        for (int index = 0; index < handlers.length; index++) {
            handlers[index].setLevel(Level.FINEST);
        }
        Logger.getLogger(CachedBdbMap.class.getName()).
            setLevel(Level.FINEST);
        // Set up values.
        final String value = "value";
        final String key = "key";
        final int upperbound = 3;
        // First put in empty hashmap.
        for (int i = 0; i < upperbound; i++) {
        	HashMap<String,String> prevVal = this.cache.putIfAbsent(key + Integer.toString(i), new HashMap<String,String>());
            assertNull("unexpected previous value", prevVal);
        }
        // Now add value to hash map.
        for (int i = 0; i < upperbound; i++) {
            HashMap<String,String> m = this.cache.get(key + Integer.toString(i));
            m.put(key, value);
        }
        this.cache.sync();
        for (int i = 0; i < upperbound; i++) {
            HashMap<String,String> m = this.cache.get(key + Integer.toString(i));
            String v = m.get(key);
            if (v == null || !v.equals(value)) {
                Logger.getLogger(CachedBdbMap.class.getName()).
                    warning("Wrong value " + i);
            }
        }
    }
    
    /**
     * Test that in scarce memory conditions, the memory map is 
     * expunged of otherwise unreferenced entries as expected.
     * 
     * NOTE: this test may be especially fragile with regard to 
     * GC/timing issues; relies on timely finalization, which is 
     * never guaranteed by JVM/GC. For example, it is so sensitive
     * to CPU speed that a Thread.sleep(1000) succeeds when my 
     * laptop is plugged in, but fails when it is on battery!
     * 
     * @throws InterruptedException
     */
    public void testMemMapCleared() throws InterruptedException {
        System.gc(); // minimize effects of earlier test heap use
        assertEquals(cache.memMap.size(), 0);
        for(int i=0; i < 10000; i++) {
            cache.putIfAbsent(""+i, new HashMap<String,String>());
        }
        assertEquals(cache.memMap.size(), 10000);
        assertEquals(cache.size(), 10000);
        TestUtils.forceScarceMemory();
        Thread.sleep(3000);
        // The 'canary' trick makes this explicit expunge, or
        // an expunge triggered by a get() or put...(), unnecessary
        // cache.expungeStaleEntries();
        System.out.println(cache.size()+","+cache.memMap.size());
        assertEquals(0, cache.memMap.size());
    }
    
    
    public static void main(String [] args) {
        junit.textui.TestRunner.run(CachedBdbMapTest.class);
    }
}
