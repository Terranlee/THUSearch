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
 * MemQueue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.queue;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.collections.Predicate;

/** An in-memory implementation of a {@link Queue}.
 *
 * @author Gordon Mohr
 *
 */
public class MemQueue<T> extends LinkedList<T> implements Queue<T> {

    private static final long serialVersionUID = -9077824759011044247L;

    /** Create a new, empty MemQueue
     */
    public MemQueue() {
        super();
    }

    /**
     * @see org.archive.queue.Queue#enqueue(Object)
     */
    public void enqueue(T o) {
        add(o);
    }

    /**
     * @see org.archive.queue.Queue#dequeue()
     */
    public T dequeue() {
        return removeFirst();
    }

    /**
     * @see org.archive.queue.Queue#length()
     */
    public long length() {
        return size();
    }

    /**
     * @see org.archive.queue.Queue#release()
     */
    public void release() {
        // nothing to release
    }

    /**
     * @see org.archive.queue.Queue#peek()
     */
    public T peek() {
        return getFirst();
    }


    /**
     * @see org.archive.queue.Queue#getIterator(boolean)
     */
    public Iterator<T> getIterator(boolean inCacheOnly) {
        return listIterator();
    }

    /**
     * @see org.archive.queue.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     */
    public long deleteMatchedItems(Predicate matcher) {
        Iterator<T> it = listIterator();
        long numberOfDeletes = 0;
        while(it.hasNext()){
            if(matcher.evaluate(it.next())){
                it.remove();
                numberOfDeletes++;
            }
        }
        return numberOfDeletes;
    }

    /* (non-Javadoc)
     * @see org.archive.queue.Queue#unpeek()
     */
    public void unpeek() {
        // nothing necessary
    }



}
