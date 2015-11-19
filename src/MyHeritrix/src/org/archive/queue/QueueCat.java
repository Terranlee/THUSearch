/* Copyright (C) 2003 Internet Archive.
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
 * QueueCat.java
 * Created on Nov 12, 2003
 *
 * $Header$
 */
package org.archive.queue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.SequenceInputStream;

import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

/**
 * Command-line tool that displays serialized object streams in a
 * line-oriented format.
 *
 * @author gojomo
 */
public class QueueCat {
    public static void main(String[] args) 
    throws IOException, ClassNotFoundException {
        InputStream inStream;
        if (args.length == 0) {
            inStream = System.in;
        } else {
            inStream = new FileInputStream(args[0]);
        }

        // Need to handle the case where the stream lacks the usual
        // objectstream prefix
        byte[] serialStart = { (byte)0xac, (byte)0xed, (byte)0x00, (byte)0x05 };
        byte[] actualStart = new byte[4];
        byte[] pseudoStart;
        inStream.read(actualStart);
        if (ArchiveUtils.byteArrayEquals(serialStart,actualStart)) {
            pseudoStart = serialStart;
        } else {
            // Have to fake serialStart and original 4 bytes
            pseudoStart = new byte[8];
            System.arraycopy(serialStart,0,pseudoStart,0,4);
            System.arraycopy(actualStart,0,pseudoStart,4,4);
        }
        inStream = new SequenceInputStream(
            new ByteArrayInputStream(pseudoStart),
            inStream);

        ObjectInputStream oin = new ObjectInputStream(inStream);

        Object o;
        while(true) {
            try {
                o=oin.readObject();
            } catch (EOFException e) {
                return;
            }
            if(o instanceof Reporter) {
                System.out.println(((Reporter)o).singleLineReport());
            } else {
                // TODO: flatten multiple-line strings!
                System.out.println(o.toString());
            }
        }
    }
}
