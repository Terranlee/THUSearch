/* LongList
 *
 * $Id: LongList.java 4648 2006-09-25 16:25:53Z paul_jack $
 * Created on Dec 18, 2003
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
 */
package org.archive.crawler.settings;

/** List of Long values
 *
 * @author John Erik Halse
 */
public class LongList extends ListType<Long> {

    private static final long serialVersionUID = -7542494945185808903L;

    /** Creates a new LongList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     */
    public LongList(String name, String description) {
        super(name, description);
    }

    /** Creates a new LongList and initializes it with the values from
     * another LongList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the list from which this lists gets its initial values.
     */
    public LongList(String name, String description, LongList l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new LongList and initializes it with the values from
     * an array of {@link java.lang.Long}.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public LongList(String name, String description, Long[] l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new LongList and initializes it with the values from
     * an array of long.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public LongList(String name, String description, long[] l) {
        super(name, description);
        addAll(l);
    }

    /** Add a new {@link java.lang.Long} at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, Long element) {
        super.add(index, element);
    }

    /** Add a new <code>long</code> at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, long element) {
        super.add(index, new Long(element));
    }

    /** Add a new {@link java.lang.Long} at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(Long element) {
        super.add(element);
    }

    /** Add a new long at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(long element) {
        super.add(new Long(element));
    }

    /** Appends all of the elements in the specified list to the end of this
     * list, in the order that they are returned by the specified lists's
     * iterator.
     *
     * The behavior of this operation is unspecified if the specified
     * collection is modified while the operation is in progress.
     *
     * @param l list whose elements are to be added to this list.
     */
    public void addAll(LongList l) {
        super.addAll(l);
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(Long[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(long[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    /** Replaces the element at the specified position in this list with the
     *  specified element.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @return the element previously at the specified position.
     */
    public Long set(int index, Long element) {
        return (Long) super.set(index, element);
    }

    /** Check if element is of right type for this list.
     *
     * If this method gets a String, it tries to convert it to
     * the an Long before eventually throwing an exception.
     *
     * @param element element to check.
     * @return element of the right type.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public Long checkType(Object element) throws ClassCastException {
        if (element instanceof Long) {
            return (Long)element;
        } else {
            return (Long)
                SettingsHandler.StringToType(
                    (String) element,
                    SettingsHandler.LONG);
        }
    }
}
