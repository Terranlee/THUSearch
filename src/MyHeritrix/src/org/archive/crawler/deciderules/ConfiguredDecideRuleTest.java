/* ConfiguredDecideRuleTest
 * 
 * Created on Apr 4, 2005
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

import java.io.File;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 * @version $Date: 2005-04-05 01:12:11 +0000 (Tue, 05 Apr 2005) $, $Revision: 3318 $
 */
public class ConfiguredDecideRuleTest extends TmpDirTestCase {
    /**
     * Gets setup by {@link #setUp()}.
     */
    private ConfiguredDecideRule rule = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        final String name = this.getClass().getName();
        SettingsHandler settingsHandler = new XMLSettingsHandler(
            new File(getTmpDir(), name + ".order.xml"));
        settingsHandler.initialize();
        // Create a new ConfigureDecideRule instance and add it to a MapType
        // (I can change MapTypes after instantiation).  The chosen MapType
        // is the rules canonicalization rules list.
        this.rule = (ConfiguredDecideRule)((MapType)settingsHandler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(settingsHandler.
                getSettingsObject(null), new ConfiguredDecideRule(name));
    }
    
    public void testDefault() {
        Object decision = rule.decisionFor(new Object());
        assertTrue("Wrong answer " + decision, decision == DecideRule.ACCEPT);
    }
    
    public void testACCEPT()
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        runTest(DecideRule.ACCEPT);
    }
    
    public void testPASS()
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        String exceptionMessage = null;
        try {
            runTest(DecideRule.PASS);
        } catch(InvalidAttributeValueException e) {
            exceptionMessage = e.getMessage();
        }
        assertNotNull("Did not get expected exception", exceptionMessage);
    }
    
    public void testREJECT()
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        runTest(DecideRule.REJECT);
    }
    
    protected void runTest(String expectedResult)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        configure(expectedResult);
        Object decision = rule.decisionFor(new Object());
        assertTrue("Expected " + expectedResult + " but got answer " +
            decision, decision == expectedResult);
    }
    
    protected void configure(String setting)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        this.rule.setAttribute(
            new Attribute(ConfiguredDecideRule.ATTR_DECISION, setting));
    }
}
