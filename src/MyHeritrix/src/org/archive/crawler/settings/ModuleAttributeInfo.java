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
 * ModuleAttributeInfo.java
 * Created on Dec 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.settings;

import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;

/**
 *
 * @author John Erik Halse
 */
public class ModuleAttributeInfo extends MBeanAttributeInfo {

    private static final long serialVersionUID = -4447321338690051514L;

    private String type;
    private boolean isOverrideable;
    private boolean isTransient;
    private final Object defaultValue;
    private final Object legalValueLists[];
    private boolean complexType = false;
    private boolean isExpertSetting;

    /** Construct a new instance of ModuleAttributeInfo.
     *
     * @param type the element to create info for.
     *
     * @throws InvalidAttributeValueException
     * @throws java.lang.IllegalArgumentException
     */
    public ModuleAttributeInfo(Type type)
            throws InvalidAttributeValueException {

        super(type.getName(), type.getClass().getName(), type.getDescription(),
                true, true, false);
        setType(type.getDefaultValue());
        this.isOverrideable = type.isOverrideable();
        this.isTransient = type.isTransient();
        this.legalValueLists = type.getLegalValues();
        this.isExpertSetting = type.isExpertSetting();
        //this.defaultValue = checkValue(type.getValue());
        this.defaultValue = type.getValue();
        if (type.getDefaultValue() instanceof ComplexType) {
            complexType = true;
        }
    }

    public ModuleAttributeInfo(ModuleAttributeInfo attr) {
        super(attr.getName(), attr.getType(), attr.getDescription(),
                true, true, false);
        setType(attr.getDefaultValue());
        this.isOverrideable = attr.isOverrideable();
        this.isTransient = attr.isTransient();
        this.legalValueLists = attr.getLegalValues();
        this.isExpertSetting = attr.isExpertSetting();
        this.defaultValue = attr.getDefaultValue();
        this.complexType = attr.complexType;
    }

    public Object[] getLegalValues() {
        return legalValueLists;
    }

    /** Returns true if this attribute refers to a ComplexType.
     *
     * @return true if this attribute refers to a ComplexType.
     */
    public boolean isComplexType() {
        return complexType;
    }

    /** Returns true if this attribute could be overridden in per settings.
     *
     * @return True if overrideable.
     */
    public boolean isOverrideable() {
        return isOverrideable;
    }

    /** Returns true if this attribute should be hidden from UI and not be
     * serialized to persistent storage.
     *
     * @return True if transient.
     */
    public boolean isTransient() {
        return isTransient;
    }

    /** Returns true if this Type should only show up in expert mode in UI.
     *
     * @return true if this Type should only show up in expert mode in UI.
     */
    public boolean isExpertSetting() {
        return isExpertSetting;
    }

    /**
     * @return Default value.
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanAttributeInfo#getType()
     */
    public String getType() {
        return type;
    }

    protected void setType(Object type) {
        this.type = type.getClass().getName();
    }
}
