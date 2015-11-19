/* StoredQueueTest.java
 *
 * $Id: StoredQueueTest.java 5197 2007-06-06 01:31:46Z gojomo $
 *
 * Created on Jun 14, 2007
 *
 * Copyright (C) 2007 Internet Archive
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
package org.archive.queue;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;

import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;
import org.archive.util.bdbje.EnhancedEnvironment;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;

public class StoredQueueTest extends TmpDirTestCase {
    StoredQueue<String> queue;
    EnhancedEnvironment env;
    Database db; 
    File envDir; 

    protected void setUp() throws Exception {
        super.setUp();
        this.envDir = new File(getTmpDir(),"StoredMapTest");
        this.envDir.mkdirs();
        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setTransactional(false);
            envConfig.setAllowCreate(true);
            env = new EnhancedEnvironment(envDir,envConfig);
            DatabaseConfig dbConfig = StoredQueue.databaseConfig();
            db = env.openDatabase(null, "StoredMapTest", dbConfig);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
        this.queue = new StoredQueue<String>(db, String.class, env.getClassCatalog());
    }
    
    protected void tearDown() throws Exception {
        db.close();
        env.close(); 
        FileUtils.deleteDir(this.envDir);
        super.tearDown();
    }
    
    public void testAdd() {
        assertEquals("not empty at start",0,queue.size());
        fill(queue, 10);
        assertEquals("unexpected size at full",10,queue.size());
    }

    /**
     * @deprecated Use {@link #fill(Queue,int)} instead
     */
    protected void fill(int size) {
        fill(queue, size);
    }

    protected void fill(java.util.Queue<String> q, int size) {
        for(int i = 1; i <= size; i++) {
            q.add("item-"+i);
        }
    }
    
    protected int drain(java.util.Queue<String> q) {
        int count = 0; 
        while(true) {
            try {
                q.remove();
                count++;
            } catch(NoSuchElementException nse) {
                return count;
            }
        }
    }

    public void testClear() {
        fill(queue, 10);
        queue.clear();
        assertEquals("unexpected size after clear",0,queue.size());
    }

    public void testRemove() {
        fill(queue, 10);
        assertEquals("unexpected remove value","item-1",queue.remove());
        assertEquals("improper count of removed items",9,drain(queue));
        try {
            queue.remove();
            fail("expected NoSuchElementException not received");
        } catch (NoSuchElementException nse) {
            // do nothing
        }
    }
    
    public void testOrdering() {
        fill(queue, 10);
        for(int i = 1; i <= 10; i++) {
            assertEquals("unexpected remove value","item-"+i,queue.remove());
        }
    }

    public void testElement() {
        fill(queue, 10);
        assertEquals("unexpected element value","item-1",queue.element());
        assertEquals("unexpected element value",queue.peek(),queue.element());
        queue.clear();
        try {
            queue.element();
            fail("expected NoSuchElementException not received");
        } catch (NoSuchElementException nse) {
            // do nothing
        }
    }

    public void xestTimingsAgainstLinkedBlockingQueue() {
        tryTimings(50000);
        tryTimings(500000);
    }

    private void tryTimings(int i) {
        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<String>();
        long start = System.currentTimeMillis();
        fill(lbq,i);
        drain(lbq);
        long finish = System.currentTimeMillis();
        System.out.println("LBQ - "+i+":"+(finish-start));
        start = System.currentTimeMillis();
        fill(queue,i);
        drain(queue);
        finish = System.currentTimeMillis();
        System.out.println("SQ - "+i+":"+(finish-start));
    }
}
