/* ObjectPlusFilesInputStream
*
* $Id: ObjectPlusFilesInputStream.java 4646 2006-09-22 17:23:04Z paul_jack $
*
* Created on Apr 28, 2004
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
package org.archive.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.archive.util.FileUtils;


/**
 * Enhanced ObjectOutputStream with support for restoring
 * files that had been saved, in parallel with object
 * serialization.
 *
 * @author gojomo
 *
 */
public class ObjectPlusFilesInputStream extends ObjectInputStream {
    LinkedList<File> auxiliaryDirectoryStack = new LinkedList<File>();
    LinkedList<Runnable> postRestoreTasks = new LinkedList<Runnable>();

    /**
     * Instantiate over the given stream and using the supplied
     * auxiliary storage directory.
     *
     * @param in
     * @param storeDir
     * @throws IOException
     */
    public ObjectPlusFilesInputStream(InputStream in, File storeDir)
    throws IOException {
        super(in);
        auxiliaryDirectoryStack.addFirst(storeDir);
    }

    /**
     * Push another default storage directory for use
     * until popped.
     *
     * @param dir
     */
    public void pushAuxiliaryDirectory(String dir) {
        auxiliaryDirectoryStack.
            addFirst(new File(getAuxiliaryDirectory(), dir));
    }

    /**
     * Discard the top auxiliary directory.
     */
    public void popAuxiliaryDirectory() {
        auxiliaryDirectoryStack.removeFirst();
    }

    /**
     * Return the top auxiliary directory, from
     * which saved files are restored.
     *
     * @return Auxillary directory.
     */
    public File getAuxiliaryDirectory() {
        return (File)auxiliaryDirectoryStack.getFirst();
    }

    /**
     * Restore a file from storage, using the name and length
     * info on the serialization stream and the file from the
     * current auxiliary directory, to the given File.
     *
     * @param destination
     * @throws IOException
     */
    public void restoreFile(File destination) throws IOException {
        String nameAsStored = readUTF();
        long lengthAtStoreTime = readLong();
        File storedFile = new File(getAuxiliaryDirectory(),nameAsStored);
        FileUtils.copyFile(storedFile, destination, lengthAtStoreTime);
    }

    /**
     * Restore a file from storage, using the name and length
     * info on the serialization stream and the file from the
     * current auxiliary directory, to the given File.
     *
     * @param directory
     * @throws IOException
     */
    public void restoreFileTo(File directory) throws IOException {
        String nameAsStored = readUTF();
        long lengthAtStoreTime = readLong();
        File storedFile = new File(getAuxiliaryDirectory(),nameAsStored);
        File destination = new File(directory,nameAsStored);
        FileUtils.copyFile(storedFile, destination, lengthAtStoreTime);
    }

    /**
     * Register a task to be done when the ObjectPlusFilesInputStream
     * is closed.
     *
     * @param task
     */
    public void registerFinishTask(Runnable task) {
        postRestoreTasks.addFirst(task);
    }

    private void doFinishTasks() {
    	Iterator iter = postRestoreTasks.iterator();
    	while(iter.hasNext()) {
    		((Runnable)iter.next()).run();
        }
    }

    /**
     * In addition to default, do any registered cleanup tasks.
     *
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException {
        super.close();
        doFinishTasks();
    }
}
