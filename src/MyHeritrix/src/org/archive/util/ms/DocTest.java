/* DocTest
*
* Created on September 12, 2006
*
* Copyright (C) 2006 Internet Archive.
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
package org.archive.util.ms;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.poi.hdf.extractor.WordDocument;

import junit.framework.TestCase;


public class DocTest extends TestCase {

    
    final private static File TEST_DIR = new File("testdata/ms");

    
    // Rename to testAgainstPOI to actually run the test.
    public void testAgainstPOI() throws IOException {
        int errors = 0;
        long start = System.currentTimeMillis();
        for (File f: TEST_DIR.listFiles()) try {
            start = System.currentTimeMillis();
            if (f.getName().endsWith(".doc")) {
                errors += runDoc(f);
            }
        } finally {
            long duration = System.currentTimeMillis() - start;
            System.out.println("Duration in milliseconds: " + duration);
        }
        if (errors > 0) {
            throw new IOException(errors + " errors, see stdout.");
        }
    }

    
    private int runDoc(File doc) throws IOException {
        System.out.println("===== Now processing " + doc.getName());
        String name = doc.getName();
        int p = name.lastIndexOf('.');
        String expectedName = name.substring(0, p) + ".txt";
        File expectedFile = new File(TEST_DIR, expectedName);
        if (!expectedFile.exists()) {
            createExpectedOutput(doc, expectedFile);
        }
        return runFiles(doc, expectedFile);
    }
    
    
    private void createExpectedOutput(File doc, File output) 
    throws IOException {
        FileInputStream finp = new FileInputStream(doc);
        FileOutputStream fout = new FileOutputStream(output);

        try {
            WordDocument wd = new WordDocument(finp);        
            Writer writer = new OutputStreamWriter(fout, "UTF-16BE");
            wd.writeAllText(writer);
        } finally {
            close(finp);
            close(fout);
        }
    }
    
    
    private static void close(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private int runFiles(File doc, File expected) 
    throws IOException {
        FileInputStream expectedIn = new FileInputStream(expected);
        Reader expectedReader = new InputStreamReader(expectedIn, "UTF-16BE");
        Reader docReader = Doc.getText(doc);
        try {
            return runReaders(docReader, expectedReader);
        } finally {
            close(docReader);
            close(expectedReader);
        }
    }
    
    
    private int runReaders(Reader doc, Reader expected) 
    throws IOException {
        int count = 0;
        int errors = 0;
        boolean go = true;
        while (go) {
            int ch = doc.read();
            int expectedCh = correctPOI(expected.read());
            if ((ch < 0) || (expectedCh < 0)) {
                go = false;
                if ((ch >= 0) || (expectedCh >= 0)) {
                    errors++;
                    System.out.println("File lengths differ.");
                }
            }
            if (ch != expectedCh) {
                errors += 1;
                report(count, expectedCh, ch);
            }
            count++;
        }
        return errors;
    }

    
    private void report(int count, int expected, int actual) {
        StringBuilder msg = new StringBuilder("#").append(count);
        msg.append(": Expected ");
        msg.append(expected).append(" (").append(toChar(expected));
        msg.append(") but got ").append(actual).append(" (");
        msg.append(toChar(actual)).append(").");
        System.out.println(msg);
    }


    private static String toChar(int ch) {
        if (ch < 0) {
            return "EOF";
        } else {
            return Character.toString((char)ch);
        }
    }
    
    /**
     * Corrects POI's Cp1252 output.  There's a bug somewhere in POI that
     * makes it produce incorrect characters.  Not sure where and don't have
     * time to track it down.  But I have visually checked the input 
     * documents to verify that Doc is producing the right character, and
     * that POI is not.
     * 
     * @param ch  the POI-produced character to check
     * @return    the corrected character
     */
    private static int correctPOI(int ch) {
        switch (ch) {
            case 8734:
                // POI produced the infinity sign when it should have 
                // produced the degrees sign.
                return 176;
            case 214:
                // POI produced an umat O instead of an ellipses mark.
                return 8230;
            case 237:
                // POI produced an acute i instead of a fancy single quote
                return 8217;
            case 236:
                // POI produced a reverse acute i instead of fancy double quote
                return 8220;
            case 238:
                // POI produced a caret i instead of fancy double quote
                return 8221;
            default:
                return ch;
        }
    }

    
}
