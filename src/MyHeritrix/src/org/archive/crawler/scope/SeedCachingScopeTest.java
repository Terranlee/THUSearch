package org.archive.crawler.scope;

/* SeedCachingScopeTest
*
* $Id: SeedCachingScopeTest.java 4651 2006-09-25 18:31:13Z paul_jack $
*
* Created on Mar 30, 2005
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.TmpDirTestCase;


/**
* Test {@link SeedCachingScope}.
* @author stack gojomo
* @version $Revision: 4651 $, $Date: 2006-09-25 18:31:13 +0000 (Mon, 25 Sep 2006) $
*/
public class SeedCachingScopeTest extends TmpDirTestCase {
    /**
     * Constrained SeedCachingScope subclass for testing
     * 
     * @author gojomo
     */
    private class UnitTestSeedCachingScope extends SeedCachingScope {

        private static final long serialVersionUID = -1651873833038665447L;

        private File seedsfile;

        public UnitTestSeedCachingScope(File seedsfile) {
            super("test");
            this.seedsfile = seedsfile;
        }
        
        public File getSeedfile() {
            return seedsfile;
        } 
    }
    
   private static Set<UURI> seeds = null;

   /**
    * Comparator for treeset of uuris.
    */
   private static final Comparator<UURI> CMP = new Comparator<UURI> () {
       public int compare(UURI o1, UURI o2) {
           int result = -1;
           if (o1 == null && o1 == null){
               result = 0;
           } else if (o1 == null) {
               result = -1;
           } else if (o2 == null) {
               result = 1;
           } else {
               String s1 = o1.toString();
               String s2 = o2.toString();
               result = s1.compareTo(s2);
               result = (result < 0)? result = -1:
                   (result > 0)? result = 1: 0;
           }
           return result;
       }
   };


   /**
    * Seed file reference.
    */
   private File seedsfile;


   /* (non-Javadoc)
    * @see org.archive.util.TmpDirTestCase#setUp()
    */
   protected void setUp() throws Exception {
       super.setUp();

       // First create array of seeds and add to treeset.
       SeedCachingScopeTest.seeds = new TreeSet<UURI>(SeedCachingScopeTest.CMP);
       String [] uris = {"mailto:www.google.com",
           "http://www.port.com:80/etc/motd2",
           "http://a:b@userinfo.com/etc/motd2",
           "news:www.google.com",
           "http://www.google.com",
           "https://www.google.com",
           "gopher://www.google.com",
           "news://www.google.com",
           "rss://www.google.com",
           "telnet://www.google.com",
           "ftp://myname@example.com/etc/motd",
           "ftp://example.com/etc/motd2"
       };
       for (int i = 0; i < uris.length; i++) {
           SeedCachingScopeTest.seeds.add(UURIFactory.getInstance(uris[i]));
       }

       // Write a seeds file w/ our list of seeds.
       this.seedsfile = new File(getTmpDir(),
               SeedCachingScopeTest.class.getName() + ".seedfile");
       PrintWriter writer = new PrintWriter(new FileWriter(this.seedsfile));
       for (int i = 0; i < uris.length; i++) {
           writer.println(uris[i]);
       }
       writer.close();
   }


   /* (non-Javadoc)
    * @see org.archive.util.TmpDirTestCase#tearDown()
    */
   protected void tearDown() throws Exception {
       super.tearDown();
       if (this.seedsfile.exists()) {
            this.seedsfile.delete();
       }
   }

   public void testGeneral() throws URIException {
       // First make sure that I can get the seed set from seed file.
       SeedCachingScope sl = checkContent(SeedCachingScopeTest.seeds);
       // Now do add and see if get set matches seed file content.
       final CrawlURI curi = new CrawlURI(UURIFactory.getInstance("http://one.two.three"));
       sl.addSeed(curi);
       Set<UURI> set = new TreeSet<UURI>(SeedCachingScopeTest.CMP);
       set.addAll(SeedCachingScopeTest.seeds);
       set.add(curi.getUURI());
       checkContent(sl, set);
   }

   public void testNoScheme() throws IOException {
       final String NOSCHEME = "x.y.z";
       FileWriter fw = new FileWriter(this.seedsfile, true);
       // Write to new (last) line the URL.
       fw.write("\n");
       fw.write(NOSCHEME);
       fw.flush();
       fw.close();
       boolean found = false;
       SeedCachingScope sl = new UnitTestSeedCachingScope(seedsfile);
       for (Iterator i = sl.seedsIterator(); i.hasNext();) {
           UURI uuri = (UURI)i.next();
           if (uuri.getHost() == null) {
               continue;
           }
           if (uuri.getHost().equals(NOSCHEME)) {
               found = true;
               break;
           }
       }
       assertTrue("Did not find " + NOSCHEME, found);
   }

   private SeedCachingScope checkContent(Set seedSet) {
       return checkContent(null, seedSet);
   }

   private SeedCachingScope checkContent(SeedCachingScope sl, Set seedSet) {
       if (sl == null) {
           sl = new UnitTestSeedCachingScope(this.seedsfile);
       }
       int count = 0;
       for (Iterator i = sl.seedsIterator(); i.hasNext();) {
           count++;
           UURI uuri = (UURI)i.next();
           assertTrue("Does not contain: " + uuri.toString(),
               seedSet.contains(uuri));
       }
       assertTrue("Different sizes: " + count + ", " + seedSet.size(),
           count == seedSet.size());
       return sl;
   }
}

