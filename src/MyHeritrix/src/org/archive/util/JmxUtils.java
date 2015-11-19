/* JmxUtils
 * 
 * Created on May 18, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.util;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.InvalidOpenTypeException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;


/**
 * Static utility used by JMX.
 * @author stack
 * @version $Date: 2005-12-15 00:35:44 +0000 (Thu, 15 Dec 2005) $, $Revision: 4022 $
 */
public class JmxUtils {
    private static final Logger LOGGER =
        Logger.getLogger(JmxUtils.class.getName());
    public static final String TYPE = "type";
    public static final String SERVICE = "CrawlService";
    public static final String JOB = SERVICE + ".Job";
    public static final String NAME = "name";
    public static final String HOST = "host";
    public static final String JMX_PORT = "jmxport";
    public static final String GUI_PORT = "guiport";
    public static final String KEY = "key";

    /**
     * Key for name of the Heritrix instance hosting a Job: i.e. the
     * CrawlJob's host/'mother'.
     */
    public static final String MOTHER = "mother";

    public static final ObjectName MBEAN_SERVER_DELEGATE;
    static {
        try {
            MBEAN_SERVER_DELEGATE = new ObjectName(
                "JMImplementation:type=MBeanServerDelegate");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static final List OPENTYPES =
        Arrays.asList(OpenType.ALLOWED_CLASSNAMES);
    
    protected JmxUtils() {
        super();
    }
    
    /**
     * Return a string suitable for logging on registration.
     * @param mbeanName Mbean that got registered.
     * @param mbeanServer Server we were registered to.
     * @param registrationDone Whether we registered successfully or not.
     * @return String suitable for logging.
     */
    public static String getLogRegistrationMsg(final String mbeanName,
            final MBeanServer mbeanServer, final boolean registrationDone) {
        return mbeanName + (registrationDone? " " : " NOT ") +
            "registered to " + JmxUtils.getServerDetail(mbeanServer);
    }
    
    public static String getLogUnregistrationMsg(final String mbeanName,
            final MBeanServer mbeanServer) {
        return mbeanName + " unregistered from " +
            JmxUtils.getServerDetail(mbeanServer);
    }
    
    public static String getServerDetail(final MBeanServer server) {
        ObjectName delegateName = null;
        try {
            delegateName = new ObjectName(
                    "JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            LOGGER.log(Level.SEVERE, "Failed to create ObjectName for " +
                    "JMImplementation:type=MBeanServerDelegate", e);
            return null;
        }
        StringBuffer buffer = new StringBuffer("MBeanServerId=");
        try {
            buffer.append((String) server.getAttribute(delegateName,
                    "MBeanServerId"));
            buffer.append(", SpecificationVersion=");
            buffer.append((String) server.getAttribute(delegateName,
                    "SpecificationVersion"));
            buffer.append(", ImplementationVersion=");
            buffer.append((String) server.getAttribute(delegateName,
                    "ImplementationVersion"));
            buffer.append(", SpecificationVendor=");
            buffer.append((String) server.getAttribute(delegateName,
                    "SpecificationVendor"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed gettting server detail for " +
                    "JMImplementation:type=MBeanServerDelegate", e);
        }
        return buffer.toString();
    }
    
    /**
     * @param operationName
     *            Name of operation we're checking params on (Used in exception
     *            if one thrown).
     * @param params
     *            Amount of params passed.
     * @param count
     *            Count of params expected.
     */
    public static void checkParamsCount(String operationName, Object[] params,
            int count) {
        if ((params.length != count)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(
                  "Cannot invoke " + operationName + ": Passed " +
                  Integer.toString(params.length) + " argument(s) " +
                  "but expected " + Integer.toString(count)),
                  "Wrong number of arguments passed: unable to invoke " +
                  operationName + " method");
        } 
    }
    
    /**
     * @param in MBeanOperation to convert.
     * @return An OpenMBeanOperation version of the passed MBeanOperation.
     */
    public static OpenMBeanOperationInfo convertToOpenMBeanOperation(
            MBeanOperationInfo in) {
        return convertToOpenMBeanOperation(in, null, null);
    }
    
    /**
     * @param in MBeanOperation to convert.
     * @param prefix A prefix to add to the name of new operation.
     * @param returnType Return type to use in new operation.  Use this
     * parameter to override the passed <code>in</code> return type.
     * @return An OpenMBeanOperation version of the passed MBeanOperation.
     */   
    public static OpenMBeanOperationInfo convertToOpenMBeanOperation(
            final MBeanOperationInfo in, final String prefix,
            final OpenType returnType) {
        MBeanParameterInfo [] params = in.getSignature();
        OpenMBeanParameterInfo [] signature =
            new OpenMBeanParameterInfo[params.length];
        for (int i = 0; i < params.length; i++) {
            signature[i] = convertToOpenMBeanOperationInfo(params[i]);
        }
        return new OpenMBeanOperationInfoSupport(
                ((prefix != null)? prefix + in.getName(): in.getName()),
                in.getDescription(), signature,
                (returnType != null)?
                        returnType: getOpenType(in.getReturnType()),
                convertImpact(in.getImpact()));
    }
    
    public static int convertImpact(final int impact) {
        if (impact != MBeanOperationInfo.ACTION &&
                impact != MBeanOperationInfo.ACTION_INFO &&
                impact != MBeanOperationInfo.INFO) {
            // Don't allow back MBeanOperationInfo.UNKNOWN impact.
            return MBeanOperationInfo.ACTION_INFO;
        }
        return impact;
    }
    
    /**
     * @param in MBeanParameterInfo to convert to an OpenMBeanParameterInfo.
     * @return OpenMBeanParameterInfo version of <code>in</code>
     */
    public static OpenMBeanParameterInfo
            convertToOpenMBeanOperationInfo(MBeanParameterInfo in) {
        return new OpenMBeanParameterInfoSupport(in.getName(),
                in.getDescription(), getOpenType(in.getType())); 
    }
    
    public static boolean isOpenType(final Class c) {
        return isOpenType(c.getName());
    }
    
    public static boolean isOpenType(final String classname) {
        return OPENTYPES.contains(classname);
    }

    /**
     * @param classString Java class name.
     * @return Its OpenType equivalent.
     */
    public static OpenType getOpenType(final String classString) {
        return getOpenType(classString, null);
    }
    
    /**
     * @param classString Java class name.
     * @param defaultType If no equivalent found, use passed defaultType.
     * @return Its OpenType equivalent.
     */
    public static OpenType getOpenType(String classString,
            final OpenType defaultType) {
        if (classString.equals("void")) {
            return SimpleType.VOID;
        }
        if (!isOpenType(classString)) {
            throw new InvalidOpenTypeException(classString);
        }
        if (classString.equals(String.class.getName())) {
            return SimpleType.STRING;
        } else if (classString.equals(Boolean.class.getName())) {
            return SimpleType.BOOLEAN;
        } else if (classString.equals(Long.class.getName())) {
            return SimpleType.LONG;
        } else if (classString.equals(Integer.class.getName())) {
            return SimpleType.INTEGER;
        } else if (classString.equals(Float.class.getName())) {
            return SimpleType.FLOAT;
        } else if (classString.equals(Double.class.getName())) {
            return SimpleType.DOUBLE;
        } else if (defaultType != null) {
            return defaultType;
        }
        throw new RuntimeException("Unsupported type: " + classString);
    }
   
    /**
     * @param in AttributeInfo to convert.
     * @return OpenMBeanAttributeInfo version of <code>in</code>.
     */
    public static OpenMBeanAttributeInfo
    convertToOpenMBeanAttribute(MBeanAttributeInfo in) {
        return convertToOpenMBeanAttribute(in, null);
    }

    /**
     * @param in AttributeInfo to convert.
     * @param prefix Prefix to add to names of new attributes. If null, nothing
     * is added.
     * @return OpenMBeanAttributeInfo version of <code>in</code>.
     */
    public static OpenMBeanAttributeInfo
            convertToOpenMBeanAttribute(final MBeanAttributeInfo in,
                    final String prefix) {
        return createOpenMBeanAttributeInfo(getOpenType(in.getType()), in,
                prefix);
    }
    
    /**
     * @param type Type of new OpenMBeanAttributeInfo.
     * @param in The MBeanAttributeInfo we're converting.
     * @param prefix Prefix to add to name of new Attribute (If null, nothing
     * is added).
     * @return New OpenMBeanAttributeInfo based on <code>in</code>.
     */
    public static OpenMBeanAttributeInfo createOpenMBeanAttributeInfo(
            final OpenType type, final MBeanAttributeInfo in,
            final String prefix) {
        return new OpenMBeanAttributeInfoSupport(
                ((prefix != null)? prefix + in.getName(): in.getName()),
                in.getDescription(), type, in.isReadable(),
                in.isWritable(), in.isIs());
    }
    
    /**
     * @param m A map to make a CompositeType of (Assumption is that order does
     * not matter. If it does, pass a map of sorted keys).
     * @param compositeTypeName Name to give created compositeType.
     * @param compositeTypeDescription Description.
     * @return CompositeType made by examination of passed TreeMap.
     * @throws OpenDataException
     */
    public static CompositeType createCompositeType(final Map m,
            final String compositeTypeName,
            final String compositeTypeDescription)
    throws OpenDataException {
        String [] keys = new String[m.size()];
        OpenType [] types = new OpenType[m.size()];
        int index = 0;
        for (final Iterator i = m.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            keys[index] = key;
            types[index] = getOpenType(m.get(key).getClass().getName());
            index++;
        }
        return new CompositeType(compositeTypeName, compositeTypeDescription,
            keys, keys, types);
    }

    public static InetSocketAddress extractAddress(final ObjectName name) {
        return new InetSocketAddress(name.getKeyProperty(HOST),
            Integer.parseInt(name.getKeyProperty(JMX_PORT)));
    }

    /**
     * Returns the UID portion of the name key property of
     * an object name representing a "CrawlService.Job" bean.
     * It is assumed that the name will have the format
     * {crawl name}-{uid}.
     * @param on A CrawlServer.Job object name.
     * @return Uid for a CrawlServer.Job.
     */
    public static String getUid(final ObjectName on) {
        String name = on.getKeyProperty(NAME);
        return name.substring(name.indexOf("-") + 1);
    }
}