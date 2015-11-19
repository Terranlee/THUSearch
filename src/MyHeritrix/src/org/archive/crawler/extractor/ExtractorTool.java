/* ExtractorTool
 * 
 * Created on Mar 14, 2005
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
package org.archive.crawler.extractor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.management.Attribute;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.net.UURIFactory;
import org.archive.util.HttpRecorder;
import org.archive.util.OneLineSimpleLogger;

/**
 * Run named extractors against passed ARC file.
 * This extractor tool runs suboptimally.  It takes each ARC file record,
 * writes it to a new scratch file, and then it runs each listed
 * extractor against the scratch.  It works in this manner because
 * extractors want CharSequence, being able to refer to characters
 * by absolute position, but ARCs are compressed streams.  The work
 * to get a CharSequence on an underlying compressed stream has not
 * been done.  Other issues are need to setup CrawlerSetting environment
 * so extractors can run.
 * @author stack
 * @version $Date: 2006-09-26 23:47:15 +0000 (Tue, 26 Sep 2006) $, $Revision: 4671 $
 */
public class ExtractorTool {
//    private static final Logger logger =
//        Logger.getLogger(ExtractorTool.class.getName());
    static {
        // Setup the oneline logger.
        Handler [] hs = Logger.getLogger("").getHandlers();
        for (int i = 0; i < hs.length; i++) {
            Handler h = hs[0];
            if (h instanceof ConsoleHandler) {
                h.setFormatter(new OneLineSimpleLogger());
            }
        }
    }
    
    private static final String [] DEFAULT_EXTRACTORS =
        {"org.archive.crawler.extractor.ExtractorHTTP",
            "org.archive.crawler.extractor.ExtractorHTML"};
    private final List<Processor> extractors;
    private final File scratchDir;
    private static final String DEFAULT_SCRATCH = "/tmp";
    
    public ExtractorTool()
    throws Exception {
        this(DEFAULT_EXTRACTORS, DEFAULT_SCRATCH);
    }
    
    public ExtractorTool(String [] e, String scratch)
    throws Exception {
        super();
        // Setup the scratch directory.
        this.scratchDir = scratch == null?
            new File(DEFAULT_SCRATCH): new File(scratch);
        if (!this.scratchDir.exists()) {
            this.scratchDir.mkdirs();
        }
        // Set up settings system.  Needed by extractors.
        File orderFile = new File(this.scratchDir.getAbsolutePath(),
            ExtractorTool.class.getName() + "_order.xml");
        SettingsHandler settingsHandler = new XMLSettingsHandler(orderFile);
        settingsHandler.initialize();
        settingsHandler.getOrder().
            setAttribute(new Attribute(CrawlOrder.ATTR_SETTINGS_DIRECTORY,
                this.scratchDir.getAbsolutePath()));
        CrawlerSettings globalSettings =
            settingsHandler.getSettingsObject(null);
        MapType extractorsSettings = (MapType)settingsHandler.getOrder().
            getAttribute(CrawlOrder.ATTR_EXTRACT_PROCESSORS);
        this.extractors = new ArrayList<Processor>();
        for (int i = 0; i < e.length; i++) {
            Constructor c = Class.forName(e[i]).
                getConstructor(new Class [] {String.class});
            String name = Integer.toString(i);
            Processor p  = (Processor)c.newInstance(new Object [] {name});
            extractorsSettings.addElement(globalSettings, p);
            p.setAttribute(
                new Attribute(Processor.ATTR_ENABLED, Boolean.TRUE));
            this.extractors.add(p);
        }
    }
    
    public void extract(String resource) throws IOException,
    URIException, InterruptedException {
        ARCReader reader = ARCReaderFactory.get(new File(resource));
        for (Iterator i = reader.iterator(); i.hasNext();) {
            ARCRecord ar = (ARCRecord)i.next();
            HttpRecorder hr = HttpRecorder.
                wrapInputStreamWithHttpRecord(this.scratchDir,
                    this.getClass().getName(), ar, null);
            CrawlURI curi = getCrawlURI(ar, hr);
            for (Iterator ii = this.extractors.iterator(); ii.hasNext();) {
                ((Processor)ii.next()).process(curi);
            }
            outlinks(curi);
        }
    }
    
    protected void outlinks(CrawlURI curi) {
        System.out.println(curi.getUURI().toString());
        for(Link l: curi.getOutLinks()) {
            System.out.println(" " + l.getDestination() + " " +
                l.getHopType() + " " + l.getContext());
        }
    }
    
    protected CrawlURI getCrawlURI(final ARCRecord record,
            final HttpRecorder hr)
    throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.
            getInstance(record.getMetaData().getUrl()));
        curi.setContentSize(record.getMetaData().getLength());
        curi.setContentType(record.getMetaData().getMimetype());
        curi.setHttpRecorder(hr);
        // Fake out the extractor that this is a legit HTTP transaction.
        if (!curi.getUURI().getScheme().equals("filedesc")) {
            curi.putObject(CoreAttributeConstants.A_HTTP_TRANSACTION,
                new HttpMethodBase() {
                    public String getName() {
                        return this.getClass().getName() + "_method";
                    }

                    public Header getResponseHeader(String headerName) {
                        String value = (String)record.getMetaData().
                            getHeaderValue(headerName);
                        return (value == null || value.length() == 0)?
                            null: new Header(headerName, value);
                    }
            });
            String statusCode = record.getMetaData().getStatusCode();
            curi.setFetchStatus(statusCode == null?
                200: Integer.parseInt(statusCode));
        }
        return curi;
    }
    
    /**
     * Format usage message.
     * @param formatter Help formatter instance.
     * @param options Usage options.
     * @param exitCode Exit code.
     */
    private static void usage(HelpFormatter formatter, Options options,
            int exitCode) {
        formatter.printHelp("java " + ExtractorTool.class.getName() +
            " \\\n[--scratch=DIR] [--extractor=EXTRACTOR1,EXTRACTOR2,...] ARC", options);
        System.exit(exitCode);
    }
    
    public static void main(String[] args)
    throws Exception {
        Options options = new Options();
        options.addOption(new Option("h", "help", false,
            "Prints this message and exits."));
        StringBuffer defaultExtractors = new StringBuffer();
        for (int i = 0; i < DEFAULT_EXTRACTORS.length; i++) {
            if (i > 0) {
                defaultExtractors.append(", ");
            }
            defaultExtractors.append(DEFAULT_EXTRACTORS[i]);
        }
        options.addOption(new Option("e", "extractor", true,
            "List of comma-separated extractor class names. " +
            "Run in order listed. " +
            "If no extractors listed, runs following: " +
            defaultExtractors.toString() + "."));
        options.addOption(new Option("s", "scratch", true,
            "Directory to write scratch files to. Default: '/tmp'."));
        PosixParser parser = new PosixParser();
        CommandLine cmdline = parser.parse(options, args, false);
        List cmdlineArgs = cmdline.getArgList();
        Option [] cmdlineOptions = cmdline.getOptions();
        HelpFormatter formatter = new HelpFormatter();
        // If no args, print help.
        if (cmdlineArgs.size() <= 0) {
            usage(formatter, options, 0);
        }

        // Now look at options passed.
        String [] extractors = DEFAULT_EXTRACTORS;
        String scratch = null;
        for (int i = 0; i < cmdlineOptions.length; i++) {
            switch(cmdlineOptions[i].getId()) {
                case 'h':
                    usage(formatter, options, 0);
                    break;

                case 'e':
                    String value = cmdlineOptions[i].getValue();
                    if (value == null || value.length() <= 0) {
                        // Allow saying NO extractors so we can see
                        // how much it costs just reading through
                        // ARCs.
                        extractors = new String [0];
                    } else {
                        extractors = value.split(",");
                    }
                    break;
                    
                case 's':
                    scratch = cmdlineOptions[i].getValue();
                    break;
                  
                default:
                    throw new RuntimeException("Unexpected option: " +
                        + cmdlineOptions[i].getId());
            }
        }
        
        ExtractorTool tool = new ExtractorTool(extractors, scratch);
        for (Iterator i = cmdlineArgs.iterator(); i.hasNext();) {
            tool.extract((String)i.next());
        }
    }
}
