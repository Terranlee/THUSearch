/* ARHostQueueTest.java
*
* Created on Sep 13, 2004
*
* Copyright (C) 2004 Kristinn Sigur?sson.
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
package org.archive.crawler.frontier;

import java.io.File;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.TmpDirTestCase;
import org.archive.util.FileUtils;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * A JUnit test for {@link AdaptiveRevisitHostQueue AdaptiveRevisitHostQueue}
 * class. 
 * <p>
 * Since the ARHostQueue maintains significant state information there is only
 * one Unit test described here that tests various different transitions.
 *
 * @author Kristinn Sigurdsson
 */
public class AdaptiveRevisitHostQueueTest
extends TmpDirTestCase
implements AdaptiveRevisitAttributeConstants {
    public void testHQ() throws Exception {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true); 
        envConfig.setAllowCreate(true);    
        File envDir = new File(getTmpDir(), "AR");
        if (envDir.exists()) {
            FileUtils.deleteDir(envDir);
        }
        envDir.mkdirs();
        Environment env = new Environment(envDir, envConfig);
        // Open the class catalog database. Create it if it does not
        // already exist. 
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        StoredClassCatalog catalog =
            new StoredClassCatalog(env.openDatabase(null, "classes", dbConfig));
        AdaptiveRevisitHostQueue hq =
            new AdaptiveRevisitHostQueue("bok.hi.is", env, catalog, 1);


        // Make the CrawlUris
        CrawlURI[] curis = {null,null,null,null};

        UURI uuri = UURIFactory.getInstance("http://bok.hi.is/1.html");
        curis[0] = new CrawlURI(uuri);
        curis[0].setVia(null);
        
        uuri = UURIFactory.getInstance("http://bok.hi.is/2.html");
        curis[1] = new CrawlURI(uuri);
        curis[1].setVia(null);

        uuri = UURIFactory.getInstance("http://bok.hi.is/3.html");
        curis[2] = new CrawlURI(uuri);
        curis[2].setVia(null);

        uuri = UURIFactory.getInstance("http://bok.hi.is/4.html");
        curis[3] = new CrawlURI(uuri);
        curis[3].setVia(null);

        assertTrue("HQ should be empty initially",
                hq.getState() == AdaptiveRevisitHostQueue.HQSTATE_EMPTY);
        assertEquals("Incorrect nextReadyTime on Empty",
                Long.MAX_VALUE,hq.getNextReadyTime());
        assertEquals("Initial size of HQ should be 0",0,hq.getSize());
        
        assertEquals("Peek should return null when 'ready queue' is empty", 
                null, hq.peek());
    
        /*
         * Add three CrawlURIs and ensures that the correct one is reported by 
         * peek(); All are added later then current time!
         */

        curis[0].putLong(
                A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis()); // now
        curis[1].putLong(
                A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis()+5000); // in 5 sec
        curis[2].putLong(
                A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis()+20000); // in 20 sec.
        
        hq.add(curis[0],false);
        assertEquals("First CrawlURI should be top",curis[0].toString(),
                hq.peek().toString());
        assertTrue("HQ should no longer be empty",
                hq.getState()!=AdaptiveRevisitHostQueue.HQSTATE_EMPTY);
        assertEquals("Size of HQ should now be 1",1,hq.getSize());
        
        /*
         * Invoke next and ensure that the HQ is now busy (initial valence was
         * set to 1). Also check for proper errors for a busy HQ. Such as when
         * trying to reinvoke next().
         *
         */
        CrawlURI curi = hq.next(); // Should return curis[2]
        assertEquals("next() did not return 'top' URI",
                curis[0].toString(),curi.toString());
        assertTrue("HQ should now be busy, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_BUSY);
        try{
            hq.next();
            assertTrue("next() should throw an IllegalStateException if HQ " +
                    "not ready",false);
        } catch(IllegalStateException e){
            // This is supposed to happen.
        }
        assertEquals("New top URI should be null",
                null,hq.peek());
        
        hq.add(curis[1],false);
        assertEquals("Second CrawlURI should be top",curis[1].toString(),
                hq.peek().toString());
        assertEquals("Size of HQ should now be 2",2,hq.getSize());

        // Return it with next fetch time in the future.
        curi.putLong(A_TIME_OF_NEXT_PROCESSING,
            hq.peek().getLong(A_TIME_OF_NEXT_PROCESSING)
                        +100000); // 100 sec behind current top.
        hq.update(curi,false,0);
        assertEquals("Second CrawlURI should be still be top",
                curis[1].toString(),hq.peek().toString());
        assertEquals("Size of HQ should still be 2",2,hq.getSize());
        
        hq.add(curis[2],false);
        assertEquals("Second CrawlURI should still be top",
                curis[1].toString(), hq.peek().toString());
        assertEquals("Size of HQ should now be 3",3,hq.getSize());

        /*
         * If there are no URIs ready, the queue should snooze, even though no
         * politeness demand has been made.
         * <p>
         * Confirms this and that it wakes up.
         */
        assertTrue("HQ should be snoozed, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_SNOOZED);
        // Wait past wakeup time        
        synchronized(this){
            wait(hq.getNextReadyTime()-System.currentTimeMillis()+100);
        }
        assertTrue("HQ should now be ready, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_READY);
    
        /*
         * Re-adds a URI with a lower ready time which should promote it to the
         * top of the queue. Checks if this happens correctly.
         * 
         * Then tests an add override which would demote it back, ensures that 
         * this fails as it should (i.e. URIs time of next processing remains 
         * unchanged).
         */
        curis[2].putLong(
                A_TIME_OF_NEXT_PROCESSING,
                curis[1].getLong(A_TIME_OF_NEXT_PROCESSING)
                            -1000); // 1 sec. prior to current top 
        hq.add(curis[2],true);
        assertEquals("Size of HQ should still be 3",hq.getSize(),3);
        assertEquals("Third CrawlURI should be now be top",
                curis[2].toString(), hq.peek().toString());
        curis[2].putLong(A_TIME_OF_NEXT_PROCESSING,
                curis[1].getLong(A_TIME_OF_NEXT_PROCESSING)
                            +10000); // 10 sec. later 
        hq.add(curis[2],true);
        assertEquals("Size of HQ should still be 3",hq.getSize(),3);
        assertEquals("Third CrawlURI should still top",
                curis[2].toString(), hq.peek().toString());

    
        /*
         * Invoke next and ensure that the HQ is now busy (initial valence was
         * set to 1). Also check for proper errors for a busy HQ. Such as when
         * trying to reinvoke next().
         *
         */
        curi = hq.next(); // Should return curis[2]
        assertEquals("next() did not return 'top' URI",
                curis[2].toString(),curi.toString());
        assertTrue("HQ should now be busy, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_BUSY);
        try{
            hq.next();
            assertTrue("next() should throw an IllegalStateException if HQ " +
                    "not ready",false);
        } catch(IllegalStateException e){
            // This is supposed to happen.
        }
        assertEquals("New top URI",
                curis[1].toString(),hq.peek().toString());
        
        /*
         * Add a URI while HQ is busy. Check if this succeeds normally.
         *
         */
        
        curis[3].putLong(A_TIME_OF_NEXT_PROCESSING,
                curis[1].getLong(A_TIME_OF_NEXT_PROCESSING) 
                        - 1); // 1 msec. ahead of current top (order [2] 3 1 0) 
        hq.add(curis[3],false);
        assertEquals("Size of HQ should now be 4",4,hq.getSize());
        
        
        /*
         * Invoke update, first with an invalid URI (not the one issued by 
         * next() earlier), this should fail. Then with the correct one, this  
         * should succeed. Then finally test update again with an invalid URI 
         * (i.e. when no HQ has no outstanding URIs, that should fail.
         * 
         * At each step, proper checks are made of state and that  methods give  
         * appropriate errors.
         * 
         * Updated URI is given low time of next processing to put it 'in front'
         */
    
        try {
            hq.update(curis[1],false,0);
            assertTrue("update() should not accept URI",false);
        } catch(IllegalStateException e){
            // This is supposed to happen
        }
        
        // We do not change the 'time of next processing' on update
        // so curis[2] should again be at top of queue. 
        long timeOfPolitenessWakeUp = System.currentTimeMillis()+2000;
        hq.update(curi,true,timeOfPolitenessWakeUp); // Wake in 5 sec.
        assertTrue("HQ should be snoozed, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_SNOOZED);
        
        try {
            hq.update(curis[2],false,0);
            assertTrue("update() should not accept URI",false);
        } catch(IllegalStateException e){
            // This is supposed to happen
        }
        assertEquals("HQs time of next ready should reflect set wait time ",
                timeOfPolitenessWakeUp, hq.getNextReadyTime());
        
        
        /*
         * Check if the HQ wakes up from it's 'snoozing'
         *
         */
        // Wait past wakeup time        
        synchronized(this){
            wait(hq.getNextReadyTime()-System.currentTimeMillis()+100);
        }
        assertTrue("HQ should now be ready, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_READY);
        assertEquals("HQs time of next ready should still be when it 'woken' " +
                "up.", timeOfPolitenessWakeUp, hq.getNextReadyTime());
   
        /*
         * Invoke next so that the HQ has a URI being processed. Then
         * close the HQ and reopen it to ensure that this happens normally, i.e.
         * state is recovered properly, including the restoration of the URI
         * being processed, back to the regular queue (where it should be 
         * first).
         * 
         * On recreating the HQ, set valence to 2.
         */
        curi = hq.next(); // Should return curis[2]
        assertEquals("next() did not return 'top' URI",
                curis[2].toString(),curi.toString());
        assertTrue("HQ should now be busy, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_BUSY);
        hq.close();
        
        hq = new AdaptiveRevisitHostQueue("bok.hi.is", env, catalog, 2);
        
        assertEquals("Size of HQ after reopening should now be 4",
                4, hq.getSize());
        assertTrue("HQ should be ready on reopen, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_READY);
        assertEquals("CrawlURI 'in processing' before should be top",
                curi.toString(), hq.peek().toString());
    
        /* Check if valence higher then 1 is properly handled.
         * 
         * Invoke next(), check if still ready and new top URI.
         */ 
        curi = hq.next(); // Should return curis[2]
        assertEquals("next() did not return 'top' URI",
                curis[2].toString(),curi.toString());
        assertTrue("HQ should still be ready, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_READY);
        
        /* Invoke next() again, check if now busy.
         */ 
        curi = hq.next(); // Should return curis[3]
        assertEquals("next() did not return 'top' URI",
                curis[3].toString(),curi.toString());
        assertTrue("HQ should be busy, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_BUSY);
        assertEquals("Size of HQ should still be 4",
                4, hq.getSize());

        
        /* Update() second URI issued. Confirm HQ is now ready again. URI is 
         * given same time of next processing to put it 'in front'. (no snooze)
         */ 
        hq.update(curi,false,0);
        assertTrue("HQ should now be ready, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_READY);
        assertEquals("'updated' CrawlURI before should be top",
                curi.toString(), hq.peek().toString());
        
        
        /* Update() again, ensure proper state. URI is NOT placed at front of 
         * queue and snooze time is given. But the HQ should not enter a 
         * snoozed state because the 'other' slot is free.
         */
        
        hq.update(curis[2],true,System.currentTimeMillis() + 1000000); // 10sec
        curis[3].putLong(A_TIME_OF_NEXT_PROCESSING,
                curis[1].getLong(A_TIME_OF_NEXT_PROCESSING) 
                        + 1000); // 1 sec. behind of current top 
        assertTrue("HQ should still be ready, is " + hq.getStateByName(),
                hq.getState()==AdaptiveRevisitHostQueue.HQSTATE_READY);
        assertEquals("Top CrawlURI before should be unchanged",
                curi.toString(), hq.peek().toString());
        

        // TODO: Test sorting with scheduling directives.
        
        /*
         * Close the ARHostQueue and the Environment
         */
        hq.close();
        catalog.close();
        env.close();
        cleanUpOldFiles("AR");
    }
    
}
