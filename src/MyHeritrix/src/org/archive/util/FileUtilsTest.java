/* FileUtilsTest
 * 
 * Created on Apr 7, 2005
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
import java.io.IOException;


/**
 * @author stack
 * @version $Date: 2006-09-20 22:40:21 +0000 (Wed, 20 Sep 2006) $, $Revision: 4644 $
 */
public class FileUtilsTest extends TmpDirTestCase {
    private String srcDirName = FileUtilsTest.class.getName() + ".srcdir";
    private File srcDirFile = null;
    private String tgtDirName = FileUtilsTest.class.getName() + ".tgtdir";
    private File tgtDirFile = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        this.srcDirFile = new File(getTmpDir(), srcDirName);
        this.srcDirFile.mkdirs();
        this.tgtDirFile = new File(getTmpDir(), tgtDirName);
        this.tgtDirFile.mkdirs();
        addFiles();
    }
 
    private void addFiles() throws IOException {
        addFiles(3, this.getName());
    }
    
    private void addFiles(final int howMany, final String baseName)
    throws IOException {
        for (int i = 0; i < howMany; i++) {
            File.createTempFile(baseName, null, this.srcDirFile);
        }
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDir(this.srcDirFile);
        FileUtils.deleteDir(this.tgtDirFile);
    }

    public void testCopyFiles() throws IOException {
        FileUtils.copyFiles(this.srcDirFile, this.tgtDirFile);
        File [] srcFiles = this.srcDirFile.listFiles();
        for (int i = 0; i < srcFiles.length; i++) {
            File tgt = new File(this.tgtDirFile, srcFiles[i].getName());
            assertTrue("Tgt doesn't exist " + tgt.getAbsolutePath(),
                tgt.exists());
        }
    }
    
    public void testCopyFile() {
        // Test exception copying nonexistent file.
        File [] srcFiles = this.srcDirFile.listFiles();
        srcFiles[0].delete();
        IOException e = null;
        try {
        FileUtils.copyFile(srcFiles[0],
            new File(this.tgtDirFile, srcFiles[0].getName()));
        } catch (IOException ioe) {
            e = ioe;
        }
        assertNotNull("Didn't get expected IOE", e);
    }
    
    public void testSyncDirectories() throws IOException {
        FileUtils.syncDirectories(this.srcDirFile, null, this.tgtDirFile);
        addFiles(1, "xxxxxx");
        FileUtils.syncDirectories(this.srcDirFile, null, this.tgtDirFile);
        assertEquals("Not equal", this.srcDirFile.list().length,
            this.tgtDirFile.list().length);
    }
}
