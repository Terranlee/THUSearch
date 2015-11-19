/* IoUtils
 * 
 * Created on Dec 8, 2004
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
package org.archive.util;

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * I/O Utility methods.
 * @author stack
 * @version $Date: 2007-02-20 23:25:20 +0000 (Tue, 20 Feb 2007) $, $Revision: 4919 $
 */
public class IoUtils {
    protected static Logger logger =
        Logger.getLogger(IoUtils.class.getName());
    
    /**
     * @param file File to operate on.
     * @return Path suitable for use getting resources off the CLASSPATH
     * (CLASSPATH resources always use '/' as path separator, even on
     * windows).
     */
    public static String getClasspathPath(File file) {
        String path = file.getPath();
        if (File.separatorChar != '/') {
            // OK.  We're probably on a windows system. Strip
            // drive if its present and convert '\' to '/'.
            path = path.replace(File.separatorChar, '/');
            int index = path.indexOf(':');
            if (index > 0 && index < 3) {
                path = path.substring(index + 1);
            }
        }
        return path;
    }
    
    /**
     * Ensure writeable directory.
     *
     * If doesn't exist, we attempt creation.
     *
     * @param dir Directory to test for exitence and is writeable.
     *
     * @return The passed <code>dir</code>.
     *
     * @exception IOException If passed directory does not exist and is not
     * createable, or directory is not writeable or is not a directory.
     */
    public static File ensureWriteableDirectory(String dir)
    throws IOException {
        return ensureWriteableDirectory(new File(dir));
    }
    
    /**
     * Ensure writeable directories.
     *
     * If doesn't exist, we attempt creation.
     *
     * @param dirs List of Files to test.
     *
     * @return The passed <code>dirs</code>.
     *
     * @exception IOException If passed directory does not exist and is not
     * createable, or directory is not writeable or is not a directory.
     */
    public static List ensureWriteableDirectory(List<File> dirs)
    throws IOException {
        for (Iterator<File> i = dirs.iterator(); i.hasNext();) {
             ensureWriteableDirectory(i.next());
        }
        return dirs;
    }

    /**
     * Ensure writeable directory.
     *
     * If doesn't exist, we attempt creation.
     *
     * @param dir Directory to test for exitence and is writeable.
     *
     * @return The passed <code>dir</code>.
     *
     * @exception IOException If passed directory does not exist and is not
     * createable, or directory is not writeable or is not a directory.
     */
    public static File ensureWriteableDirectory(File dir)
    throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            if (!dir.canWrite()) {
                throw new IOException("Dir " + dir.getAbsolutePath() +
                    " not writeable.");
            } else if (!dir.isDirectory()) {
                throw new IOException("Dir " + dir.getAbsolutePath() +
                    " is not a directory.");
            }
        }

        return dir;
    }

    /**
     * Read the entire stream to EOF, returning what's read as a String.
     * 
     * @param inputStream
     * @return String of the whole inputStream's contents
     * @throws IOException
     */
    public static String readFullyAsString(InputStream inputStream)
    throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        while((c = inputStream.read()) > -1) {
            sb.append((char)c);
        }
        return sb.toString();
    }
    
    /**
     * Read the entire stream to EOF into the passed file.
     * @param is
     * @param toFile File to read into .
     * @throws IOException 
     * @throws IOException
     */
    public static void readFullyToFile(InputStream is,
            File toFile) throws IOException {
        readFullyToFile(is, toFile, new byte[4096]);
    }
    
    /**
     * Read the entire stream to EOF into the passed file.
     * Closes <code>is</code> when done or if an exception.
     * @param is Stream to read.
     * @param toFile File to read into .
     * @param buffer Buffer to use reading.
     * @return Count of bytes read.
     * @throws IOException
     */
    public static long readFullyToFile(final InputStream is, final File toFile,
            final byte [] buffer)
    throws IOException {
        long totalcount = -1;
        OutputStream os =
            new FastBufferedOutputStream(new FileOutputStream(toFile));
        InputStream localIs = (is instanceof BufferedInputStream)?
            is: new BufferedInputStream(is);
        try {
            for (int count = -1;
                (count = localIs.read(buffer, 0, buffer.length)) != -1;
                    totalcount += count) {
                os.write(buffer, 0, count);  
            }
        } finally {
            os.close();
            if (localIs != null) {
                localIs.close();
            }
        }
        return totalcount;
    }

    /**
     * Wrap generic Throwable as a checked IOException
     * @param e wrapped exception
     * @return IOException
     */
    public static IOException wrapAsIOException(Throwable e) {
        IOException ioe = new IOException(e.toString());
        ioe.initCause(e);
        return ioe;
    }
    
    
    public static void readFully(InputStream input, byte[] buf) 
    throws IOException {
        int max = buf.length;
        int ofs = 0;
        while (ofs < max) {
            int l = input.read(buf, ofs, max - ofs);
            if (l == 0) {
                throw new EOFException();
            }
            ofs += l;
        }
    }
    
    /**
     * Return the maximum number of bytes per character in the named
     * encoding, or 0 if encoding is invalid or unsupported. 
     *
     * @param encoding Encoding to consider.  For now, should be java 
     * canonical name for the encoding.
     *
     * @return True if multibyte encoding.
     */
    public static float encodingMaxBytesPerChar(String encoding) {
        boolean isMultibyte = false;
        final Charset cs;
        try {
            if (encoding != null && encoding.length() > 0) {
                cs = Charset.forName(encoding);
                if(cs.canEncode()) {
                    return cs.newEncoder().maxBytesPerChar();
                } else {
                    logger.info("Encoding not fully supported: " + encoding
                            + ".  Defaulting to single byte.");
                }
            }
        } catch (IllegalArgumentException e) {
            // Unsupported encoding
            logger.log(Level.INFO,"Illegal encoding name: " + encoding,e);
        }

        logger.fine("Encoding " + encoding + " is multibyte: "
            + ((isMultibyte) ? Boolean.TRUE : Boolean.FALSE));
        // default: return 0
        return 0;
    }

    /**
     * Utility method to serialize an object to the given File. 
     * 
     * @param object Object to serialize
     * @param file File to receive serialized copy
     * @throws IOException
     */
    public static void serializeToFile(Object object, File file) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        oos.writeObject(object);
        oos.close();
    }

    /**
     * Utility method to deserialize an Object from given File. 
     * 
     * @param file File source
     * @return deserialized Object
     * @throws IOException
     */
    public static Object deserializeFromFile(File file) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
        Object object;
        try {
            object = ois.readObject();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        ois.close();
        return object;
    }
    
    /**
     * Utility method to serialize Object to byte[]. 
     * 
     * @param object Object to be serialized
     * @return byte[] serialized form
     */
    public static byte[] serializeToByteArray(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
        } catch (IOException e) {
            // shouldn't be possible
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    /**
     * Utility method to deserialize Object from  byte[]. 
     * 
     * @param in byte[] source
     * @return Object deserialized
     */
    public static Object deserializeFromByteArray(byte[] in) {
        Object object;
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(in));
            try {
                object = ois.readObject();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
            ois.close();
        } catch (IOException e) {
            // shouldn't be possible
            throw new RuntimeException(e);
        }
        return object;
    }
}