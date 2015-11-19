/* FloatList
 *
 * $Id: FloatList.java 4648 2006-09-25 16:25:53Z paul_jack $
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

/** List of Float values
 *
 * @author John Erik Halse
 */
public class FloatList extends ListType<Float> {

    private static final long serialVersionUID = -8836233200837205447L;

    /** Creates a new FloatList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     */
    public FloatList(String name, String description) {
        super(name, description);
    }

    /** Creates a new FloatList and initializes it with the values from
     * another FloatList.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the list from which this lists gets its initial values.
     */
    public FloatList(String name, String description, FloatList l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new FloatList and initializes it with the values from
     * an array of Floats.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public FloatList(String name, String description, Float[] l) {
        super(name, description);
        addAll(l);
    }

    /** Creates a new FloatList and initializes it with the values from
     * an float array.
     *
     * @param name of the list.
     * @param description of the list. This string should be suitable for using
     *        in a user interface.
     * @param l the array from which this lists gets its initial values.
     */
    public FloatList(String name, String description, float[] l) {
        super(name, description);
        addAll(l);
    }

    /** Add a new {@link java.lang.Float} at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, Float element) {
        super.add(index, element);
    }

    /** Add a new <code>float</code> at the specified index to this list.
     *
     * @param index index at which the specified element is to be inserted.
     * @param element the value to be added.
     */
    public void add(int index, float element) {
        super.add(index, new Float(element));
    }

    /** Add a new {@link java.lang.Float} at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(Float element) {
        super.add(element);
    }

    /** Add a new float at the end of this list.
     *
     * @param element the value to be added.
     */
    public void add(float element) {
        super.add(new Float(element));
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
    public void addAll(FloatList l) {
        super.addAll(l);
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(Float[] l) {
        for (int i = 0; i < l.length; i++) {
            add(l[i]);
        }
    }

    /** Appends all of the elements in the specified array to the end of this
     * list, in the same order that they are in the array.
     *
     * @param l array whose elements are to be added to this list.
     */
    public void addAll(float[] l) {
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
    public Float set(int index, Float element) {
        return (Float) super.set(index, element);
    }

    /** Check if element is of right type for this list.
     *
     * If this method gets a String, it tries to convert it to
     * the an Float before eventually throwing an exception.
     *
     * @param element element to check.
     * @return element of the right type.
     * @throws ClassCastException is thrown if the element was of wrong type
     *         and could not be converted.
     */
    public Float checkType(Object element) throws ClassCastException {
        if (element instanceof Float) {
            return (Float)element;
        } else {
            return (Float)
                SettingsHandler.StringToType(
                    (String) element,
                    SettingsHandler.FLOAT);
        }
    }
}
