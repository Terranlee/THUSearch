/* SoftHashMap
 *
 * $Id: SoftSettingsHash.java 4665 2006-09-26 00:20:33Z paul_jack $
 *
 * Created on Mar 18, 2004
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
package org.archive.crawler.settings;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class SoftSettingsHash {

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private SettingsEntry[] table;

    /**
     * The number of key-value mappings contained in this hash.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * Reference queue for cleared entries
     */
    private final ReferenceQueue<? super String> queue 
     = new ReferenceQueue<String>();

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    private volatile int modCount;

    /**
     * Constructs a new, empty <tt>SoftSettingsHash</tt> with the given initial
     * capacity.
     *
     * @param  initialCapacity The initial capacity of the
     *         <tt>SoftSettingsHash</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative.
     */
    public SoftSettingsHash(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        table = new SettingsEntry[capacity];
        threshold = (int)(capacity * LOAD_FACTOR);
    }

    /**
     * Check for equality of non-null reference x and possibly-null y.  By
     * default uses Object.equals.
     */
    static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * Return index for hash code h.
     */
    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Expunge stale entries from the table.
     */
    private void expungeStaleEntries() {
        SettingsEntry entry;
        Reference ref;
        while ( (ref = queue.poll()) != null) {
            entry = (SettingsEntry)ref;
            int h = entry.hash;
            int i = indexFor(h, table.length);

            SettingsEntry prev = table[i];
            SettingsEntry p = prev;
            while (p != null) {
                SettingsEntry next = p.next;
                if (p == entry) {
                    if (prev == entry)
                        table[i] = next;
                    else
                        prev.next = next;
                    entry.next = null;  // Help GC
                    entry.settings = null; //  "   "
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns the value to which the specified key is mapped in this weak
     * hash map, or <tt>null</tt> if the map contains no mapping for
     * this key. Null is also returned if the element has been GC'ed.
     *
     * @param   key the key whose associated settings object is to be returned.
     * @return  the settings object represented by the key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(String, CrawlerSettings)
     */
    public CrawlerSettings get(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        int hash = hash(key);
        expungeStaleEntries();
        int index = indexFor(hash, table.length);
        SettingsEntry e = table[index];
        while (e != null) {
            if (e.hash == hash && eq(key, e.get()))
                return e.settings;
            e = e.next;
        }
        return null;
    }

    /**
     * Associates the specified settings object with the specified key in this
     * hash.
     *
     * If the hash previously contained a settings object for this key, the old
     * object is replaced.
     *
     * @param key key with which the specified settings object is to be
     *            associated.
     * @param settings settings object to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.
     */
    public CrawlerSettings put(String key, CrawlerSettings settings) {
        if (settings == null) {
            throw new NullPointerException("Settings object was null");
        }
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        int hash = hash(key);
        expungeStaleEntries();
        int i = indexFor(hash, table.length);

        for (SettingsEntry entry = table[i]; entry != null; entry = entry.next) {
            if (hash == entry.hash && eq(key, entry.get())) {
                CrawlerSettings oldValue = entry.settings;
                if (settings != oldValue)
                    entry.settings = settings;
                return oldValue;
            }
        }

        modCount++;
        table[i] = new SettingsEntry(key, settings, queue, hash, table[i]);
        if (++size >= threshold)
            resize(table.length * 2);
        return null;
    }

    public CrawlerSettings put(SettingsEntry entry) {
        return put(entry.getKey(), entry.getValue());
    }

    /**
     * Rehashes the contents of this hash into a new <tt>HashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     *
     * Note that this method is a no-op if it's called with newCapacity ==
     * 2*MAXIMUM_CAPACITY (which is Integer.MIN_VALUE).
     *
     * @param newCapacity the new capacity, MUST be a power of two.
     */
    void resize(int newCapacity) {
        expungeStaleEntries();
        SettingsEntry[] oldTable = table;
        int oldCapacity = oldTable.length;

        // check if needed
        if (size < threshold || oldCapacity > newCapacity)
            return;

        SettingsEntry[] newTable = new SettingsEntry[newCapacity];

        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * LOAD_FACTOR);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /** Transfer all entries from src to dest tables */
    private void transfer(SettingsEntry[] src, SettingsEntry[] dest) {
        for (int j = 0; j < src.length; ++j) {
            SettingsEntry entry = src[j];
            src[j] = null;
            while (entry != null) {
                SettingsEntry next = entry.next;
                Object key = entry.get();
                if (key == null) {
                    entry.next = null;  // Help GC
                    entry.settings = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(entry.hash, dest.length);
                    entry.next = dest[i];
                    dest[i] = entry;
                }
                entry = next;
            }
        }
    }

    /**
     * Removes the settings object identified by the key from this hash if
     * present.
     *
     * @param key key whose element is to be removed from the hash.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.
     */
    public Object remove(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        int hash = hash(key);
        expungeStaleEntries();
        int i = indexFor(hash, table.length);
        SettingsEntry prev = table[i];
        SettingsEntry entry = prev;

        while (entry != null) {
            SettingsEntry next = entry.next;
            if (hash == entry.hash && eq(key, entry.get())) {
                modCount++;
                size--;
                if (prev == entry)
                    table[i] = next;
                else
                    prev.next = next;
                return entry.settings;
            }
            prev = entry;
            entry = next;
        }

        return null;
    }

    /**
     * Removes all settings object from this hash.
     */
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        modCount++;
        SettingsEntry tab[] = table;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
   }

    /**
     * The entries in this hash extend SoftReference, using the host string
     * as the key.
     */
    static class SettingsEntry extends SoftReference<String> {
        private CrawlerSettings settings;
        private final int hash;
        private SettingsEntry next;

        /**
         * Create new entry.
         */
        SettingsEntry(String key, CrawlerSettings settings, 
              ReferenceQueue<? super String> queue,
              int hash, SettingsEntry next) {
            super(key, queue);
            this.settings = settings;
            this.hash  = hash;
            this.next  = next;
        }

        public String getKey() {
            return (String) this.get();
        }

        public CrawlerSettings getValue() {
            return settings;
        }

        public boolean equals(Object o) {
            if (!(o instanceof SettingsEntry))
                return false;
            SettingsEntry e = (SettingsEntry)o;
            String key1 = getKey();
            String key2 = e.getKey();
            if (key1 == key2 || (key1 != null && key1.equals(key2))) {
                CrawlerSettings setting1 = getValue();
                CrawlerSettings setting2 = e.getValue();
                if (setting1 == setting2 || (setting1 != null && setting1.equals(setting2)))
                    return true;
            }
            return false;
        }
    }

    /** Iterator over all elements in hash.
     */
    class EntryIterator implements Iterator {
        int index;
        SettingsEntry entry = null;
        SettingsEntry lastReturned = null;
        int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        String nextKey = null;

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        String currentKey = null;

        EntryIterator() {
            index = (size() != 0 ? table.length : 0);
        }

        public boolean hasNext() {
            SettingsEntry[] t = table;

            while (nextKey == null) {
                SettingsEntry e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = (String) e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        /** The common parts of next() across different types of iterators */
        public Object next() {
            return nextEntry();
        }

        public SettingsEntry nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            SoftSettingsHash.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    /** Make hash value from a String.
     *
     * @param key the string for which to create hash value.
     * @return the hash value.
     */
    static int hash(String key) {
        int hash = key.hashCode();

        hash += ~(hash << 9);
        hash ^=  (hash >>> 14);
        hash +=  (hash << 4);
        hash ^=  (hash >>> 10);
        return hash;
    }

    public EntryIterator iterator() {
        return new EntryIterator();
    }

}
