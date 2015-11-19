/* ComplexType
 *
 * $Id: ComplexType.java 5028 2007-03-29 23:21:48Z gojomo $
 *
 * Created on Dec 17, 2003
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.Constraint.FailedCheck;
import org.archive.net.UURI;

/** Superclass of all configurable modules.
 *
 * This class is in many ways the heart of the settings framework. All modules
 * that should be configurable extends this class or one of its subclasses.
 *
 * All subclasses of this class will automatically conform to the
 * JMX DynamicMBean. You could then use the {@link #getMBeanInfo()} method to
 * investigate which attributes this module supports and then use the
 * {@link #getAttribute(String)} and {@link #setAttribute(Attribute)} methods to
 * alter the attributes values.
 *
 * Because the settings framework supports per domain/host settings there is
 * also available context sensitive versions of the DynamicMBean methods.
 * If you use the non context sensitive methods, it is the global settings
 * that will be altered.
 *
 * @author John Erik Halse
 */
public abstract class ComplexType extends Type implements DynamicMBean {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.settings.ComplexType");

    private transient SettingsHandler settingsHandler;
    private transient ComplexType parent;
    private String description;
    private String absoluteName;
    protected final List<Type> definition = new ArrayList<Type>();
    protected final Map<String,Type> definitionMap = new HashMap<String,Type>();
    private boolean initialized = false;
    private String[] preservedFields = new String[0];

    /**
     * Private constructor to make sure that no one
     * instantiates this class with the empty constructor.
     */
    private ComplexType() {
        super(null, null);
    }

    /** Creates a new instance of ComplexType.
     *
     * @param name the name of the element.
     * @param description the description of the element.
     */
    public ComplexType(String name, String description) {
        super(name, null);
        this.description = description.intern();
    }

    protected void setAsOrder(SettingsHandler settingsHandler)
    throws InvalidAttributeValueException {
        this.settingsHandler = settingsHandler;
        this.absoluteName = "";
        globalSettings().addTopLevelModule((CrawlOrder) this);
        addComplexType(settingsHandler.getSettingsObject(null), this);
        this.parent = null;
    }

    /** Get the global settings object (aka order).
     *
     * @return the global settings object.
     */
    public CrawlerSettings globalSettings() {
        if (settingsHandler == null) {
            return null;
        }
        return settingsHandler.getSettingsObject(null);
    }

    public Type addElement(CrawlerSettings settings, Type type)
        throws InvalidAttributeValueException {
        getOrCreateDataContainer(settings).addElementType(type);
        if (type instanceof ComplexType) {
            addComplexType(settings, (ComplexType) type);
        }
        return type;
    }

    private ComplexType addComplexType(CrawlerSettings settings,
            ComplexType object) throws InvalidAttributeValueException {

        if (this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        setupVariables(object);
        settings.addComplexType(object);
        if (!object.initialized) {
            Iterator it = object.definition.iterator();
            while (it.hasNext()) {
                Type t = (Type) it.next();
                object.addElement(settings, t);
            }
            object.earlyInitialize(settings);
        }
        object.initialized = true;

        return object;
    }

    private ComplexType replaceComplexType(CrawlerSettings settings,
            ComplexType object) throws InvalidAttributeValueException,
            AttributeNotFoundException {
        if (this.settingsHandler == null) {
            throw new IllegalStateException(
                    "Can't add ComplexType to 'free' ComplexType");
        }
        String[] preservedFields = object.getPreservedFields();

        setupVariables(object);

        DataContainer oldData = settings.getData(object);
        settings.addComplexType(object);
        DataContainer newData = settings.getData(object);

        if (!object.initialized) {
            Iterator it = object.definition.iterator();
            while (it.hasNext()) {
                Type t = (Type) it.next();

                // Check if attribute should be copied from old object.
                boolean found = false;
                if (preservedFields.length > 0) {
                    for (int i = 0; i < preservedFields.length; i++) {
                        if (preservedFields[i].equals(t.getName())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found && oldData.copyAttribute(t.getName(), newData)) {
                    if (t instanceof ComplexType) {
                        object.setupVariables((ComplexType) t);
                    }
                } else {
                    object.addElement(settings, t);
                }
            }
            object.earlyInitialize(settings);
        }
        object.initialized = true;

        return object;
    }

    /** Set a list of attribute names that the complex type should attempt to
     * preserve if the module is exchanged with an other one.
     *
     * @param preservedFields array of attributenames to preserve.
     */
    protected void setPreservedFields(String[] preservedFields) {
        this.preservedFields = preservedFields;
    }

    /** Get a list of attribute names that the complex type should attempt to
     * preserve if the module is exchanged with an other one.
     *
     * @return an array of attributenames to preserve.
     */
    protected String[] getPreservedFields() {
        return this.preservedFields;
    }

    /** Get the active data container for this ComplexType for a specific
     * settings object.
     *
     * If no value has been overridden on the settings object for this
     * ComplexType, then it traverses up until it find a DataContainer with
     * values for this ComplexType.
     *
     * This method should probably not be called from user code. It is a helper
     * method for the settings framework.
     *
     * @param context Context from which we get settings.
     * @return the active DataContainer.
     */
    protected DataContainer getDataContainerRecursive(Context context) {
        if (context.settings == null) {
            return null;
        }
        DataContainer data = context.settings.getData(this);
        if (data == null && context.settings.getParent(context.uri) != null) {
            context.settings = context.settings.getParent(context.uri);
            data = getDataContainerRecursive(context);
        }
        return data;
    }

    /** Get the active data container for this ComplexType for a specific
     * settings object.
     *
     * If the key has not been overridden on the settings object for this
     * ComplexType, then it traverses up until it find a DataContainer with
     * the key for this ComplexType.
     *
     * This method should probably not be called from user code. It is a helper
     * method for the settings framework.
     *
     * @param context the settings object for which the {@link DataContainer}
     *                 is active.
     * @param key the key to look for.
     * @return the active DataContainer.
     * @throws AttributeNotFoundException
     */
    protected DataContainer getDataContainerRecursive(Context context,
            String key) throws AttributeNotFoundException {
        Context c = new Context(context.settings, context.uri);
        DataContainer data = getDataContainerRecursive(c);
        while (data != null) {
            if (data.containsKey(key)) {
                return data;
            }
            c.settings = data.getSettings().getParent(c.uri);
            data = getDataContainerRecursive(c);
        }
        throw new AttributeNotFoundException(key);
    }

    /** Sets up some variables for a new complex type.
     *
     * The complex type is set up to be an attribute of
     * this complex type.
     *
     * @param object to be set up.
     */
    private void setupVariables(ComplexType object) {
        object.parent = this;
        object.settingsHandler = getSettingsHandler();
        object.absoluteName =
            (getAbsoluteName() + '/' + object.getName()).intern();
    }

    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /** Get the absolute name of this ComplexType.
     *
     * The absolute name is like a file path with the name of the element
     * prepended by all the parents names separated by slashes.
     * @return Absolute name.
     */
    public String getAbsoluteName() {
        return absoluteName;
    }

    /**
     * Get settings object valid for a URI.
     * <p/>
     * This method takes an object,
     * try to convert it into a {@link CrawlURI} and then tries to get the
     * settings object from it. If this fails, then the global settings object
     * is returned.
     * <p/>
     * If the requested attribute is not set on this settings
     * object it tries its parent until it gets a settings object where this
     * attribute is set is found. If nothing is found, global settings is
     * returned.
     *
     * @param o possible {@link CrawlURI}.
     * @param attributeName the attribute that should have a value set on the
     *            returned settings object.
     * @return the settings object valid for the URI.
     */
    Context getSettingsFromObject(Object o, String attributeName) {
        Context context;
        if (o == null) {
            context = null;
        } else if (o instanceof Context) {
            context = (Context) o;
        } else if (o instanceof CrawlerSettings) {
            context = new Context((CrawlerSettings) o, null);
        } else if (o instanceof UURI || o instanceof CandidateURI) {
            // Try to get settings for URI that has no references to a
            // CrawlServer [SIC - CrawlURI may have CrawlServer -gjm]
            context = new Context();
            context.uri = (o instanceof CandidateURI)?
                ((CandidateURI) o).getUURI(): (UURI)o;
            try {
               context.settings = getSettingsHandler().
                   getSettings(context.uri.getReferencedHost(),
                       context.uri);
            }
            catch (URIException e1) {
                logger.severe("Failed to get host");
            }

            if (attributeName != null) {
                try {
                    context.settings =
                        getDataContainerRecursive(context, attributeName).
                            getSettings();
                } catch (AttributeNotFoundException e) {
                    // Nothing found, globals will be used
                }
            }
        } else {
            logger.warning("Unknown object type: " +
                o.getClass().getName());
            context = null;
        }

        // if settings could not be resolved use globals.
        if (context == null) {
            context = new Context(globalSettings(), null);
        }
        return context;
    }

    /** Get settings object valid for a URI.
    *
    * This method takes an object, try to convert it into a {@link CrawlURI}
    * and then tries to get the settings object from it. If this fails, then
    * the global settings object is returned.
    *
    * @param o possible {@link CrawlURI}.
    * @return the settings object valid for the URI.
    */
    Context getSettingsFromObject(Object o) {
        return getSettingsFromObject(o, null);
    }

    /** Returns true if an element is overridden for this settings object.
     *
     * @param settings the settings object to investigate.
     * @param name the name of the element to check.
     * @return true if element is overridden for this settings object, false
     *              if not set here or is first defined here.
     * @throws AttributeNotFoundException if element doesn't exist.
     */
    public boolean isOverridden(CrawlerSettings settings, String name)
            throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        DataContainer data = settings.getData(this);
        if (data == null || !data.containsKey(name)) {
            return false;
        }

        // Try to find attribute, will throw an exception if not found.
        Context context = new Context(settings.getParent(), null);
        getDataContainerRecursive(context, name);
        return true;
    }

    /** Obtain the value of a specific attribute from the crawl order.
     *
     * If the attribute doesn't exist in the crawl order, the default
     * value will be returned.
     *
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved.
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public Object getAttribute(String name)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        return getAttribute(null, name);
    }

    /** Obtain the value of a specific attribute that is valid for a
     * specific CrawlURI.
     *
     * This method will try to get the attribute from the host settings
     * valid for the CrawlURI. If it is not found it will traverse the
     * settings up to the order and as a last resort deliver the default
     * value. This is also the case if the CrawlURI is null or if the CrawlURI
     * hasn't been assigned a CrawlServer.
     *
     * @param name the name of the attribute to be retrieved.
     * @param uri the CrawlURI that this attribute should be valid for.
     * @return The value of the attribute retrieved.
     * @see #getAttribute(Object settings, String name)
     * @throws AttributeNotFoundException
     */
    public Object getAttribute(String name, CrawlURI uri)
        throws AttributeNotFoundException {
        return getAttribute(uri, name);
    }
    
    /**
     * Obtain the value of a specific attribute that is valid for a specific
     * CrawlerSettings object.<p>
     * 
     * This method will first try to get a settings object from the supplied
     * context, then try to look up the attribute from this settings object. If
     * it is not found it will traverse the settings up to the order and as a
     * last resort deliver the default value.
     * 
     * @param context the object to get the settings from.
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved.
     * @see CrawlerSettings
     * @throws AttributeNotFoundException
     */
    public Object getAttribute(Object context, String name)
        throws AttributeNotFoundException {
        Context ctxt = getSettingsFromObject(context);

        // If settings is not set, return the default value
        if (ctxt.settings == null) {
            try {
                return ((Type) definitionMap.get(name)).getDefaultValue();
            } catch (NullPointerException e) {
                throw new AttributeNotFoundException(
                        "Could not find attribute: " + name);
            }
        }

        return getDataContainerRecursive(ctxt, name).get(name);
    }

    /**
     * Obtain the value of a specific attribute that is valid for a specific
     * CrawlerSettings object.
     * <p>
     * 
     * This method will first try to get a settings object from the supplied
     * context, then try to look up the attribute from this settings object. If
     * it is not found it will traverse the settings up to the order and as a
     * last resort deliver the default value.
     * <p>
     * 
     * The only difference from the {@link #getAttribute(Object, String)}is
     * that this method doesn't throw any checked exceptions. If an undefined
     * attribute is requested from a ComplexType, it is concidered a bug and a
     * runtime exception is thrown instead.
     * 
     * @param context the object to get the settings from.
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved.
     * @see #getAttribute(Object, String)
     * @see CrawlerSettings
     * @throws IllegalArgumentException
     */
    public Object getUncheckedAttribute(Object context, String name) {
        try {
            return getAttribute(context, name);
        } catch (AttributeNotFoundException e) {
            throw new IllegalArgumentException("Was passed '" + name +
                "' and got this exception: " + e);
        }
    }

    /** Obtain the value of a specific attribute that is valid for a
     * specific CrawlerSettings object.
     *
     * This method will try to get the attribute from the supplied host
     * settings object. If it is not found it will return <code>null</code>
     * and not try to investigate the hierarchy of settings.
     *
     * @param settings the CrawlerSettings object to search for this attribute.
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved or null if its not set.
     * @see CrawlerSettings
     * @throws AttributeNotFoundException is thrown if the attribute doesn't
     *         exist.
     */
    public Object getLocalAttribute(CrawlerSettings settings, String name)
            throws AttributeNotFoundException {

        settings = settings == null ? globalSettings() : settings;

        DataContainer data = settings.getData(this);
        if (data != null && data.containsKey(name)) {
            // Attribute was found return it.
            return data.get(name);
        }
        // Try to find the attribute, will throw an exception if not found.
        Context context = new Context(settings, null);
        getDataContainerRecursive(context, name);
        return null;
    }

    /** Set the value of a specific attribute of the ComplexType.
     *
     * This method sets the specific attribute for the order file.
     *
     * @param attribute The identification of the attribute to be set and the
     *                  value it is to be set to.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with this name.
     * @throws InvalidAttributeValueException is thrown if the attribute is of
     *         wrong type and cannot be converted to the right type.
     * @throws MBeanException this is to conform to the MBean specification, but
     *         this exception is never thrown, though this might change in the
     *         future.
     * @throws ReflectionException this is to conform to the MBean specification, but
     *         this exception is never thrown, though this might change in the
     *         future.
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public synchronized final void setAttribute(Attribute attribute)
        throws
            AttributeNotFoundException,
            InvalidAttributeValueException,
            MBeanException,
            ReflectionException {
        setAttribute(settingsHandler.getSettingsObject(null), attribute);
    }

    /** Set the value of a specific attribute of the ComplexType.
     *
     * This method is an extension to the Dynamic MBean specification so that
     * it is possible to set the value for a CrawlerSettings object other than
     * the settings object representing the order.
     *
     * @param settings the settings object for which this attributes value is valid
     * @param attribute The identification of the attribute to be set and the
     *                  value it is to be set to.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with this name.
     * @throws InvalidAttributeValueException is thrown if the attribute is of
     *         wrong type and cannot be converted to the right type.
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public synchronized final void setAttribute(CrawlerSettings settings,
            Attribute attribute) throws InvalidAttributeValueException,
            AttributeNotFoundException {

        if(settings==null){
            settings = globalSettings();
        }

        DataContainer data = getOrCreateDataContainer(settings);
        Object value = attribute.getValue();

        ModuleAttributeInfo attrInfo = (ModuleAttributeInfo) getAttributeInfo(
                settings.getParent(), attribute.getName());

        ModuleAttributeInfo localAttrInfo = (ModuleAttributeInfo) data
                .getAttributeInfo(attribute.getName());

        // Check if attribute exists
        if (attrInfo == null && localAttrInfo == null) {
            throw new AttributeNotFoundException(attribute.getName());
        }

        // Check if we are overriding and if that is allowed for this attribute
        if (localAttrInfo == null) {
            if (!attrInfo.isOverrideable()) {
                throw new InvalidAttributeValueException(
                        "Attribute not overrideable: " + attribute.getName());
            }
            localAttrInfo = new ModuleAttributeInfo(attrInfo);
        }

        // Check if value is of correct type. If not, see if it is
        // a string and try to turn it into right type
        Class typeClass = getDefinition(attribute.getName()).getLegalValueType();
        if (!(typeClass.isInstance(value)) && value instanceof String) {
            try {
                value = SettingsHandler.StringToType((String) value,
                        SettingsHandler.getTypeName(typeClass.getName()));
            } catch (ClassCastException e) {
                throw new InvalidAttributeValueException(
                        "Unable to decode string '" + value + "' into type '"
                                + typeClass.getName() + "'");
            }
        }

        // Check if the attribute value is legal
        FailedCheck error = checkValue(settings, attribute.getName(), value);
        if (error != null) {
            if (error.getLevel() == Level.SEVERE) {
                throw new InvalidAttributeValueException(error.getMessage());
            } else if (error.getLevel() == Level.WARNING) {
                if (!getSettingsHandler().fireValueErrorHandlers(error)) {
                    throw new InvalidAttributeValueException(error.getMessage());
                }
            } else {
                getSettingsHandler().fireValueErrorHandlers(error);
            }
        }

        // Everything ok, set it
        localAttrInfo.setType(value);
        Object oldValue = data.put(attribute.getName(), localAttrInfo, value);

        // If the attribute is a complex type other than the old value,
        // make sure that all sub attributes are correctly set
        if (value instanceof ComplexType && value != oldValue) {
            ComplexType complex = (ComplexType) value;
            replaceComplexType(settings, complex);
        }
    }

    /**
     * Get the content type definition for an attribute.
     *
     * @param attributeName the name of the attribute to get definition for.
     * @return the content type definition for the attribute.
     */
    Type getDefinition(String attributeName) {
        return (Type) definitionMap.get(attributeName);
    }

    /**
     * Check an attribute to see if it fulfills all the constraints set on the
     * definition of this attribute.
     *
     * @param settings the CrawlerSettings object for which this check was
     *            executed.
     * @param attributeName the name of the attribute to check.
     * @param value the value to check.
     * @return null if everything is ok, otherwise it returns a FailedCheck
     *         object with detailed information of what went wrong.
     */
    public FailedCheck checkValue(CrawlerSettings settings,
            String attributeName, Object value) {
        return checkValue(settings, attributeName,
                getDefinition(attributeName), value);
    }

    FailedCheck checkValue(CrawlerSettings settings, String attributeName,
            Type definition, Object value) {
        FailedCheck res = null;

        // Check if value fulfills any constraints
        List constraints = definition.getConstraints();
        if (constraints != null) {
            for (Iterator it = constraints.iterator(); it.hasNext()
                    && res == null;) {
                res = ((Constraint) it.next()).check(settings, this,
                        definition, value);
            }
        }

        return res;
    }

    /** Unset an attribute on a per host level.
     *
     * This methods removes an override on a per host or per domain level.
     *
     * @param settings the settings object for which the attribute should be
     *        unset.
     * @param name the name of the attribute.
     * @return The removed attribute or null if nothing was removed.
     * @throws AttributeNotFoundException is thrown if the attribute name
     *         doesn't exist.
     */
    public Object unsetAttribute(CrawlerSettings settings, String name)
            throws AttributeNotFoundException {

        if (settings == globalSettings()) {
            throw new IllegalArgumentException(
                "Not allowed to unset attributes in Crawl Order.");
        }

        DataContainer data = settings.getData(this);
        if (data != null && data.containsKey(name)) {
            // Remove value
            return data.removeElement(name);
        }

        // Value not found. Check if we should return null or throw an exception
        // This method throws an exception if not found.
        Context context = new Context(settings, null);
        getDataContainerRecursive(context, name);
        return null;
    }

    private DataContainer getOrCreateDataContainer(CrawlerSettings settings)
        throws InvalidAttributeValueException {

        // Get this ComplexType's data container for the submitted settings
        DataContainer data = settings.getData(this);

        // If there isn't a container, create one
        if (data == null) {
            ComplexType parent = getParent();
            if (parent == null) {
                settings.addTopLevelModule((ModuleType) this);
            } else {
                DataContainer parentData =
                    settings.getData(parent);
                if (parentData == null) {
                    if (this instanceof ModuleType) {
                        settings.addTopLevelModule((ModuleType) this);
                    } else {
                        settings.addTopLevelModule((ModuleType) parent);
                        try {
                            parent.setAttribute(settings, this);
                        } catch (AttributeNotFoundException e) {
                            logger.severe(e.getMessage());
                        }
                    }
                } else {
                    globalSettings().getData(parent).copyAttributeInfo(
                        getName(),
                        parentData);
                }
            }

            // Create fresh DataContainer
            data = settings.addComplexType(this);
        }

        // Make sure that the DataContainer references right type
        if (data.getComplexType() != this) {
            if (this instanceof ModuleType) {
                data = settings.addComplexType(this);
            }
        }
        return data;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
     */
    public AttributeList getAttributes(String[] name) {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
     */
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
     */
    public Object invoke(String arg0, Object[] arg1, String[] arg2)
        throws MBeanException, ReflectionException {
        throw new ReflectionException(
            new NoSuchMethodException("No methods to invoke."));
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#getMBeanInfo()
     */
    public MBeanInfo getMBeanInfo() {
        return getMBeanInfo(globalSettings());
    }

    public MBeanInfo getMBeanInfo(Object context) {
        MBeanAttributeInfoIterator it = getAttributeInfoIterator(context);
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[it.size()];
        int index = 0;
        while(it.hasNext()) {
            attributes[index++] = (MBeanAttributeInfo) it.next();
        }

        MBeanInfo info =
            new MBeanInfo(getClass().getName(), getDescription(), attributes,
                null, null, null);
        return info;
    }

    /** Get the effective Attribute info for an element of this type from
     * a settings object.
     *
     * @param settings the settings object for which the Attribute info is
     *        effective.
     * @param name the name of the element to get the attribute for.
     * @return the attribute info
     */
    public MBeanAttributeInfo getAttributeInfo(CrawlerSettings settings,
            String name) {

        MBeanAttributeInfo info = null;

        Context context = new Context(settings, null);
        DataContainer data = getDataContainerRecursive(context);
        while (data != null && info == null) {
            info = data.getAttributeInfo(name);
            if (info == null) {
                context.settings = data.getSettings().getParent();
                data = getDataContainerRecursive(context);
            }
        }

        return info;
    }

    /** Get the Attribute info for an element of this type from the global
     * settings.
     *
     * @param name the name of the element to get the attribute for.
     * @return the attribute info
     */
    public MBeanAttributeInfo getAttributeInfo(String name) {
        return getAttributeInfo(globalSettings(), name);
    }

    /** Get the description of this type
     *
     * The description should be suitable for showing in a user interface.
     *
     * @return this type's description
     */
    public String getDescription() {
        return description;
    }

    /** Get the parent of this ComplexType.
     *
     * @return the parent of this ComplexType.
     */
    public ComplexType getParent() {
        return parent;
    }

    /** Set the description of this ComplexType
     *
     * The description should be suitable for showing in a user interface.
     *
     * @param string the description to set for this type.
     */
    public void setDescription(String string) {
        description = string;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.Type#getDefaultValue()
     */
    public Object getDefaultValue() {
        return this;
    }

    /** Add a new attribute to the definition of this ComplexType.
     *
     * This method can only be called before the ComplexType has been
     * initialized. This usally means that this method is available for
     * constructors of subclasses of this class.
     *
     * @param type the type to add.
     * @return the newly added type.
     */
    public Type addElementToDefinition(Type type) {
        if (isInitialized()) {
            throw new IllegalStateException(
                    "Elements should only be added to definition in the " +
                    "constructor.");
        }
        if (definitionMap.containsKey(type.getName())) {
            definition.remove(definitionMap.remove(type.getName()));
        }
            
        definition.add(type);
        definitionMap.put(type.getName(), type);
        return type;
    }

    /** Get an element definition from this complex type.
     *
     * This method can only be called before the ComplexType has been
     * initialized. This usally means that this method is available for
     * constructors of subclasses of this class.
     *
     * @param name name of element to get.
     * @return the requested element or null if non existent.
     */
    public Type getElementFromDefinition(String name) {
        if (isInitialized()) {
            throw new IllegalStateException(
                    "Elements definition can only be accessed in the " +
                    "constructor.");
        }
        return (Type) definitionMap.get(name);
    }
    
    /**
     * This method can only be called before the ComplexType has been
     * initialized. This usually means that this method is available for
     * constructors of subclasses of this class.
     * @param name Name of element to remove.
     * @return Element removed.
     */
    protected Type removeElementFromDefinition(final String name) {
        if (isInitialized()) {
            throw new IllegalStateException(
                "Elements definition can only be removed in constructor.");
        }
        Object removedObj = this.definitionMap.remove(name);
        if (removedObj != null) {
            this.definition.remove(removedObj);
        }
        return (Type)removedObj;
    } 

    /** This method can be overridden in subclasses to do local
     * initialisation.
     *
     * This method is run before the class has been updated with
     * information from settings files. That implies that if you
     * call getAttribute inside this method you will only get the
     * default values.
     *
     * @param settings the CrawlerSettings object for which this
     *        complex type is defined.
     */
    public void earlyInitialize(CrawlerSettings settings) {
    }

    /** Returns true if this ComplexType is initialized.
     *
     * @return true if this ComplexType is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    public Object[] getLegalValues() {
        return null;
    }

    /** Returns this object.
     *
     * This method is implemented to be able to treat the ComplexType as an
     * subclass of {@link javax.management.Attribute}.
     *
     * @return this object.
     * @see javax.management.Attribute#getValue()
     */
    public Object getValue() {
        return this;
    }

    class Context {
        CrawlerSettings settings;
        UURI uri;

        Context() {
            settings = null;
            uri = null;
        }

        Context(CrawlerSettings settings, UURI uri) {
            this.settings = settings;
            this.uri = uri;
        }
    }

    /** Get an Iterator over all the attributes in this ComplexType.
    *
    * @param context the context for which this set of attributes are valid.
    * @return an iterator over all the attributes in this map.
    */
   public Iterator iterator(Object context) {
       return new AttributeIterator(context);
   }

   /** Get an Iterator over all the MBeanAttributeInfo in this ComplexType.
   *
   * @param context the context for which this set of MBeanAttributeInfo are valid.
   * @return an iterator over all the MBeanAttributeInfo in this map.
   */
   public MBeanAttributeInfoIterator getAttributeInfoIterator(Object context) {
       return new MBeanAttributeInfoIterator(context);
   }

   /**
    * Iterator over all attributes in a ComplexType.
    *
    * @author John Erik Halse
    */
   private class AttributeIterator implements Iterator {
       private Context context;
       private Stack<Iterator<MBeanAttributeInfo>> attributeStack
        = new Stack<Iterator<MBeanAttributeInfo>>();
       private Iterator currentIterator;

       public AttributeIterator(Object ctxt) {
           this.context = getSettingsFromObject(ctxt);
           Context c = new Context(context.settings, context.uri);
           DataContainer data = getDataContainerRecursive(c);
           while (data != null) {
               this.attributeStack.push(data.getLocalAttributeInfoList().
                   iterator());
               c.settings = data.getSettings().getParent();
               data = getDataContainerRecursive(c);
           }

           this.currentIterator = (Iterator) this.attributeStack.pop();
       }

       public boolean hasNext() {
           if (this.currentIterator.hasNext()) {
               return true;
           }
           if (this.attributeStack.isEmpty()) {
               return false;
           }
           this.currentIterator = (Iterator) this.attributeStack.pop();
           return this.currentIterator.hasNext();
       }

       public Object next() {
           hasNext();
           try {
               MBeanAttributeInfo attInfo = (MBeanAttributeInfo) this.currentIterator.next();
               Object attr = getAttribute(this.context, attInfo.getName());
               if (!(attr instanceof Attribute)) {
                   attr = new Attribute(attInfo.getName(), attr);
               }
               return attr;
           } catch (AttributeNotFoundException e) {
               // This should never happen
               e.printStackTrace();
               return null;
           }
       }

       public void remove() {
           throw new UnsupportedOperationException();
       }
   }

   /**
    * Iterator over all MBeanAttributeInfo for this ComplexType
    *
    * @author John Erik Halse
    */
   public class MBeanAttributeInfoIterator implements Iterator {
       private Context context;
       private Stack<Iterator<MBeanAttributeInfo>> attributeStack
        = new Stack<Iterator<MBeanAttributeInfo>>();
       private Iterator currentIterator;
       private int attributeCount = 0;

       public MBeanAttributeInfoIterator(Object ctxt) {
           this.context = getSettingsFromObject(ctxt);
           //Stack attributeStack = new Stack();
           //
           DataContainer data = getDataContainerRecursive(context);
           while (data != null) {
               attributeStack.push(data.getLocalAttributeInfoList().iterator());
               attributeCount += data.getLocalAttributeInfoList().size();
               context.settings = data.getSettings().getParent();
               data = getDataContainerRecursive(context);
           }

           this.currentIterator = (Iterator) this.attributeStack.pop();
       }

       public boolean hasNext() {
            if (this.currentIterator.hasNext()) {
                return true;
            }
            if (this.attributeStack.isEmpty()) {
                return false;
            }
            this.currentIterator = (Iterator)this.attributeStack.pop();
            return this.currentIterator.hasNext();
        }

       public Object next() {
           hasNext();
           MBeanAttributeInfo attInfo = (MBeanAttributeInfo) this.currentIterator.next();
           return attInfo;
       }

       public void remove() {
           throw new UnsupportedOperationException();
       }

       public int size() {
           return attributeCount;
       }
   }
   
   @Override
   public String toString() {
       // In 1.6, toString goes into infinite loop.  Default implementation is
       // return getName() + '=' + getValue() but this class returns itself
       // for a value on which we do a toString... and around we go.  Short
       // circuit it here.
       return getName() + ": " +
           getClass().getName() + "@" + Integer.toHexString(hashCode());
   }
}
