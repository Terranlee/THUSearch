/* IoUtils
 * 
 * $Id: IoUtils.java 3716 2005-07-20 22:21:59Z stack-sf $
 *
 * Created on Jun 9, 2005
 * 
 * Copyright (C) 2003 Internet Archive.
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
 *
 */
package org.archive.crawler.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Expand;
import org.archive.net.UURI;

/**
 * Logging utils.
 * @author stack
 */
public class IoUtils {
    public static InputStream getInputStream(String pathOrUrl) {
        return getInputStream(null, pathOrUrl);
    }
    
    /**
     * Get inputstream.
     * 
     * This method looks at passed string and tries to judge it a
     * filesystem path or an URL.  It then gets an InputStream on to
     * the file or URL.
     * 
     * <p>ASSUMPTION: Scheme on any url will probably only ever be 'file' 
     * or 'http'.
     * 
     * @param basedir If passed <code>fileOrUrl</code> is a file path and
     * it is not absolute, prefix with this basedir (May be null then
     * no prefixing will be done).
     * @param pathOrUrl Pass path to a file on disk or pass in a URL.
     * @return An input stream.
     */
    public static InputStream getInputStream(File basedir, String pathOrUrl) {
        InputStream is = null;
        if (UURI.hasScheme(pathOrUrl)) {
            try {
                URL url = new URL(pathOrUrl);
                is = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Assume its not an URI or we failed the parse.
            // Try it as a file.
            File source = new File(pathOrUrl);
            if (!source.isAbsolute() && basedir != null) {
                source = new File(basedir, pathOrUrl);
            }
            try {
                is = new FileInputStream(source);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return is;
    }
    
    /**
     * Use ant to unjar.
     * @param zipFile File to unzip.
     * @param destinationDir Where to unzip to.
     */
    public static void unzip(File zipFile, File destinationDir) {
        unzip(zipFile, destinationDir, true);
    }
     
    /**
     * Use ant to unjar.
     * @param zipFile File to unzip.
     * @param destinationDir Where to unzip to.
     * @param overwrite Whether to overwrite existing content.
     */
    public static void unzip(File zipFile, File destinationDir,
            boolean overwrite) {
        final class Expander extends Expand {
                public Expander() {
            }   
        }
        Expander expander = new Expander();
        expander.setProject(new Project());
        expander.getProject().init();
        expander.setTaskType("unzip");
        expander.setTaskName("unzip");
        expander.setOwningTarget(new Target());
        expander.setSrc(zipFile);
        expander.setDest(destinationDir);
        expander.setOverwrite(overwrite);
        expander.execute();
    }
}
