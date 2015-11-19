/* ModuleType
 *
 * $Id: ModuleType.java 4657 2006-09-25 21:44:36Z paul_jack $
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

import java.util.List;

import javax.management.InvalidAttributeValueException;

/**
 * Superclass of all modules that should be configurable.
 *
 * @author John Erik Halse
 */
public class ModuleType extends ComplexType {

    private static final long serialVersionUID = 3686678928531236811L;

    /** Creates a new ModuleType.
     *
     * This constructor is made to help implementors of subclasses. It is an
     * requirement that subclasses at the very least implements a constructor
     * that takes only the name as an argument.
     *
     * @param name the name of the module.
     * @param description the description of the module.
     */
    public ModuleType(String name, String description) {
        super(name, description);
    }

    /** Every subclass should implement this constructor
     *
     * @param name of the module
     */
    public ModuleType(String name) {
        super(name, name);
    }

    public Type addElement(CrawlerSettings settings, Type type)
            throws InvalidAttributeValueException {
        if (isInitialized()) {
            throw new IllegalStateException(
                    "Not allowed to add elements to modules after"
                            + " initialization. (Module: " + getName()
                            + ", Element: " + type.getName() + ", Settings: "
                            + settings.getName() + " (" + settings.getScope()
                            + ")");
        }
        return super.addElement(settings, type);
    }

    /**
     * Those Modules that use files on disk should list them all when this
     * method is called.
     *
     * <p>Each file (as a string name with full path) should be added to the
     * provided list.
     *
     * <p>Modules that do not use any files can safely ignore this method.
     *
     * @param list The list to add files to.
     */
    protected void listUsedFiles(List<String> list){
        // By default do nothing
    }
}