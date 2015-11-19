/* ExtractorHTMLTest
 *
 * Created on May 19, 2004
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
package org.archive.crawler.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.HttpRecorder;
import org.archive.util.TmpDirTestCase;


/**
 * Test html extractor.
 *
 * @author stack
 * @version $Revision: 6830 $, $Date: 2010-04-21 23:39:57 +0000 (Wed, 21 Apr 2010) $
 */
public class ExtractorHTMLTest
extends TmpDirTestCase
implements CoreAttributeConstants {
    private final String ARCHIVE_DOT_ORG = "archive.org";
    private final String LINK_TO_FIND = "http://www.hewlett.org/";
    private HttpRecorder recorder = null;
    private ExtractorHTML extractor = null;
    
    protected ExtractorHTML createExtractor()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        // Hack in a settings handler.  Do this by adding this extractor
        // to the order file (I'm adding it to a random MapType; seemingly
        // can only add to MapTypes post-construction). This takes care
        // of setting a valid SettingsHandler into the ExtractorHTML (This
        // shouldn't be so difficult).  Of note, the order file below is
        // not written to disk.
        final String name = this.getClass().getName();
        SettingsHandler handler = new XMLSettingsHandler(
            new File(getTmpDir(), name + ".order.xml"));
        handler.initialize();
        return (ExtractorHTML)((MapType)handler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(handler.
                getSettingsObject(null), new ExtractorHTML(name));
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        this.extractor = createExtractor();
        final boolean USE_NET = false;
        URL url = null;
        if (USE_NET) {
            url = new URL("http://" + this.ARCHIVE_DOT_ORG);
        } else {
            File f = new File(getTmpDir(), this.ARCHIVE_DOT_ORG + ".html");
            url = f.toURI().toURL();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(("<html><head><title>test</title><body>" +
                "<a href=" + this.LINK_TO_FIND + ">Hewlett Foundation</a>" +
                "</body></html>").getBytes());
            fos.flush();
            fos.close();
        }
        this.recorder = HttpRecorder.wrapInputStreamWithHttpRecord(getTmpDir(),
            this.getClass().getName(), url.openStream(), null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInnerProcess() throws IOException {
        UURI uuri = UURIFactory.getInstance("http://" + this.ARCHIVE_DOT_ORG);
        CrawlURI curi = setupCrawlURI(this.recorder, uuri.toString());
        this.extractor.innerProcess(curi);
        Collection<Link> links = curi.getOutLinks();
        boolean foundLinkToHewlettFoundation = false;
        for (Iterator<Link> i = links.iterator(); i.hasNext();) {
            Link link = (Link)i.next();
            if (link.getDestination().toString().equals(this.LINK_TO_FIND)) {
                foundLinkToHewlettFoundation = true;
                break;
            }
        }
        assertTrue("Did not find gif url", foundLinkToHewlettFoundation);
    }
    
    private CrawlURI setupCrawlURI(HttpRecorder rec, String url)
    		throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance(url));
        curi.setContentSize(this.recorder.getRecordedInput().getSize());
        curi.setContentType("text/html");
        curi.setFetchStatus(200);
        curi.setHttpRecorder(rec);
        // Fake out the extractor that this is a HTTP transaction.
        curi.putObject(CoreAttributeConstants.A_HTTP_TRANSACTION,
            new Object());
        return curi;
    }
    
    /**
     * Test single net or local filesystem page parse.
     * Set the uuri to be a net url or instead put in place a file
     * named for this class under the unit test directory.
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InvalidAttributeValueException
     */
    public void testPageParse()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException, IOException {
        UURI uuri = null;
        
// DO
//      uuri = UURIFactory.getInstance("http://www.xjmu.edu.cn/");
// OR
//        File f = new File(getTmpDir(), this.getClass().getName() +
//        ".html");
//        if (f.exists()) {
//        	uuri = UURIFactory.getInstance("file://" +
//        			f.getAbsolutePath());
//        }
// OR 
//      uuri = getUURI(URL or PATH)
//
// OR 
//      Use the main method below and pass this class an argument.
//     
        if (uuri != null) {
        	runExtractor(uuri);
        }
    }
    
    protected UURI getUURI(String url) throws URIException {
        url = (url.indexOf("://") > 0)? url: "file://" + url;
        return UURIFactory.getInstance(url);
    }
    
    protected void runExtractor(UURI baseUURI)
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException, IOException {
        runExtractor(baseUURI, null);
    }
    
    protected void runExtractor(UURI baseUURI, String encoding)
    throws IOException, InvalidAttributeValueException,
    AttributeNotFoundException, MBeanException, ReflectionException {
        if (baseUURI == null) {
        	return;
        }
        this.extractor = createExtractor();
        URL url = new URL(baseUURI.toString());
        this.recorder = HttpRecorder.
            wrapInputStreamWithHttpRecord(getTmpDir(),
            this.getClass().getName(), url.openStream(), encoding);
        CrawlURI curi = setupCrawlURI(this.recorder, url.toString());
        this.extractor.innerProcess(curi);
        
        System.out.println("+" + this.extractor.report());
        int count = 0; 
        Collection<Link> links = curi.getOutLinks();
        System.out.println("+HTML Links (hopType="+Link.NAVLINK_HOP+"):");
        if (links != null) {
            for (Iterator<Link> i = links.iterator(); i.hasNext();) {
                Link link = (Link)i.next();
                if (link.getHopType()==Link.NAVLINK_HOP) {
                    count++;
                    System.out.println(link.getDestination());
                }
            }
        }
        System.out.println("+HTML Embeds (hopType="+Link.EMBED_HOP+"):");
        if (links != null) {
            for (Iterator<Link> i = links.iterator(); i.hasNext();) {
                Link link = (Link)i.next();
                if (link.getHopType()==Link.EMBED_HOP) {
                    count++;
                    System.out.println(link.getDestination());
                }
            }
        }
        System.out.
            println("+HTML Speculative Embeds (hopType="+Link.SPECULATIVE_HOP+"):");
        if (links != null) {
            for (Iterator<Link> i = links.iterator(); i.hasNext();) {
                Link link = (Link)i.next();
                if (link.getHopType()==Link.SPECULATIVE_HOP) {
                    count++;
                    System.out.println(link.getDestination());
                }
            }
        }
        System.out.
            println("+HTML Other (all other hopTypes):");
        if (links != null) {
            for (Iterator<Link> i = links.iterator(); i.hasNext();) {
                Link link = (Link) i.next();
                if (link.getHopType() != Link.SPECULATIVE_HOP
                        && link.getHopType() != Link.NAVLINK_HOP
                        && link.getHopType() != Link.EMBED_HOP) {
                    count++;
                    System.out.println(link.getHopType() + " "
                            + link.getDestination());
                }
            }
        }
        System.out.println("TOTAL URIS EXTRACTED: "+count);
    }

    /**
     * Test a particular <embed src=...> construct that was suspicious in
     * the No10GovUk crawl.
     *
     * @throws URIException
     */
    public void testEmbedSrc() throws URIException {
        CrawlURI curi=
            new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        // An example from http://www.records.pro.gov.uk/documents/prem/18/1/default.asp?PageId=62&qt=true
        CharSequence cs = "<embed src=\"/documents/prem/18/1/graphics/qtvr/" +
            "hall.mov\" width=\"320\" height=\"212\" controller=\"true\" " +
            "CORRECTION=\"FULL\" pluginspage=\"http://www.apple.com/" +
            "quicktime/download/\" /> ";
        this.extractor.extract(curi,cs);
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/documents/prem/18/1/graphics/qtvr/hall.mov")>=0;
            }
        }));
    }
    
    /**
     * Test a whitespace issue found in href.
     * 
     * See [ 963965 ] Either UURI or ExtractHTML should strip whitespace better.
     * https://sourceforge.net/tracker/?func=detail&atid=539099&aid=963965&group_id=73833
     *
     * @throws URIException
     */
    public void testHrefWhitespace() throws URIException {
        CrawlURI curi =
            new CrawlURI(UURIFactory.getInstance("http://www.carsound.dk"));
        CharSequence cs = "<a href=\"http://www.carsound.dk\n\n\n" +
        	"\"\ntarget=\"_blank\">C.A.R. Sound\n\n\n\n</a>";   
        this.extractor.extract(curi,cs);
        curi.getOutLinks();
        assertTrue("Not stripping new lines", CollectionUtils.exists(curi
                .getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "http://www.carsound.dk/")>=0;
            }
        }));
    }
    
    /**
     * Test a missing whitespace issue found in form
     * 
     * [HER-1128] ExtractorHTML fails to extract FRAME SRC link without
     * whitespace before SRC http://webteam.archive.org/jira/browse/HER-1128
     */
    public void testNoWhitespaceBeforeValidAttribute() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com"));
        CharSequence cs = "<frame name=\"main\"src=\"http://www.example.com/\"> ";
        this.extractor.extract(curi, cs);
        Link[] links = curi.getOutLinks().toArray(new Link[0]);
        assertTrue("no links found",links.length==1);
        assertTrue("expected link not found", 
                links[0].getDestination().toString().equals("http://www.example.com/"));
    }
    
    /**
     * Test only extract FORM ACTIONS with METHOD GET 
     * 
     * [HER-1280] do not by default GET form action URLs declared as POST, 
     * because it can cause problems/complaints 
     * http://webteam.archive.org/jira/browse/HER-1280
     */
    public void testOnlyExtractFormGets() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com"));
        CharSequence cs = 
            "<form method=\"get\" action=\"http://www.example.com/ok1\"> "+
            "<form action=\"http://www.example.com/ok2\" method=\"get\"> "+
            "<form method=\"post\" action=\"http://www.example.com/notok\"> "+
            "<form action=\"http://www.example.com/ok3\"> ";
        this.extractor.extract(curi, cs);
        Link[] links = curi.getOutLinks().toArray(new Link[0]);
        assertTrue("incorrect number of links found",links.length==3);
    }
    
    /**
     * Test that relative URIs with late colons aren't misinterpreted
     * as absolute URIs with long, illegal scheme components. 
     * 
     * See http://webteam.archive.org/jira/browse/HER-1268
     * 
     * @throws URIException
     */
    public void testBadRelativeLinks() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com"));
        CharSequence cs = "<a href=\"example.html;jsessionid=deadbeef:deadbeed?parameter=this:value\"/>"
                + "<a href=\"example.html?parameter=this:value\"/>";
        this.extractor.extract(curi, cs);

        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object)
                        .getDestination()
                        .toString()
                        .indexOf(
                                "/example.html;jsessionid=deadbeef:deadbeed?parameter=this:value") >= 0;
            }
        }));

        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/example.html?parameter=this:value") >= 0;
            }
        }));
    }
    
    /**
     * Test if scheme is maintained by speculative hops onto exact 
     * same host
     * 
     * [HER-1524] speculativeFixup in ExtractorJS should maintain URL scheme
     */
    public void testSpeculativeLinkExtraction() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("https://www.example.com"));
        CharSequence cs = 
            "<script type=\"text/javascript\">_parameter=\"www.anotherexample.com\";"
                + "_anotherparameter=\"www.example.com/index.html\""
                + ";</script>";
        this.extractor.extract(curi, cs);

        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().equals(
                        "http://www.anotherexample.com/");
            }
        }));
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().equals(
                        "https://www.example.com/index.html");
            }
        }));
    }    
    
    /**
     * test to see if embedded <SCRIPT/> which writes script TYPE
     * creates any outlinks, e.g. "type='text/javascript'". 
     * 
     * [HER-1526] SCRIPT writing script TYPE common trigger of bogus links 
     *   (eg. 'text/javascript')
     *   
     * @throws URIException
     */
    public void testScriptTagWritingScriptType() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com/en/fiche/dossier/322/"));
        CharSequence cs = 
            "<script type=\"text/javascript\">"
            + "var gaJsHost = ((\"https:\" == document.location.protocol) "
            + "? \"https://ssl.\" : \"http://www.\");"
            + "document.write(unescape(\"%3Cscript src='\" + gaJsHost + "
            + "\"google-analytics.com/ga.js' "
            + "type='text/javascript'%3E%3C/script%3E\"));"
            + "</script>";
        this.extractor.extract(curi, cs);
        assertTrue("outlinks should be empty",curi.getOutLinks().isEmpty());    
    }
    
    protected Predicate destinationContainsPredicate(final String fragment) {
        return new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(fragment) >= 0;
            }
        };
    }
    
    protected Predicate destinationsIsPredicate(final String value) {
        return new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().equals(value);
            }
        };
    }
    
    /**
     * HER-1728 
     * @throws URIException 
     */
    public void testFlashvarsParamValue() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance("http://www.example.com/"));
        CharSequence cs = 
            "<object classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" codebase=\"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,28,0\" id=\"ZoomifySlideshowViewer\" height=\"372\" width=\"590\">\n" + 
            "    <param name=\"flashvars\" value=\"zoomifyXMLPath=ParamZoomifySlideshowViewer.xml\">\n" + 
            "    <param name=\"menu\" value=\"false\">\n" + 
            "    <param name=\"bgcolor\" value=\"#000000\">\n" + 
            "    <param name=\"src\" value=\"ZoomifySlideshowViewer.swf\">\n" + 
            "    <embed flashvars=\"zoomifyXMLPath=EmbedZoomifySlideshowViewer.xml\" src=\"ZoomifySlideshowViewer.swf\" menu=\"false\" bgcolor=\"#000000\" pluginspage=\"http://www.adobe.com/go/getflashplayer\" type=\"application/x-shockwave-flash\" name=\"ZoomifySlideshowViewer\" height=\"372\" width=\"590\">\n" + 
            "</object> ";
        this.extractor.extract(curi, cs);
        String expected = "http://www.example.com/ParamZoomifySlideshowViewer.xml";
        assertTrue("outlinks should contain: "+expected,
                CollectionUtils.exists(curi.getOutLinks(),destinationsIsPredicate(expected)));
    }
    
    /**
     * HER-1728 
     * @throws URIException 
     */
    public void testFlashvarsEmbedAttribute() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance("http://www.example.com/"));
        CharSequence cs = 
            "<object classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" codebase=\"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,28,0\" id=\"ZoomifySlideshowViewer\" height=\"372\" width=\"590\">\n" + 
            "    <param name=\"flashvars\" value=\"zoomifyXMLPath=ParamZoomifySlideshowViewer.xml\">\n" + 
            "    <param name=\"menu\" value=\"false\">\n" + 
            "    <param name=\"bgcolor\" value=\"#000000\">\n" + 
            "    <param name=\"src\" value=\"ZoomifySlideshowViewer.swf\">\n" + 
            "    <embed flashvars=\"zoomifyXMLPath=EmbedZoomifySlideshowViewer.xml\" src=\"ZoomifySlideshowViewer.swf\" menu=\"false\" bgcolor=\"#000000\" pluginspage=\"http://www.adobe.com/go/getflashplayer\" type=\"application/x-shockwave-flash\" name=\"ZoomifySlideshowViewer\" height=\"372\" width=\"590\">\n" + 
            "</object> ";
        this.extractor.extract(curi, cs);
        String expected = "http://www.example.com/EmbedZoomifySlideshowViewer.xml";
        assertTrue("outlinks should contain: "+expected,
                CollectionUtils.exists(curi.getOutLinks(),destinationsIsPredicate(expected)));
    }
    
    /**
     * False test: tries to verify extractor ignores a 'longDesc'
     * attribute. In fact, HTML spec says longDesc is a URI, so 
     * crawler should find 2 links here. 
     * See [HER-206]
     * @throws URIException
     */
    public void xestAvoidBadSpec() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com"));
        CharSequence cs = 
            "<TBODY>\r\n" + 
            "<TR>\r\n" + 
            "<TD><IMG height=259 alt=\"Operation Overlord Commanders\"\r\n" + 
            "src=\"/img/aboutus/history/dday60/commanders.jpg\"\r\n" + 
            "width=500 longDesc=\"Overlord Commanders, Back row, left\r\n" + 
            "to right:<BR>Lieutenant General Bradley, Admiral\r\n" + 
            "Ramsay, Air Chief Marshal Leigh-Mallory, General Bedell\r\n" + 
            "Smith.<BR>Front row, left to right: Air Chief Marshal\r\n" + 
            "Tedder, General Eisenhower, General Montgomery.\"></TD></TR>\r\n" + 
            "<TR>\r\n" + 
            "<TD class=caption>�Overlord� Commanders, Back row, left\r\n" + 
            "to right:<BR>Lieutenant General Bradley, Admiral\r\n" + 
            "Ramsay, Air Chief Marshal Leigh-Mallory, General Bedell\r\n" + 
            "Smith.<BR>Front row, left to right: Air Chief Marshal\r\n" + 
            "Tedder, General Eisenhower, General\r\n" + 
            "Montgomery.</TD></TR></TBODY></TABLE>\r\n" + 
            "<P>\r\n" + 
            "<TABLE id=imageinset width=\"35%\" align=right\r\n" + 
            "summary=\"Key Facts About the Allied Forces Deployed on\r\n" + 
            "D-Day\" border=0>\r\n" + 
            "<TBODY>";
        this.extractor.extract(curi, cs);
        Link[] links = curi.getOutLinks().toArray(new Link[0]);
        assertTrue("incorrect number of links found",links.length==1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: " + ExtractorHTMLTest.class.getName() +
                " URL|PATH [ENCODING]");
            System.exit(1);
        }
        ExtractorHTMLTest testCase = new ExtractorHTMLTest();
        testCase.setUp();
        try {
            testCase.runExtractor(testCase.getUURI(args[0]),
                (args.length == 2)? args[1]: null);
        } finally {
            testCase.tearDown();
        }
    }
}
