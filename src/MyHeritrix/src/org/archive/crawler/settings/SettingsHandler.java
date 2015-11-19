/* SettingsHandler
 *
 * $Id: SettingsHandler.java 6703 2009-11-25 01:28:49Z gojomo $
 *
 * Created on Dec 16, 2003
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.Checkpointer;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.settings.refinements.Refinement;
import org.archive.net.UURI;
import org.archive.util.ArchiveUtils;

/** An instance of this class holds a hierarchy of settings.
 *
 * More than one instance in memory is allowed so that a new CrawlJob could
 * be configured while another job is running.
 *
 * This class should be subclassed to adapt to a persistent storage.
 *
 * @author John Erik Halse
 */
public abstract class SettingsHandler {
    /** Cached CrawlerSettings objects */
    private SettingsCache settingsCache =
        new SettingsCache(new CrawlerSettings(this, null));

    /** Reference to the order module */
    private CrawlOrder order;

    private Set<ValueErrorHandler> valueErrorHandlers 
     = Collections.synchronizedSet(new HashSet<ValueErrorHandler>());
    private int errorReportingLevel = Level.ALL.intValue();

    /** Datatypes supported by the settings framwork */
    final static String INTEGER = "integer";
    final static String LONG = "long";
    final static String FLOAT = "float";
    final static String DOUBLE = "double";
    final static String BOOLEAN = "boolean";
    final static String STRING = "string";
    final static String TEXT = "text";
    final static String OBJECT = "object";
    final static String TIMESTAMP = "timestamp";
    final static String MAP = "map";
    final static String INTEGER_LIST = "integerList";
    final static String LONG_LIST = "longList";
    final static String FLOAT_LIST = "floatList";
    final static String DOUBLE_LIST = "doubleList";
    final static String STRING_LIST = "stringList";
    private final static String names[][] = new String[][] {
            { INTEGER, "java.lang.Integer"},
            { LONG, "java.lang.Long"},
            { FLOAT, "java.lang.Float"},
            { DOUBLE, "java.lang.Double"},
            { BOOLEAN, "java.lang.Boolean"},
            { STRING, "java.lang.String"},
            { TEXT, "org.archive.crawler.settings.TextField"},
            { OBJECT, "org.archive.crawler.settings.ModuleType"},
            { TIMESTAMP, "java.util.Date"},
            { MAP, "org.archive.crawler.settings.MapType"},
            { INTEGER_LIST,
                    "org.archive.crawler.settings.IntegerList"},
            { LONG_LIST, "org.archive.crawler.settings.LongList"},
            { FLOAT_LIST, "org.archive.crawler.settings.FloatList"},
            { DOUBLE_LIST, "org.archive.crawler.settings.DoubleList"},
            { STRING_LIST, "org.archive.crawler.settings.StringList"}};
    private final static Map<String,String> name2class
     = new HashMap<String,String>();
    private final static Map<String,String> class2name
     = new HashMap<String,String>();
    static {
        for (int i = 0; i < names.length; i++) {
            name2class.put(names[i][0], names[i][1]);
            class2name.put(names[i][1], names[i][0]);
        }
    }

    /** Create a new SettingsHandler object.
     *
     * @throws InvalidAttributeValueException
     */
    public SettingsHandler() throws InvalidAttributeValueException {
        order = new CrawlOrder();
        order.setAsOrder(this);
    }

    /** Initialize the SettingsHandler.
     *
     * This method reads the default settings from the persistent storage.
     */
    public void initialize() {
        readSettingsObject(settingsCache.getGlobalSettings());
    }
    
    public void cleanup() {
        this.settingsCache = null;
        if (this.order != null) {
            this.order.setController(null);
        }
        this.order =  null;
    }

    /** Strip off the leftmost part of a domain name.
     *
     * @param scope the domain name.
     * @return scope with everything before the first dot ripped off.
     */
    protected String getParentScope(String scope) {
        int split = scope.indexOf('.');
        return (split == -1)? null: scope.substring(split + 1);
    }

    /** Get a module by name.
     *
     * All modules in the order should have unique names. This method makes it
     * possible to get the modules of the order by its name.
     *
     * @param name the modules name.
     * @return the module the name references.
     */
    public ModuleType getModule(String name) {
        return settingsCache.getGlobalSettings().getModule(name);
    }

    /** Get a complex type by its absolute name.
     *
     * The absolute name is the complex types name and the path leading to
     * it.
     *
     * @param settings the settings object to query.
     * @param absoluteName the absolute name of the complex type to get.
     * @return the complex type referenced by the absolute name or null if
     *         the complex type could not be found in this settings object.
     * @throws AttributeNotFoundException is thrown if no ComplexType by this
     *         name exist.
     */
    public ComplexType getComplexTypeByAbsoluteName(
            CrawlerSettings settings, String absoluteName)
            throws AttributeNotFoundException {

        settings = settings == null ? settingsCache.getGlobalSettings() : settings;

        DataContainer data = settings.getData(absoluteName);
        if (data == null) {
            CrawlerSettings parentSettings = settings.getParent();
            if (parentSettings == null) {
                throw new AttributeNotFoundException(absoluteName);
            }
            return getComplexTypeByAbsoluteName(parentSettings, absoluteName);
        }
        return data.getComplexType();
    }

    protected static String getTypeName(String className) {
        return (String) class2name.get(className);
    }

    protected static String getClassName(String typeName) {
        return (String) name2class.get(typeName);
    }

    /** Convert a String object to an object of <code>typeName</code>.
     *
     * @param stringValue string to convert.
     * @param typeName type to convert to. typeName should be one of the
     *        supported types represented by constants in this class.
     * @return the new value object.
     * @throws ClassCastException is thrown if string could not be converted.
     */
    protected static Object StringToType(String stringValue, String typeName) {
        Object value;
        if (typeName == SettingsHandler.STRING) {
            value = stringValue;
        } else if (typeName == SettingsHandler.TEXT) {
            value = new TextField(stringValue);
        } else if (typeName == SettingsHandler.INTEGER) {
            value = Integer.decode(stringValue);
        } else if (typeName == SettingsHandler.LONG) {
            value = Long.decode(stringValue);
        } else if (typeName == SettingsHandler.BOOLEAN) {
            value = Boolean.valueOf(stringValue);
        } else if (typeName == SettingsHandler.DOUBLE) {
            value = Double.valueOf(stringValue);
        } else if (typeName == SettingsHandler.FLOAT) {
            value = Float.valueOf(stringValue);
        } else if (typeName == SettingsHandler.TIMESTAMP) {
            try {
                value = ArchiveUtils.parse14DigitDate(stringValue);
            } catch (ParseException e) {
                throw new ClassCastException(
                    "Cannot convert '"
                        + stringValue
                        + "' to type '"
                        + typeName
                        + "'");
            }
        } else {
            throw new ClassCastException(
                "Cannot convert '"
                    + stringValue
                    + "' to type '"
                    + typeName
                    + "'");
        }
        return value;
    }

    /** Get CrawlerSettings object in effect for a host or domain.
     *
     * If there is no specific settings for the host/domain, it will recursively
     * go up the hierarchy to find the settings object that should be used for
     * this host/domain.
     *
     * @param host the host or domain to get the settings for.
     * @return settings object in effect for the host/domain.
     * @see #getSettingsObject(String)
     * @see #getOrCreateSettingsObject(String)
     */
    public CrawlerSettings getSettings(String host) {
        return getRefinementsForSettings(getSettingsForHost(host), null);
    }

    /** Get CrawlerSettings object in effect for a host or domain.
    *
    * If there is no specific settings for the host/domain, it will recursively
    * go up the hierarchy to find the settings object that should be used for
    * this host/domain.
    * <p/>
    * This method passes around a URI that refinement are checked against.
    *
    * @param host the host or domain to get the settings for.
    * @param uuri UURI for context.
    * @return settings object in effect for the host/domain.
    * @see #getSettingsObject(String)
    * @see #getOrCreateSettingsObject(String)
    */
    public CrawlerSettings getSettings(String host, UURI uuri) {
        return getRefinementsForSettings(getSettingsForHost(host), uuri);
    }

    protected CrawlerSettings getSettingsForHost(String host) {
        CrawlerSettings settings = settingsCache.getSettings(host, null);

        if (settings == null) {
            String tmpHost = host;
            settings = getSettingsObject(tmpHost);
            while (settings == null && tmpHost != null) {
                tmpHost = getParentScope(tmpHost);
                settings = getSettingsObject(tmpHost);
            }

            settingsCache.putSettings(host, settings);
        }

        return settings;
    }

    private CrawlerSettings getRefinementsForSettings(CrawlerSettings settings,
            UURI uri) {
        if (settings.hasRefinements()) {
            for(Iterator it = settings.refinementsIterator(); it.hasNext();) {
                Refinement refinement = (Refinement) it.next();
                if (refinement.isWithinRefinementBounds(uri)) {
                    settings = getSettingsObject(settings.getScope(),
                            refinement.getReference());
                }
            }
        }

        return settings;
    }

    /** Get CrawlerSettings object for a host or domain.
     *
     * The difference between this method and the
     * <code>getSettings(String host)</code> is that this method will return
     * null if there is no settings for particular host or domain.
     *
     * @param scope the host or domain to get the settings for.
     * @return settings object for the host/domain or null if no
     *         settings exist for the host/domain.
     * @see #getSettings(String)
     * @see #getOrCreateSettingsObject(String)
     */
    public CrawlerSettings getSettingsObject(String scope) {
        return getSettingsObject(scope, null);
    }

    /**
     * Get CrawlerSettings object for a host/domain and a particular refinement.
     *
     * @param scope the host or domain to get the settings for.
     * @param refinement the refinement reference to get.
     * @return CrawlerSettings object for a host/domain and a particular
     * refinement or null if no settings exist for the host/domain.
     */
    public CrawlerSettings getSettingsObject(String scope, String refinement) {
        CrawlerSettings settings =
            settingsCache.getSettingsObject(scope, refinement);

        if (settings == null) {
            // Reference not found
            settings = new CrawlerSettings(this, scope, refinement);
            // Try to read settings from persisten storage. If its not there
            // it will be set to null.
            settings = readSettingsObject(settings);
            if (settings != null) {
                settingsCache.putSettings(scope, settings);
            }
        }
        return settings;
    }

    /** Get or create CrawlerSettings object for a host or domain.
     *
     * This method is similar to {@link #getSettingsObject(String)} except that
     * if there is no settings for this particular host or domain a new settings
     * object will be returned.
     *
     * @param scope the host or domain to get or create the settings for.
     * @return settings object for the host/domain.
     * @see #getSettings(String)
     * @see #getSettingsObject(String)
     */
    public CrawlerSettings getOrCreateSettingsObject(String scope) {
        return getOrCreateSettingsObject(scope, null);
    }

    public CrawlerSettings getOrCreateSettingsObject(String scope,
            String refinement) {
        CrawlerSettings settings;
        settings = getSettingsObject(scope, refinement);
        if (settings == null) {
            scope = scope.intern();

            // No existing settings object found, create one
            settings = new CrawlerSettings(this, scope, refinement);
            settingsCache.refreshHostToSettings();
            settingsCache.putSettings(scope, settings);
        }
        return settings;
    }

    /** Write the CrawlerSettings object to persistent storage.
     *
     * @param settings the settings object to write.
     */
    public abstract void writeSettingsObject(CrawlerSettings settings);

    /** Read the CrawlerSettings object from persistent storage.
     *
     * @param settings the settings object to be updated with data from the
     *                 persistent storage.
     * @return the updated settings object or null if there was no data for this
     *         in the persistent storage.
     */
    protected abstract CrawlerSettings readSettingsObject(CrawlerSettings settings);

    /** Delete a settings object from persistent storage.
     *
     * @param settings the settings object to delete.
     */
    public void deleteSettingsObject(CrawlerSettings settings) {
        settingsCache.deleteSettingsObject(settings);
    }

    /** Get the CrawlOrder.
     *
     * @return the CrawlOrder
     */
    public CrawlOrder getOrder() {
        return order;
    }

    /** Instatiate a new ModuleType given its name and className.
     *
     * @param name the name for the new ComplexType.
     * @param className the class name of the new ComplexType.
     * @return an instance of the class identified by className.
     *
     * @throws InvocationTargetException
     */
    @SuppressWarnings("unchecked")
    public static ModuleType instantiateModuleTypeFromClassName(
            String name, String className)
            throws InvocationTargetException {

        Class cl;
        try {
            cl = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new InvocationTargetException(e);
        }

        ModuleType module;
        try {
            Constructor co =
                cl.getConstructor(new Class[] { String.class });
            module = (ModuleType) co.newInstance(new Object[] { name });
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e);
        } catch (InstantiationException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e);
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e);
        }
        return module;
    }

    /**
     * Transforms a relative path so that it is relative to a location that is
     * regarded as a working dir for these settings. If an absolute path is given,
     * it will be returned unchanged.
     * @param path A relative path to a file (or directory)
     * @return The same path modified so that it is relative to the file level
     *         location that is considered the working directory for these settings.
     */
    public abstract File getPathRelativeToWorkingDirectory(String path);

    /**
     * Will return a Collection of strings with domains that contain 'per'
     * domain overrides (or their subdomains contain them). 
     * 
     * The domains considered are
     * limited to those that are subdomains of the supplied domain. If null or
     * empty string is supplied the TLDs will be considered.
     * @param rootDomain The domain to get domain overrides for. Examples:
     *                   'org', 'archive.org', 'crawler.archive.org' etc.
     * @return An array of domains that contain overrides. If rootDomain does not
     *         exist an empty array will be returned.
     */
    public abstract Collection getDomainOverrides(String rootDomain);

    /**
     * Unregister an instance of {@link ValueErrorHandler}.
     *
     * @param errorHandler the <code>CalueErrorHandler</code> to unregister.
     *
     * @see ValueErrorHandler
     * @see #setErrorReportingLevel(Level)
     * @see #registerValueErrorHandler(ValueErrorHandler)
     *
     */
    public void unregisterValueErrorHandler(ValueErrorHandler errorHandler) {
        valueErrorHandlers.remove(errorHandler);
    }

    /**
     * Register an instance of {@link ValueErrorHandler}.
     * <p>
     * If a ValueErrorHandler is registered, only constraints with level
     * {@link Level#SEVERE}will throw an {@link InvalidAttributeValueException}.
     * The ValueErrorHandler will recieve a notification for all failed checks
     * with level equal or greater than the error reporting level.
     *
     * @param errorHandler the <code>CalueErrorHandler</code> to register.
     *
     * @see ValueErrorHandler
     * @see #setErrorReportingLevel(Level)
     * @see #unregisterValueErrorHandler(ValueErrorHandler)
     */
    public void registerValueErrorHandler(ValueErrorHandler errorHandler) {
        if (errorHandler != null) {
            valueErrorHandlers.add(errorHandler);
        }
    }

    /**
     * Fire events on all registered {@link ValueErrorHandler}.
     *
     * @param error the failed constraints return value.
     * @return true if there was any registered ValueErrorHandlers to notify.
     */
    boolean fireValueErrorHandlers(Constraint.FailedCheck error) {
        if (error.getLevel().intValue() >= errorReportingLevel) {
            for (Iterator it = valueErrorHandlers.iterator(); it.hasNext();) {
                ((ValueErrorHandler) it.next()).handleValueError(error);
            }
        }
        return valueErrorHandlers.size() > 0;
    }

    /**
     * Set the level for which notification of failed constraints will be fired.
     *
     * @param level the error reporting level.
     */
    public void setErrorReportingLevel(Level level) {
        errorReportingLevel = level.intValue();
    }

    /**
     * Creates and returns a <tt>List</tt> of all files comprising the current
     * settings framework.
     *
     * <p>The List contains the absolute String path of each file.
     *
     * <p>The list should contain any configurable files, including such files
     * as seed file and any other files use by the various settings modules.
     *
     * <p>Implementations of the SettingsHandler that do not use files for
     * permanent storage should return an empty list.
     * @return <code>List</code> of framework files.
     */
    public abstract List getListOfAllFiles();
    
    /**
     * Clear any per-host settings cached in memory; allows editting of 
     * per-host settings files on disk, perhaps in bulk/automated fashion,
     * to take effect in running crawl. 
     */
    public void clearPerHostSettingsCache() {
        settingsCache.clear();
    }

    static ThreadLocal<SettingsHandler> threadContextSettingsHandler = 
        new ThreadLocal<SettingsHandler>();
    public static void setThreadContextSettingsHandler(SettingsHandler settingsHandler) {
        threadContextSettingsHandler.set(settingsHandler);
    }
    public static SettingsHandler getThreadContextSettingsHandler() {
        Thread t = Thread.currentThread();
        if (t instanceof Checkpointer.CheckpointingThread) {
            return ((Checkpointer.CheckpointingThread)t)
                .getController().getSettingsHandler();
        } 
        if (t instanceof ToeThread) {
            return ((ToeThread) Thread.currentThread())
                .getController().getSettingsHandler();
        } 
        if(threadContextSettingsHandler.get()!=null) {
            return threadContextSettingsHandler.get();
        }
        
        // in most cases, returning a null means an NPE soon, 
        // so perhaps this should log/raise differently
        
        // however, requesting object *might* just be transiently
        // instantiated (as in momentary deserialization with some 
        // Stored** operations), including in finalization thread 
        // (which will never be linked to a usable settingsHandler).
        // So, don't raise/log a noisy error for now. 
        return null;
    }
}
