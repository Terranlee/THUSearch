/* MapType
 *
 * $Id: MapType.java 4661 2006-09-25 23:11:16Z paul_jack $
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

import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.settings.Constraint.FailedCheck;

/** This class represents a container of settings.
 *
 * This class is usually used to make it possible to have a dynamic number
 * of ModuleTypes like for instance a list of filters of different type.
 *
 * When this type is overridden on a per domain basis, the following
 * restrictions apply:
 * <ul>
 *   <li>Added elements is placed after the elements in the map it overrides.
 *   <li>You can not remove elements from the map it overrides. If it is
 *       necessary to be able to remove an element, this has to be done by
 *       adding some disable feature to the modules referenced by the map. An
 *       example of this is the enabled attribute on the
 *       {@link org.archive.crawler.framework.Filter} class.
 *   <li>All elements defined in maps that this map overrides might have their
 *       settings changed, but the order can not be changed.
 * </ul>
 *
 * @author John Erik Halse
 */
public class MapType extends ComplexType {

    private static final long serialVersionUID = -3694800285930202700L;

    /** The content type allowed for this map. */
    private final Type definition;

    /** Construct a new MapType object.
     *
     * @param name the name of this element.
     * @param description the description of the attribute.
     */
    public MapType(String name, String description) {
        this(name, description, Object.class);
    }

    /** Construct a new MapType object.
     *
     * @param name the name of this element.
     * @param description the description of the attribute.
     * @param type the type allowed for this map
     */
    public MapType(String name, String description, Class type) {
        super(name, description);
        this.definition = new SimpleType("dummy", "dummy", null);
        this.definition.setLegalValueType(type);
    }

    /** Add a new element to this map.
     *
     * @param settings the settings object for this method to have effect.
     * @param element the element to be added.
     * @return Element added.
     * @throws InvalidAttributeValueException
     */
    public Type addElement(CrawlerSettings settings, Type element)
        throws InvalidAttributeValueException {
        settings = settings == null ? globalSettings() : settings;

        if (settings != globalSettings()) {
            try {
                getAttribute(settings, element.getName());
                // Element exist, throw an exception
                throw new IllegalArgumentException(
                    "Duplicate element: " + element.getName());

            } catch (AttributeNotFoundException e) {
                // Element doesn't exist, ok to add
            }
        }

        if (!(element instanceof MapType)) {
            return super.addElement(settings, element);
        } else {
            throw new IllegalArgumentException("Nested maps are not allowed.");
        }
    }

    /** Remove an attribute from the map.
     *
     * @param settings the settings object for which this method has effect.
     * @param name name of the attribute to remove.
     * @return the element that was removed.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted name.
     */
    public Object removeElement(CrawlerSettings settings, String name)
      throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).removeElement(name);
    }

    /** Move an attribute up one place in the list.
     *
     * @param settings the settings object for which this method has effect.
     * @param name name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at the top.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted name.
     */
    public boolean moveElementUp(CrawlerSettings settings, String name)
      throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).moveElementUp(name);
    }

    /** Move an attribute down one place in the list.
     *
     * @param settings the settings object for which this method has effect.
     * @param name name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at bottom.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted name.
     */
    public boolean moveElementDown(CrawlerSettings settings, String name)
      throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        return settings.getData(this).moveElementDown(name);
    }

    /** Returns true if this map is empty.
     *
     * @param context the settings object for which this set of elements
     * are valid.
     * @return true if this map is empty.
     */
    public boolean isEmpty(Object context) {
        Context ctxt = getSettingsFromObject(context);

        DataContainer data = getDataContainerRecursive(ctxt);
        while (data != null) {
            if (data.hasAttributes()) {
                return false;
            }
            ctxt.settings = data.getSettings().getParent();
            data = getDataContainerRecursive(ctxt);
        }
        return true;
    }

    /** Get the number of elements in this map.
     *
     * @param context the settings object for which this set of elements
     *                 are valid.
     * @return the number of elements in this map.
     */
    public int size(Object context) {
        Context ctxt = getSettingsFromObject(context);

        int size = 0;
        DataContainer data = getDataContainerRecursive(ctxt);
        while (data != null) {
            size += data.size();
            ctxt.settings = data.getSettings().getParent();
            data = getDataContainerRecursive(ctxt);
        }
        return size;
    }

    /**
     * Get the content type definition for attributes of this map.
     *
     * @param attributeName since all attributes of a map are of the same type,
     * this value is not used.
     * @return the content type definition for attributes of this map.
     */
    Type getDefinition(String attributeName) {
        return definition;
    }

    /**
     * Get the content type allowed for this map.
     *
     * @return the content type allowed for this map.
     */
    public Class getContentType() {
        return this.definition.getLegalValueType();
    }

    FailedCheck checkValue(CrawlerSettings settings, String attributeName,
            Type definition, Object value) {
        FailedCheck res = super.checkValue(settings, attributeName, definition,
                value);

        definition = super.getDefinition(attributeName);

        // Check if value fulfills any constraints
        List constraints = definition != null ? definition.getConstraints()
                : null;
        if (constraints != null) {
            FailedCheck ac = null;
            for (Iterator it = constraints.iterator(); it.hasNext()
                    && ac == null;) {
                ac = ((Constraint) it.next()).check(settings, this, definition,
                        value);
                if (res == null
                        || ac.getLevel().intValue() >= res.getLevel()
                                .intValue()) {
                    res = ac;
                }
            }
        }

        return res;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.Type#addConstraint(org.archive.crawler.settings.Constraint)
     */
    public void addConstraint(Constraint constraint) {
        definition.addConstraint(constraint);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.Type#getConstraints()
     */
    public List getConstraints() {
        return definition.getConstraints();
    }
}
