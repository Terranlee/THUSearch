/* CredentialStoreTest
 *
 * Created on Apr 1, 2004
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
package org.archive.crawler.datamodel;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SettingsFrameworkTestCase;


/**
 * Test add, edit, delete from credential store.
 *
 * @author stack
 * @version $Revision: 4668 $, $Date: 2006-09-26 21:49:01 +0000 (Tue, 26 Sep 2006) $
 */
public class CredentialStoreTest extends SettingsFrameworkTestCase {

    protected static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CredentialTest");

    final public void testCredentials()
        throws InvalidAttributeValueException, IllegalArgumentException,
        InvocationTargetException, AttributeNotFoundException, MBeanException,
        ReflectionException {

        CredentialStore store = (CredentialStore)this.settingsHandler.
            getOrder().getAttribute(CredentialStore.ATTR_NAME);
        writeCrendentials(store, this.getGlobalSettings(), "global");
        writeCrendentials(store, this.getPerDomainSettings(), "domain");
        writeCrendentials(store, this.getPerHostSettings(), "host");
        List types = CredentialStore.getCredentialTypes();
        List globalNames = checkContextNames(store.iterator(
            this.getGlobalSettings()), types.size());
        checkContextNames(store.iterator(this.getPerDomainSettings()),
            types.size() * 2 /*This should be global + domain*/);
        checkContextNames(store.iterator(this.getPerHostSettings()),
            types.size() * 3 /*This should be global + domain + host*/);
        for (Iterator i = globalNames.iterator();
                i.hasNext();) {
            store.remove(this.getGlobalSettings(),(String)i.next());
        }
        // Should be only host and domain objects at deepest scope.
        checkContextNames(store.iterator(this.getPerHostSettings()),
           types.size() * 2);
    }

    private List checkContextNames(Iterator i, int size) {
        List<String> names = new ArrayList<String>(size);
        for (; i.hasNext();) {
            String name = ((Credential)i.next()).getName();
            names.add(name);
        }
        logger.info("Added: " + names.toString());
        assertTrue("Not enough names, size " + size, size == names.size());
        return names;
    }

    private void writeCrendentials(CredentialStore store, CrawlerSettings context,
                String prefix)
        throws InvalidAttributeValueException, AttributeNotFoundException,
        IllegalArgumentException, InvocationTargetException {

        List types = CredentialStore.getCredentialTypes();
        for (Iterator i = types.iterator(); i.hasNext();) {
            Class cl = (Class)i.next();
            Credential c = store.create(context, prefix + "." + cl.getName(),
                cl);
            assertNotNull("Failed create of " + cl, c);
            logger.info("Created " + c.getName());
        }
        List<String> names = new ArrayList<String>(types.size());
        for (Iterator i = store.iterator(null); i.hasNext();) {
            names.add(((Credential)i.next()).getName());
        }
        getSettingsHandler().writeSettingsObject(context);
    }
}
