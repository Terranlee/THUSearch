/* ARCReaderFactoryTest.java
 *
 * $Id: ARCReaderFactoryTest.java 4512 2006-08-19 00:22:10Z stack-sf $
 *
 * Created Jul 15, 2005
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
package org.archive.io.arc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.archive.util.TmpDirTestCase;

public class ARCReaderFactoryTest extends TmpDirTestCase {
//    public void testGetHttpURL() throws MalformedURLException, IOException {
//        ARCReader reader = null;
//        try {
//            // TODO: I can get a single ARCRecord but trying to iterate from
//            // a certain point is getting an EOR when I go to read GZIP header.
//            reader = ARCReaderFactory.
//                get(new URL("http://localhost/test.arc.gz"), 0);
//            for (final Iterator i = reader.iterator(); i.hasNext();) {
//                ARCRecord ar = (ARCRecord)i.next();
//                System.out.println(ar.getMetaData().getUrl());
//            }
//        } finally {
//            if (reader != null) {
//                reader.close();
//            }
//        }
//    }
    
    /**
     * Test File URL.
     * If a file url, we just use the pointed to file.  There is no
     * copying down to a file in tmp that gets cleaned up after close.
     * @throws MalformedURLException
     * @throws IOException
     */
    public void testGetFileURL() throws MalformedURLException, IOException {
        File arc = ARCWriterTest.createARCFile(getTmpDir(), true);
        doGetFileUrl(arc);
    }
    
    protected void doGetFileUrl(File arc)
    throws MalformedURLException, IOException {
        ARCReader reader = null;
        File tmpFile = null;
        try {
            reader = ARCReaderFactory.
                get(new URL("file:////" + arc.getAbsolutePath()));
            tmpFile = null;
            for (Iterator i = reader.iterator(); i.hasNext();) {
                ARCRecord r = (ARCRecord)i.next();
                if (tmpFile == null) {
                    tmpFile = new File(r.getMetaData().getArc());
                }
            }
            assertTrue(tmpFile.exists());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        assertTrue(tmpFile.exists());
    }
    
    /**
     * Test path or url.
     * @throws MalformedURLException 
     * @throws IOException 
     */
    public void testGetPathOrURL() throws MalformedURLException, IOException {
        File arc = ARCWriterTest.createARCFile(getTmpDir(), true);
        ARCReader reader = ARCReaderFactory.get(arc.getAbsoluteFile());
        assertNotNull(reader);
        reader.close();
        doGetFileUrl(arc);
    }   
}
