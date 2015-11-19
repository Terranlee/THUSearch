/* ProcessorChain
 *
 * $Id: ProcessorChain.java 4434 2006-08-04 04:02:39Z gojomo $
 *
 * Created on Mar 1, 2004
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
package org.archive.crawler.framework;

import java.util.Iterator;
import java.util.logging.Logger;

import org.archive.crawler.settings.MapType;


/** This class groups together a number of processors that logically fit
 * together.
 *
 * @author John Erik Halse
 */
public class ProcessorChain {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.framework.ProcessorChain");

    private final MapType processorMap;
    private ProcessorChain nextChain;
    private Processor firstProcessor;

    /** Construct a new processor chain.
     *
     * @param processorMap a map of the processors belonging to this chain.
     */
    public ProcessorChain(MapType processorMap) {
        this.processorMap = processorMap;

        Processor previous = null;

        for (Iterator it = processorMap.iterator(null); it.hasNext();) {
            Processor p = (Processor) it.next();

            if (previous == null) {
                firstProcessor = p;
            } else {
                previous.setDefaultNextProcessor(p);
            }

            logger.info(
                "Processor: " + p.getName() + " --> " + p.getClass().getName());

            previous = p;
        }
    }

    /** Set the processor chain that the URI should be working through after
     * finishing this one.
     *
     * @param nextProcessorChain the chain that should be processed after this
     *        one.
     */
    public void setNextChain(ProcessorChain nextProcessorChain) {
        this.nextChain = nextProcessorChain;
    }

    /** Get the processor chain that the URI should be working through after
     * finishing this one.
     *
     * @return the next processor chain.
     */
    public ProcessorChain getNextProcessorChain() {
        return nextChain;
    }

    /** Get the first processor in the chain.
     *
     * @return the first processor in the chain.
     */
    public Processor getFirstProcessor() {
        return firstProcessor;
    }

    /** Get the first processor that is of class <code>classType</code> or a
     * subclass of it.
     *
     * @param classType the class of the requested processor.
     * @return the first processor matching the classType.
     */
    public Processor getProcessor(Class classType) {
        for (Iterator it = processorMap.iterator(null); it.hasNext();) {
            Processor p = (Processor) it.next();
            if (classType.isInstance(p)) {
                return p;
            }
        }
        return null;
    }

    /** Get the number of processors in this chain.
     *
     * @return the number of processors in this chain.
     */
    public int size() {
        return processorMap.size(null);
    }

    /** Get an iterator over the processors in this chain.
     *
     * @return an iterator over the processors in this chain.
     */
    public Iterator iterator() {
        return processorMap.iterator(null);
    }

    public void kickUpdate() {
        Iterator iter = iterator();
        while(iter.hasNext()) {
            Processor p = (Processor) iter.next(); 
            p.kickUpdate(); 
        }
    }
}
