/* ExternalImplDecideRule
 * 
 * Created on May 25, 2005
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
package org.archive.crawler.deciderules;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.SimpleType;

/**
 * A rule that can be configured to take alternate implementations
 * of the ExternalImplInterface.
 * If no implementation specified, or none found, returns
 * configured decision.
 * @author stack
 * @version $Date: 2007-02-18 21:53:01 +0000 (Sun, 18 Feb 2007) $, $Revision: 4914 $
 */
public class ExternalImplDecideRule
extends PredicatedDecideRule {

    private static final long serialVersionUID = 7727715263469524372L;

    private static final Logger LOGGER =
        Logger.getLogger(ExternalImplDecideRule.class.getName());
    static final String ATTR_IMPLEMENTATION = "implementation-class";
    private ExternalImplInterface implementation = null;

    /**
     * @param name Name of this rule.
     */
    public ExternalImplDecideRule(String name) {
        super(name);
        setDescription("ExternalImplDecideRule. Rule that " +
            "instantiates implementations of the ExternalImplInterface. " +
            "The implementation needs to be present on the classpath. " +
            "On initialization, the implementation is instantiated (" +
            "assumption is that there is public default constructor).");
        addElementToDefinition(new SimpleType(ATTR_IMPLEMENTATION,
            "Name of implementation of ExternalImplInterface class to " +
            "instantiate.", ""));
    }
    
    protected boolean evaluate(Object obj) {
        ExternalImplInterface impl = getConfiguredImplementation(obj);
        return (impl != null)? impl.evaluate(obj): false;
    }
    
    /** 
     * Get implementation, if one specified.
     * If none specified, will keep trying to find one. Will be messy
     * if the provided class is not-instantiable or not implementation
     * of ExternalImplInterface.
     * @param o A context object.
     * @return Instance of <code>ExternalImplInterface</code> or null.
     */
    protected synchronized ExternalImplInterface
            getConfiguredImplementation(Object o) {
        if (this.implementation != null) {
            return this.implementation;
        }
        ExternalImplInterface result = null;
        try {
            String className =
                (String)getAttribute(o, ATTR_IMPLEMENTATION);
            if (className != null && className.length() != 0) {
                Object obj = Class.forName(className).newInstance();
                if (!(obj instanceof ExternalImplInterface)) {
                    LOGGER.severe("Implementation " + className + 
                        " does not implement ExternalImplInterface");
                }
                result = (ExternalImplInterface)obj;
                this.implementation = result;
            }
        } catch (AttributeNotFoundException e) {
            LOGGER.severe(e.getMessage());
        } catch (InstantiationException e) {
            LOGGER.severe(e.getMessage());
        } catch (IllegalAccessException e) {
            LOGGER.severe(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.severe(e.getMessage());
        }
        return result;
    }
}