/* BeanShellProcessor
 *
 * Created on Aug 4, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package org.archive.crawler.processor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * A processor which runs a BeanShell script on the CrawlURI.
 *
 * Script source may be provided via a file
 * local to the crawler. 
 * Script source should define
 * a method with one argument, 'run(curi)'. Each processed CrawlURI is
 * passed to this script method. 
 * 
 * Other variables available to the script include 'self' (this 
 * BeanShellProcessor instance) and 'controller' (the crawl's 
 * CrawlController instance). 
 * 
 * @author gojomo
 * @version $Date: 2009-03-02 22:52:51 +0000 (Mon, 02 Mar 2009) $, $Revision: 6149 $
 */
public class BeanShellProcessor extends Processor implements FetchStatusCodes {

    private static final long serialVersionUID = 6926589944337050754L;

    private static final Logger logger =
        Logger.getLogger(BeanShellProcessor.class.getName());

    /** setting for script file */
    public final static String ATTR_SCRIPT_FILE = "script-file"; 

    /** whether each thread should have its own script runner (true), or
     * they should share a single script runner with synchronized access */
    public final static String ATTR_ISOLATE_THREADS = "isolate-threads";

    protected ThreadLocal<Interpreter> threadInterpreter;
    protected Interpreter sharedInterpreter;
    public Map<Object,Object> sharedMap = Collections.synchronizedMap(
            new HashMap<Object,Object>());
    
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public BeanShellProcessor(String name) {
        super(name, "BeanShellProcessor. Runs the BeanShell script source " +
                "(supplied directly or via a file path) against the " +
                "current URI. Source should define a script method " +
                "'process(curi)' which will be passed the current CrawlURI. " +
                "The script may also access this BeanShellProcessor via" +
                "the 'self' variable and the CrawlController via the " +
                "'controller' variable.");
        Type t = addElementToDefinition(new SimpleType(ATTR_SCRIPT_FILE,
                "BeanShell script file", ""));
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(ATTR_ISOLATE_THREADS,
                "Whether each ToeThread should get its own independent " +
                "script context, or they should share synchronized access " +
                "to one context. Default is true, meaning each threads " +
                "gets its own isolated context.", true));
        t.setOverrideable(false);

    }

    protected void innerProcess(CrawlURI curi) {
        // depending on previous configuration, interpreter may 
        // be local to this thread or shared
        Interpreter interpreter = getInterpreter(); 
        synchronized(interpreter) {
            // synchronization is harmless for local thread interpreter,
            // necessary for shared interpreter
            try {
                interpreter.set("curi",curi);
                interpreter.eval("process(curi)");
            } catch (EvalError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
    }

    /**
     * Get the proper Interpreter instance -- either shared or local 
     * to this thread. 
     * @return Interpreter to use
     */
    protected synchronized Interpreter getInterpreter() {
        if(sharedInterpreter!=null) {
            return sharedInterpreter;
        }
        Interpreter interpreter = threadInterpreter.get(); 
        if(interpreter==null) {
            interpreter = newInterpreter(); 
            threadInterpreter.set(interpreter);
        }
        return interpreter; 
    }

    /**
     * Create a new Interpreter instance, preloaded with any supplied
     * source code or source file and the variables 'self' (this 
     * BeanShellProcessor) and 'controller' (the CrawlController). 
     * 
     * @return  the new Interpreter instance
     */
    protected Interpreter newInterpreter() {
        Interpreter interpreter = new Interpreter(); 
        try {
            interpreter.set("self", this);
            interpreter.set("controller", getController());
            
            String filePath = (String) getUncheckedAttribute(null, ATTR_SCRIPT_FILE);
            if(filePath.length()>0) {
                try {
                    File file = getSettingsHandler().getPathRelativeToWorkingDirectory(filePath);
                    interpreter.source(file.getPath());
                } catch (IOException e) {
                    logger.log(Level.SEVERE,"unable to read script file",e);
                }
            }
        } catch (EvalError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return interpreter; 
    }

    protected void initialTasks() {
        super.initialTasks();
        kickUpdate();
    }

    /**
     * Setup (or reset) Intepreter variables, as appropraite based on 
     * thread-isolation setting. 
     */
    public void kickUpdate() {
        // TODO make it so running state (tallies, etc.) isn't lost on changes
        // unless unavoidable
        if((Boolean)getUncheckedAttribute(null,ATTR_ISOLATE_THREADS)) {
            sharedInterpreter = null; 
            threadInterpreter = new ThreadLocal<Interpreter>(); 
        } else {
            sharedInterpreter = newInterpreter(); 
            threadInterpreter = null;
        }
    }
}
