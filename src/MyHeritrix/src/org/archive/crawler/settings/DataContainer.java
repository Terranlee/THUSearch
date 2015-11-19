/* DataContainer
 *
 * $Id: DataContainer.java 4661 2006-09-25 23:11:16Z paul_jack $
 *
 * Created on Dec 17, 2003
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
 *
 */
package org.archive.crawler.settings;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

import java.util.concurrent.CopyOnWriteArrayList;

/** This class holds the data for a ComplexType for a settings object.
 *
 * @author John Erik Halse
 */
public class DataContainer extends HashMap<String,Object> {

    private static final long serialVersionUID = 2089160108643429282L;

    /** The ComplexType for which this DataContainer keeps data */
    private ComplexType complexType;

    /** The Settings object for which this data is valid */
    private Reference<CrawlerSettings> settings;

    /** The attributes defined for this DataContainers combination of
     * ComplexType and CrawlerSettings.
     */
    private List<MBeanAttributeInfo> attributes;

    /** All attributes that have their value set for this DataContainers
     * combination of ComplexType and CrawlerSettings. This includes overrides.
     */
    private Map<String,MBeanAttributeInfo> attributeNames;

    /** Create a data container for a module.
     *
     * @param settings Settings to use.
     * @param module the module to create the data container for.
     */
    public DataContainer(CrawlerSettings settings, ComplexType module) {
        super();
        this.settings = new WeakReference<CrawlerSettings>(settings);
        this.complexType = module;
        attributes =
            new CopyOnWriteArrayList<MBeanAttributeInfo>();
        attributeNames = new HashMap<String,MBeanAttributeInfo>();
    }

    /** Add a new element to the data container.
     *
     * @param type the element to add.
     * @param index index at which the specified element is to be inserted.
     * @throws InvalidAttributeValueException
     */
    public void addElementType(Type type, int index)
            throws InvalidAttributeValueException {

        if (attributeNames.containsKey(type.getName())) {
            throw new IllegalArgumentException(
                    "Duplicate field: " + type.getName());
        }
        if (type.getDefaultValue() == null) {
            throw new InvalidAttributeValueException(
                    "null is not allowed as default value for attribute '"
                            + type.getName() + "' in class '"
                            + complexType.getClass().getName() + "'");
        }
        MBeanAttributeInfo attribute = new ModuleAttributeInfo(type);
        attributes.add(index, attribute);
        //attributeNames.put(type.getName(), attribute);
        try {
            put(type.getName(), attribute, type.getDefaultValue());
        } catch (InvalidAttributeValueException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Appends the specified element to the end of this data container.
     *
     * @param type the element to add.
     * @throws InvalidAttributeValueException
     */
    public void addElementType(Type type) throws InvalidAttributeValueException {

        addElementType(type, attributes.size());
    }

    public MBeanInfo getMBeanInfo() {
        MBeanAttributeInfo attrs[] = (MBeanAttributeInfo[]) attributes
                .toArray(new MBeanAttributeInfo[0]);
        MBeanInfo info = new MBeanInfo(complexType.getClass().getName(),
                complexType.getDescription(), attrs, null, null, null);
        return info;
    }

    protected List<MBeanAttributeInfo> getLocalAttributeInfoList() {
        return attributes;
    }

    protected boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    public int size() {
        return attributes.size();
    }

    protected MBeanAttributeInfo getAttributeInfo(String name) {
        return (MBeanAttributeInfo) attributeNames.get(name);
    }

    protected void copyAttributeInfo(String name, DataContainer destination) {
        if (this != destination) {
            ModuleAttributeInfo attribute = (ModuleAttributeInfo) attributeNames.get(name);
            destination.attributeNames.put(name, new ModuleAttributeInfo(attribute));
        }
    }

    protected boolean copyAttribute(String name, DataContainer destination)
            throws InvalidAttributeValueException, AttributeNotFoundException {
        if (this != destination) {
            ModuleAttributeInfo attribute = (ModuleAttributeInfo) attributeNames
                    .get(name);

            if (attribute == null) {
                return false;
            } else {
                int index = attributes.indexOf(attribute);
                if (index != -1 && !destination.attributes.contains(attribute)) {
                    destination.attributes.add(index, attribute);
                }
                destination.put(attribute.getName(), attribute, get(attribute
                        .getName()));
            }
        }
        return true;
    }

    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public Object get(Object key) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    protected Object put(String key, MBeanAttributeInfo info, Object value)
        throws InvalidAttributeValueException, AttributeNotFoundException {
        attributeNames.put(key, info);
        return super.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(String key) throws AttributeNotFoundException {
        Object res = super.get(key);
        if (res == null && complexType.definitionMap.get(key) == null) {
            throw new AttributeNotFoundException(key);
        }
        return res;
    }

    /** Move an attribute up one place in the list.
     *
     * @param key name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at the top.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted key.
     */
    protected boolean moveElementUp(String key)
            throws AttributeNotFoundException {
        MBeanAttributeInfo element = getAttributeInfo(key);
        if (element == null) {
            throw new AttributeNotFoundException(key);
        }

        int prevIndex = attributes.indexOf(element);
        if (prevIndex == 0) {
            return false;
        }

        attributes.remove(prevIndex);
        attributes.add(prevIndex-1, element);

        return true;
    }

    /** Move an attribute down one place in the list.
     *
     * @param key name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at bottom.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted key.
     */
    protected boolean moveElementDown(String key)
            throws AttributeNotFoundException {
        MBeanAttributeInfo element = getAttributeInfo(key);
        if (element == null) { throw new AttributeNotFoundException(key); }

        int prevIndex = attributes.indexOf(element);
        if (prevIndex == attributes.size() - 1) { return false; }

        attributes.remove(prevIndex);
        attributes.add(prevIndex + 1, element);

        return true;
    }

    /**
     * Remove an attribute from the DataContainer.
     *
     * @param key name of the attribute to remove.
     * @return the element that was removed.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *             with the submitted key.
     */
    protected Object removeElement(String key) throws AttributeNotFoundException {
        MBeanAttributeInfo element = getAttributeInfo(key);
        if (element == null) {
            throw new AttributeNotFoundException(key);
        }

        attributes.remove(element);
        attributeNames.remove(element.getName());
        return super.remove(element.getName());
    }

    /** Get the ComplexType for which this DataContainer keeps data.
     *
     * @return the ComplexType for which this DataContainer keeps data.
     */
    protected ComplexType getComplexType() {
        return complexType;
    }

    /** Get the settings object for which this DataContainers data are valid.
     *
     * @return the settings object for which this DataContainers data are valid.
     */
    protected CrawlerSettings getSettings() {
        return (CrawlerSettings) settings.get();
    }

}
