/* DoubleList
 *
 * $Id: DoubleList.java 4648 2006-09-25 16:25:53Z paul_jack $
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

/** List of Double values
 *
 * @author John Erik Halse
 */
public class DoubleList extends ListType<Double> {

    private static final long serialVersionUID = -5793937164778552546L;

    /** Creates a new DoubleList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     */
    public DoubleList(String name, String description) {
        super(name, description);
    }

    /** Creates a new DoubleList and initializes it with the values from
     * another DoubleList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the list from which this lists gets its initial values.
     */
    public DoubleList(String name, String description, DoubleList l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new DoubleList and initializes it with the values from
     * an array of Doubles.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public DoubleList(String name, String description, Double[] l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new DoubleList and initializes it with the values from
     * an double array.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public DoubleList(String name, String description, double[] l) {
        super(name, description);
        addAll(l);
    }

    /** Add a new {@link java.lang.Double} at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, Double element) {
        super.add(index, element);
    }

    /** Add a new <code>double</code> at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, double element) {
        super.add(index, new Double(element));
    }

    /** Add a new {@link java.lang.Double} at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(Double element) {
        super.add(element);
    }

    /** Add a new double at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(double element) {
        super.add(new Double(element));
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
    public void addAll(DoubleList l) {
        super.addAll(l);
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(Double[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(double[] l) {
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
    public Double set(int index, Double element) {
        return (Double) super.set(index, element);
    }

    /** Check if element is of right type for this list.
     *
     * If this method gets a String, it tries to convert it to
     * the an Double before eventually throwing an exception.
     *
     * @param element element to check.
     * @return element of the right type.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public Double checkType(Object element) throws ClassCastException {
        if (element instanceof Double) {
            return (Double)element;
        } else {
            return (Double)
                SettingsHandler.StringToType(
                    (String) element,
                    SettingsHandler.DOUBLE);
        }
    }
}
