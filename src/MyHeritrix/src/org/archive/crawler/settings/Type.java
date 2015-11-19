/* Type
 *
 * $Id: Type.java 4661 2006-09-25 23:11:16Z paul_jack $
 *
 * Created on Jan 8, 2004
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.Attribute;

/**
 * Interface implemented by all element types.
 *
 * @author John Erik Halse
 */
public abstract class Type extends Attribute {
    /** Should this Type be serialized to persistent storage */
    private boolean isTransient = false;
    /** True if this Type can be overridden */
    private boolean overrideable = true;
    /** True if this Type should only show up in expert mode in UI */
    private boolean isExpertSetting = false;
    /** List of constraint that apply for the values of this type */
    private List<Constraint> constraints = new ArrayList<Constraint>();
    /** The class the value of this type must be an instance of (or instance of
     * a subclass.
     */
    private Class legalValueType;

    /** Creates a new instance of Type.
     *
     * @param name
     * @param value
     */
    public Type(String name, Object value) {
        super(name.intern(), value);
        legalValueType = value != null ? value.getClass() : this.getClass();
        constraints.add(new LegalValueTypeConstraint());
    }

    /** Get the description of this type
     *
     * The description should be suitable for showing in a user interface.
     *
     * @return this type's description
     */
    abstract String getDescription();

    /** The default value for this type
     *
     * @return this type's default value
     */
    abstract Object getDefaultValue();

    /** Get the legal values for this type.
     *
     * @return the legal values for this type or null if there are no
     *         restrictions.
     */
    abstract Object[] getLegalValues();

    /** Is this an 'overrideable' setting. All settings are overrideable by
     * default.
     *
     * @return True if this is an an overrideable setting.
     */
    public boolean isOverrideable() {
        return overrideable;
    }

    /** Set if this Type should be overideable.
     *
     * @param b true if this Type should be overideable.
     */
    public void setOverrideable(boolean b) {
        overrideable = b;
    }

    /** Returns true if this ComplexType should be saved to persistent storage.
    *
    * @return true if this ComplexType should be saved to persistent storage.
    */
   public boolean isTransient() {
       return isTransient;
   }

   /** Set to false if this attribute should not be serialized to persistent
    * storage.
    *
    * @param b if false this complexType will not be saved to persistent
    *          storage.
    */
   public void setTransient(boolean b) {
       isTransient = b;
   }

    /** Returns true if this Type should only show up in expert mode in UI.
     *
     * @return true if this Type should only show up in expert mode in UI.
     */
    public boolean isExpertSetting() {
        return isExpertSetting;
    }

    /** Set if this Type should only show up in expert mode in UI.
     *
     * @param isExpertSetting true if this Type should only show up in
     *        expert mode in UI.
     */
    public void setExpertSetting(boolean isExpertSetting) {
        this.isExpertSetting = isExpertSetting;
    }

    /** Returns a list of constraints for the value of this type.
     *
     * @return Returns the constraints or null if there aren't any.
     */
    public List getConstraints() {
        return constraints;
    }

    /** Add a constraint to this type.
     *
     * Every constraint must be fulfilled for a value of this type to be valid.
     *
     * @param constraint the constraint to add.
     */
    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
        Collections.sort(constraints);
    }

    /**
     * Get the class values of this Type must be an instance of.
     *
     * @return Returns the legalValueType.
     */
    public Class getLegalValueType() {
        return legalValueType;
    }

    /**
     * Set the class values of this Type must be an instance of.
     *
     * @param legalValueType The legalValueType to set.
     */
    public void setLegalValueType(Class legalValueType) {
        this.legalValueType = legalValueType;
    }

    /**
     * The implementation of equals consider to Types as equal if name and
     * value are equal. Description is allowed to differ.
     */
    public boolean equals(Object o) {
        return this == o
                || (o instanceof Type
                        && this.getName().equals(((Type) o).getName()) && this
                        .getValue().equals(((Type) o).getValue()));
    }
}
