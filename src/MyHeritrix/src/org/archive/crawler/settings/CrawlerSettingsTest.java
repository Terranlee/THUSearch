/* CrawlerSettingsTest
 *
 * $Id: CrawlerSettingsTest.java 4662 2006-09-25 23:45:21Z paul_jack $
 *
 * Created on Jan 28, 2004
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;


/** Test the CrawlerSettings object
 *
 * @author John Erik Halse
 */
public class CrawlerSettingsTest extends SettingsFrameworkTestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    final public void testAddComplexType() {
        ModuleType mod = new ModuleType("name");
        DataContainer data = getGlobalSettings().addComplexType(mod);
        assertNotNull(data);
    }

    final public void testGetModule() {
        ModuleType mod = new ModuleType("name");
        getGlobalSettings().addComplexType(mod);
        assertSame(mod, getGlobalSettings().getModule("name"));
    }
    
    public void testSerializingSimpleModuleType()
    throws IOException, ClassNotFoundException {
        ModuleType mt =
            new ModuleType("testSerializingSimpleModuleType");
        ModuleType mtDeserialized = (ModuleType)serializeDeserialize(mt);
        assertEquals(mt.getName(), mtDeserialized.getName());
    }
    
    public void testSerializingStringAttributeModuleType()
    throws IOException, ClassNotFoundException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        ModuleType mt =
            new ModuleType("testSerializingStringAttributeModuleType");
        final String value = "value";
        mt.addElementToDefinition(new SimpleType("name", "description",
            value));
        ModuleType mtDeserialized = (ModuleType)serializeDeserialize(mt);
        assertEquals(mt.getName(), mtDeserialized.getName());
        assertEquals(value, (String)mtDeserialized.getAttribute("name"));
    }
    
    public void testSerializingTextField()
    throws IOException, ClassNotFoundException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        TextField tf = new TextField("testSerializingTextField");
        TextField tfDeserialized = (TextField)serializeDeserialize(tf);
        assertEquals(tf.toString(), tfDeserialized.toString());
    }
    
    protected Object serializeDeserialize(Object obj)
    throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        byte [] objectBytes = baos.toByteArray();
        ObjectInputStream ois =
            new ObjectInputStream(new ByteArrayInputStream(objectBytes));
        return ois.readObject();
    }
}
