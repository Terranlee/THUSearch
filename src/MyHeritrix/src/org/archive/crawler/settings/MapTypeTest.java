/* MapTypeTest
 *
 * $Id: MapTypeTest.java 6082 2008-12-09 02:03:13Z gojomo $
 *
 * Created on Jan 29, 2004
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
import java.util.Iterator;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.prefetch.Preselector;

/** JUnit tests for MapType
 *
 * @author John Erik Halse
 *
 */
public class MapTypeTest extends SettingsFrameworkTestCase {

    /** Test different aspects of manipulating a MapType for the global
     * settings.
     *
     * @throws InvalidAttributeValueException
     * @throws AttributeNotFoundException
     */
    public void testAddRemoveSizeGlobal()
            throws InvalidAttributeValueException, AttributeNotFoundException,
            MBeanException, ReflectionException {

        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        assertTrue("Map should be empty", map.isEmpty(null));
        assertEquals("Map should be empty", map.size(null), 0);

        ModuleType module = new Preselector("testModule");
        assertSame("Did not return added element",
                map.addElement(null, module), module);
        assertFalse("Map should contain a element", map.isEmpty(null));
        assertEquals("Map should contain a element", map.size(null), 1);

        assertSame("Did not return removed element", map.removeElement(null,
                "testModule"), module);
        assertTrue("Map should be empty", map.isEmpty(null));
        assertEquals("Map should be empty", map.size(null), 0);
    }

    /** Test different aspects of manipulating a MapType for the per domain
     * settings.
     *
     * @throws InvalidAttributeValueException
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public void testAddRemoveSizeHost()
           throws InvalidAttributeValueException, AttributeNotFoundException,
                  MBeanException, ReflectionException {

        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        MBeanAttributeInfo atts[] = map.getMBeanInfo().getAttributes();
        for (int i = 0; i < atts.length; i++) {
            map.removeElement(getGlobalSettings(), atts[i].getName());
        }

        assertTrue("Map should be empty", map.isEmpty(getPerHostSettings()));
        assertEquals("Map should be empty", 0, map.size(getPerHostSettings()));

        ModuleType module1 = new Preselector("testModule1");
        ModuleType module2 = new Preselector("testModule2");
        ModuleType module3 = new Preselector("testModule3");

        assertSame("Did not return added element", module1,
            map.addElement(getGlobalSettings(), module1));

        assertSame("Did not return added element", module2,
            map.addElement(getPerHostSettings(), module2));

        assertSame("Did not return added element", module3,
            map.addElement(getPerHostSettings(), module3));

        assertFalse("Map should contain elements",
            map.isEmpty(getPerHostSettings()));
        assertEquals("Wrong number of elements", 3,
            map.size(getPerHostSettings()));
        assertEquals("Wrong number of elements", 1,
            map.size(getGlobalSettings()));

        module1.setAttribute(getPerHostSettings(), new SimpleType("enabled",
                "desc", new Boolean(false)));
        checkOrder(getGlobalSettings(), new Type[] { module1}, map);
        checkOrder(getPerHostSettings(),
                new Type[] { module1, module2, module3}, map);

        assertSame("Did not return removed element",
            map.removeElement(getGlobalSettings(), "testModule1"), module1);

        assertSame("Did not return removed element",
            map.removeElement(getPerHostSettings(), "testModule2"), module2);

        assertSame("Did not return removed element",
            map.removeElement(getPerHostSettings(), "testModule3"), module3);

        assertTrue("Map should be empty", map.isEmpty(getPerHostSettings()));
        assertEquals("Map should be empty", 0, map.size(getPerHostSettings()));
    }

    public void testMoveElementUp() throws AttributeNotFoundException,
            MBeanException, ReflectionException, InvalidAttributeValueException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        ModuleType module1 = new Preselector("testModule1");
        ModuleType module2 = new Preselector("testModule2");
        ModuleType module3 = new Preselector("testModule3");
        map.addElement(null, module1);
        map.addElement(null, module2);
        map.addElement(null, module3);

        Type modules[] = new Type[] {module1, module2, module3};
        checkOrder(null, modules, map);

        assertTrue(map.moveElementUp(null, "testModule2"));

        modules = new Type[] {module2, module1, module3};
        checkOrder(null, modules, map);

        assertFalse(map.moveElementUp(null, "testModule2"));

        modules = new Type[] {module2, module1, module3};
        checkOrder(null, modules, map);
    }

    public void testMoveElementDown() throws InvalidAttributeValueException,
            AttributeNotFoundException, MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_PRE_FETCH_PROCESSORS);

        ModuleType module1 = new Preselector("testModule1");
        ModuleType module2 = new Preselector("testModule2");
        ModuleType module3 = new Preselector("testModule3");
        map.addElement(null, module1);
        map.addElement(null, module2);
        map.addElement(null, module3);

        Type modules[] = new Type[] {module1, module2, module3};
        checkOrder(null, modules, map);

        assertTrue(map.moveElementDown(null, "testModule2"));

        modules = new Type[] {module1, module3, module2};
        checkOrder(null, modules, map);

        assertFalse(map.moveElementDown(null, "testModule2"));

        modules = new Type[] {module1, module3, module2};
        checkOrder(null, modules, map);
    }

    /** Helper method for checking that elements are in a certain order after
     * maipulating them.
     *
     * @param settings
     * @param modules
     * @param map
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public void checkOrder(CrawlerSettings settings, Type[] modules, MapType map)
           throws AttributeNotFoundException, MBeanException, ReflectionException {

        settings = settings == null ? map.globalSettings() : settings;

        MBeanAttributeInfo atts[] = map.getMBeanInfo(settings).getAttributes();
        assertEquals("AttributeInfo wrong length", modules.length, atts.length);
        for(int i=0; i<atts.length; i++) {
            assertEquals("AttributeInfo in wrong order", modules[i].getValue(),
                map.getAttribute(settings, atts[i].getName()));
        }

        Iterator it = map.iterator(settings);
        int i = 0;
        while(it.hasNext()) {
            assertEquals("Iterator in wrong order", modules[i].getValue(),
                    ((Attribute) it.next()).getValue());
            i++;
        }
        assertEquals("Iterator wrong length", modules.length, i);
    }

    public void testGetDefaultValue() throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        assertSame(map.getDefaultValue(), map);
    }

    public void testGetLegalValues() throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        assertNull(map.getLegalValues());
    }

    /*
     * Test for Object getValue()
     */
    public void testGetValue() throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        assertSame(map.getValue(), map);
    }

    /* Test for getAttribute
     *
     */
    public void testGetAttribute() throws AttributeNotFoundException,
            MBeanException, ReflectionException, InvalidAttributeValueException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        SimpleType type1 = new SimpleType("testType1", "description", "value");
        SimpleType type2 = new SimpleType("testType2", "description", "value");
        map.addElement(getGlobalSettings(), type1);
        map.addElement(getPerDomainSettings(), type2);
        assertEquals(type1.getValue(), map.getAttribute(getPerHostSettings(),
                "testType1"));
        assertEquals(type2.getValue(), map.getAttribute(getPerHostSettings(),
        "testType2"));
        try {
            map.getAttribute(getGlobalSettings(), "testType2");
            fail();
        } catch (AttributeNotFoundException e) {
            // OK
        }
    }

    public void testListAttributes() throws AttributeNotFoundException,
            MBeanException, ReflectionException, InvalidAttributeValueException {
        MapType map = (MapType) getSettingsHandler().getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        List<Attribute> atts = new ArrayList<Attribute>();
        for (Iterator it = map.iterator(null); it.hasNext();) {
            atts.add(new SimpleType("", "", ((Attribute) it.next()).getValue()));
        }

        SimpleType type1 = new SimpleType("testType1", "description", "value");
        SimpleType type2 = new SimpleType("testType2", "description", "value");
        map.addElement(getGlobalSettings(), type1);
        map.addElement(getPerDomainSettings(), type2);
        getSettingsHandler().writeSettingsObject(getGlobalSettings());
        getSettingsHandler().writeSettingsObject(getPerDomainSettings());

        atts.add(type1);
        atts.add(type2);
        Type modules[] = (Type[]) atts.toArray(new Type[0]);
        checkOrder(getPerHostSettings(), modules, map);

        XMLSettingsHandler newHandler = new XMLSettingsHandler(getOrderFile());
        newHandler.initialize();
        CrawlerSettings newPer = newHandler.getSettingsObject(getPerDomainSettings().getScope());

        checkOrder(newPer, modules, map);
    }

}
