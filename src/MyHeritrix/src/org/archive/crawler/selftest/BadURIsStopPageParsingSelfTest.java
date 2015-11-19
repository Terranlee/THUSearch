/* BadURIsStopPageParsingSelfTest
 *
 * Created on Mar 10, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.selftest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Selftest for figuring problems parsing URIs in a page.
 * 
 * @author stack
 * @see <a 
 * href="https://sourceforge.net/tracker/?func=detail&aid=788219&group_id=73833&atid=539099">[ 788219 ]
 * URI Syntax Errors stop page parsing.</a>
 * @version $Revision: 4931 $, $Date: 2007-02-21 18:48:17 +0000 (Wed, 21 Feb 2007) $
 */
public class BadURIsStopPageParsingSelfTest extends SelfTestCase
{
    /**
     * Files to find as a list.
     * 
     * We don't find goodtwo.html because it has a BASE that is out
     * of scope.
     */
    private static final List<File> FILES_TO_FIND =
        Arrays.asList(new File[]
            {new File("goodone.html"),
                new File("goodthree.html"),
                new File("one.html"),
                new File("two.html"),
                new File("three.html")});

    public void stestFilesFound() {
        assertInitialized();
        List<File> foundFiles = filesFoundInArc();
        ArrayList<File> editedFoundFiles
         = new ArrayList<File>(foundFiles.size());
        for (Iterator i = foundFiles.iterator(); i.hasNext();) {
            File f = (File)i.next();
            if (f.getAbsolutePath().endsWith("polishex.html")) {
                // There is a URI in our list with the above as suffix.  Its in
                // the arc as a 404. Remove it.  It doesn't exist on disk so it
                // will cause the below testFilesInArc to fail.
                continue;
            }
            editedFoundFiles.add(f);
        }
        testFilesInArc(FILES_TO_FIND, editedFoundFiles);
    }
}
