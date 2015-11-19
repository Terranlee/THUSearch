/* CompositeIterator
*
* $Id: CompositeIterator.java 4644 2006-09-20 22:40:21Z paul_jack $
*
* Created on Mar 3, 2004
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
package org.archive.util.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that's built up out of any number of other iterators.
 * @author gojomo
 */
public class CompositeIterator implements Iterator {
    ArrayList<Iterator> iterators = new ArrayList<Iterator>();
    Iterator currentIterator;
    int indexOfCurrentIterator = -1;

    /**
     * Moves to the next (non empty) iterator. Returns false if there are no
     * more (non empty) iterators, true otherwise.
     * @return false if there are no more (non empty) iterators, true otherwise.
     */
    private boolean nextIterator() {
        if (++indexOfCurrentIterator < iterators.size()) {
            currentIterator = (Iterator) iterators.get(indexOfCurrentIterator);
            // If the new iterator was empty this will move us to the next one.
            return hasNext();
        } else {
            currentIterator = null;
            return false;
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if(currentIterator!=null && currentIterator.hasNext()) {
            // Got more
            return true;
        } else {
            // Have got more if we can queue up a new iterator.
            return nextIterator();
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if(hasNext()) {
            return currentIterator.next();
        } else {
            throw new NoSuchElementException();
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create an empty CompositeIterator. Internal
     * iterators may be added later.
     */
    public CompositeIterator() {
        super();
    }

    /**
     * Convenience method for concatenating together
     * two iterators.
     * @param i1
     * @param i2
     */
    public CompositeIterator(Iterator i1, Iterator i2) {
        this();
        add(i1);
        add(i2);
    }

    /**
     * Add an iterator to the internal chain.
     *
     * @param i an iterator to add.
     */
    public void add(Iterator i) {
        iterators.add(i);
    }

}
