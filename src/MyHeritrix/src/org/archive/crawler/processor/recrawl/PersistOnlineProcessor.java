/* PersistOnlineProcessor.java
 * 
 * Created on Feb 18, 2005
 *
 * Copyright (C) 2007 Internet Archive.
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
package org.archive.crawler.processor.recrawl;

import org.archive.util.bdbje.EnhancedEnvironment;

import st.ata.util.AList;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;

/**
 * Common superclass for persisting Processors which directly store/load
 * to persistence (as opposed to logging for batch load later). 
 * @author gojomo
 */
public abstract class PersistOnlineProcessor extends PersistProcessor {
    private static final long serialVersionUID = -666479480942267268L;
    
    protected StoredSortedMap<String,AList> store;
    protected Database historyDb;

    /**
     * Usual constructor
     * 
     * @param name
     * @param string
     */
    public PersistOnlineProcessor(String name, String string) {
        super(name, string);
    }

    protected void initialTasks() {
        // TODO: share single store instance between Load and Store processors
        // (shared context? EnhancedEnvironment?)
        if (isEnabled()) {
            store = initStore();
        }
    }

    protected StoredSortedMap<String,AList> initStore() {
        StoredSortedMap<String,AList> historyMap;
        try {
            EnhancedEnvironment env = getController().getBdbEnvironment();
            StoredClassCatalog classCatalog = env.getClassCatalog();
            DatabaseConfig dbConfig = historyDatabaseConfig();
            historyDb = env.openDatabase(null, URI_HISTORY_DBNAME, dbConfig);
            historyMap = new StoredSortedMap<String,AList>(historyDb,
                    new StringBinding(), new SerialBinding<AList>(classCatalog,
                            AList.class), true);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
        return historyMap;
    }

    @Override
    protected void finalTasks() {
        if (isEnabled()) {
            try {
                historyDb.sync();
                historyDb.close();
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
