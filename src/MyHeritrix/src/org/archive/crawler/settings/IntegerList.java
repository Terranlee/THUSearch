/* IntegerList
 *
 * $Id: IntegerList.java 4648 2006-09-25 16:25:53Z paul_jack $
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

/** List of Integer values
 *
 * @author John Erik Halse
 */
public class IntegerList extends ListType<Integer> {

    private static final long serialVersionUID = -637584927948877976L;

    /** Creates a new IntegerList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     */
    public IntegerList(String name, String description) {
        super(name, description);
    }

    /** Creates a new IntegerList and initializes it with the values from
     * another IntegerList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the list from which this lists gets its initial values.
     */
    public IntegerList(String name, String description, IntegerList l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new IntegerList and initializes it with the values from
     * an array of Integers.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public IntegerList(String name, String description, Integer[] l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new IntegerList and initializes it with the values from
     * an int array.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public IntegerList(String name, String description, int[] l) {
        super(name, description);
        addAll(l);
    }

    /** Add a new {@link java.lang.Integer} at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, Integer element) {
        super.add(index, element);
    }

    /** Add a new <code>int</code> at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, int element) {
        super.add(index, new Integer(element));
    }

    /** Add a new {@link java.lang.Integer} at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(Integer element) {
        super.add(element);
    }

    /** Add a new int at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(int element) {
        super.add(new Integer(element));
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
    public void addAll(IntegerList l) {
        super.addAll(l);
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(Integer[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(int[] l) {
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
    public Integer set(int index, Integer element) {
        return (Integer) super.set(index, element);
    }

    /** Check if element is of right type for this list.
     *
     * If this method gets a String, it tries to convert it to
     * the an Integer before eventually throwing an exception.
     *
     * @param element element to check.
     * @return element of the right type.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public Integer checkType(Object element) throws ClassCastException {
        if (element instanceof Integer) {
            return (Integer)element;
        } else {
            return (Integer)
                SettingsHandler.StringToType(
                    (String) element,
                    SettingsHandler.INTEGER);
        }
    }
}
