/* JerichoExtractorHTMLTest
 *
 * Copyright (C) 2006 Olaf Freyer
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
 * 
 * 
 */
package org.archive.crawler.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.management.Attribute;
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


/**
 * Test html extractor.
 *
 * @author stack
 * @version $Revision: 5757 $, $Date: 2008-02-06 07:44:20 +0000 (Wed, 06 Feb 2008) $
 */
public class JerichoExtractorHTMLTest
extends ExtractorHTMLTest
implements CoreAttributeConstants {
    private final String ARCHIVE_DOT_ORG = "archive.org";
    private final String LINK_TO_FIND = "http://www.hewlett.org/";
    private HttpRecorder recorder = null;
    private JerichoExtractorHTML extractor = null;
    
    protected JerichoExtractorHTML createExtractor()
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
        return (JerichoExtractorHTML)((MapType)handler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(handler.
                getSettingsObject(null), new JerichoExtractorHTML(name));
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


    public void testInnerProcess() throws IOException {
        UURI uuri = UURIFactory.getInstance("http://" + this.ARCHIVE_DOT_ORG);
        CrawlURI curi = setupCrawlURI(this.recorder, uuri.toString());
        this.extractor.innerProcess(curi);
        Collection links = curi.getOutLinks();
        boolean foundLinkToHewlettFoundation = false;
        for (Iterator i = links.iterator(); i.hasNext();) {
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
     * Test a GET FORM ACTION extraction
     * 
     * @throws URIException
     */
    public void testFormsLinkGet() throws URIException {
        CrawlURI curi =
            new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        CharSequence cs = 
        	"<form name=\"testform\" method=\"GET\" action=\"redirect_me?form=true\"> " +
        	"  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> "+
        	"  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " +
        	"  <select name=\"selectBox\">" +
        	"    <option value=\"selectedOption\" selected>option1</option>" +
        	"    <option value=\"nonselectedOption\">option2</option>" +
        	"  </select>" +
        	"  <input type=\"submit\" name=\"test\" value=\"Go\">" +
        	"</form>";   
        this.extractor.extract(curi,cs);
        curi.getOutLinks();
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go")>=0;
            }
        }));
    }
    
    /**
     * Test a POST FORM ACTION being properly ignored 
     * 
     * @throws URIException
     */
    public void testFormsLinkIgnorePost() throws URIException {
        CrawlURI curi =
            new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        CharSequence cs = 
            "<form name=\"testform\" method=\"POST\" action=\"redirect_me?form=true\"> " +
            "  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> "+
            "  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " +
            "  <select name=\"selectBox\">" +
            "    <option value=\"selectedOption\" selected>option1</option>" +
            "    <option value=\"nonselectedOption\">option2</option>" +
            "  </select>" +
            "  <input type=\"submit\" name=\"test\" value=\"Go\">" +
            "</form>";   
        this.extractor.extract(curi,cs);
        curi.getOutLinks();
        assertTrue(! CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go")>=0;
            }
        }));
    }
    
    /**
     * Test a POST FORM ACTION being found with non-default setting
     * 
     * @throws URIException
     * @throws ReflectionException 
     * @throws MBeanException 
     * @throws InvalidAttributeValueException 
     * @throws AttributeNotFoundException 
     */
    public void testFormsLinkFindPost() throws URIException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        CrawlURI curi =
            new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        CharSequence cs = 
            "<form name=\"testform\" method=\"POST\" action=\"redirect_me?form=true\"> " +
            "  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> "+
            "  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " +
            "  <select name=\"selectBox\">" +
            "    <option value=\"selectedOption\" selected>option1</option>" +
            "    <option value=\"nonselectedOption\">option2</option>" +
            "  </select>" +
            "  <input type=\"submit\" name=\"test\" value=\"Go\">" +
            "</form>";
        this.extractor.setAttribute(
                new Attribute(ExtractorHTML.ATTR_EXTRACT_ONLY_FORM_GETS,false));
        this.extractor.extract(curi,cs);
        curi.getOutLinks();
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go")>=0;
            }
        }));
    }
    
    public void testMultipleAttributesPerElement() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com"));
        CharSequence cs = "<a src=\"http://www.example.com/\" href=\"http://www.archive.org/\"> ";
        this.extractor.extract(curi, cs);
        Link[] links = curi.getOutLinks().toArray(new Link[0]);
        assertTrue("not all links found", links.length == 2);
    }
}
