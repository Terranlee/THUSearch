/* DecideRuleSequenceTest
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

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.filter.ContentTypeRegExpFilter;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.SurtPrefixSet;
import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 * @version $Date: 2007-04-06 01:13:26 +0000 (Fri, 06 Apr 2007) $, $Revision: 5041 $
 */
public class DecideRuleSequenceTest extends TmpDirTestCase {
    /**
     * Gets setup by {@link #setUp()}.
     */
    private DecideRuleSequence rule = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        final String name = this.getClass().getName();
        SettingsHandler settingsHandler = new XMLSettingsHandler(
            new File(getTmpDir(), name + ".order.xml"));
        settingsHandler.initialize();
        // Create a new ConfigureDecideRule instance and add it to a MapType
        // (I can change MapTypes after instantiation).  The chosen MapType
        // is the rules canonicalization rules list.
        this.rule = (DecideRuleSequence)((MapType)settingsHandler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(settingsHandler.
                getSettingsObject(null), new DecideRuleSequence(name));
    }
    
    public void testEmptySequence() {
        Object decision = this.rule.decisionFor("test");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
    }
    
    public void testSingleACCEPT() throws InvalidAttributeValueException {
        Object decision = addDecideRule(new AcceptDecideRule("ACCEPT")).
            decisionFor("test");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testSingleREJECT() throws InvalidAttributeValueException {
        Object decision = addDecideRule(new RejectDecideRule("REJECT")).
            decisionFor("test");
        assertTrue("Expect REJECT but got " + decision,
                decision == DecideRule.REJECT);
    }
    
    public void testSinglePASS() throws InvalidAttributeValueException {
        Object decision = addDecideRule(new DecideRule("PASS")).
            decisionFor("test");
        assertTrue("Expect PASS but got " + decision,
                decision == DecideRule.PASS);
    }
    
    
    public void testACCEPTWins() throws InvalidAttributeValueException {
        addDecideRule(new DecideRule("PASS1"));
        addDecideRule(new RejectDecideRule("REJECT1"));
        addDecideRule(new DecideRule("PASS2"));
        addDecideRule(new AcceptDecideRule("ACCEPT1"));
        addDecideRule(new RejectDecideRule("REJECT2"));
        addDecideRule(new DecideRule("PASS3"));
        addDecideRule(new AcceptDecideRule("ACCEPT2"));
        addDecideRule(new DecideRule("PASS4"));
        Object decision = this.rule.decisionFor("test");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testREJECTWins() throws InvalidAttributeValueException {
        addDecideRule(new DecideRule("PASS1"));
        addDecideRule(new RejectDecideRule("REJECT1"));
        addDecideRule(new DecideRule("PASS2"));
        addDecideRule(new AcceptDecideRule("ACCEPT1"));
        addDecideRule(new RejectDecideRule("REJECT2"));
        addDecideRule(new DecideRule("PASS3"));
        addDecideRule(new AcceptDecideRule("ACCEPT2"));
        addDecideRule(new DecideRule("PASS4"));
        addDecideRule(new RejectDecideRule("REJECT3"));
        Object decision = this.rule.decisionFor("test");
        assertTrue("Expect REJECT but got " + decision,
            decision == DecideRule.REJECT);
    }
        
    public void testRegex()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        final String regexName = "REGEX";
        DecideRule r = addDecideRule(new MatchesRegExpDecideRule(regexName));
        // Set regex to be match anything that ends in archive.org.
        r.setAttribute(new Attribute(MatchesRegExpDecideRule.ATTR_REGEXP,
            "^.*\\.archive\\.org"));
        Object decision = this.rule.decisionFor("http://google.com");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
        decision = this.rule.decisionFor("http://archive.org");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
        decision = this.rule.decisionFor("http://www.archive.org");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testNotRegex()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        final String regexName = "NOT_REGEX";
        DecideRule r = addDecideRule(new NotMatchesRegExpDecideRule(regexName));
        // Set regex to be match anything that ends in archive.org.
        r.setAttribute(new Attribute(MatchesRegExpDecideRule.ATTR_REGEXP,
            "^.*\\.archive\\.org"));
        Object decision = this.rule.decisionFor("http://google.com");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
        decision = this.rule.decisionFor("http://www.archive.org");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
    }
    
    
    public void testPrerequisite()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new PrerequisiteAcceptDecideRule("PREREQUISITE"));
        UURI uuri = UURIFactory.getInstance("http://archive.org");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
        candidate = new CandidateURI(uuri, "LLP", null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testHops()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new TooManyHopsDecideRule("HOPS"));
        testHopLimit(TooManyHopsDecideRule.DEFAULT_MAX_HOPS.intValue(), 'L',
            DecideRule.PASS, DecideRule.REJECT);
    }
    
    public void testTransclusion()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new TransclusionDecideRule("TRANSCLUSION"));
        final int max =
            TransclusionDecideRule.DEFAULT_MAX_TRANS_HOPS.intValue();
        final char pathExpansion = 'E';
        UURI uuri = UURIFactory.getInstance("http://archive.org");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        StringBuffer path = new StringBuffer(max);
        for (int i = 0; i < (max - 1); i++) {
            path.append(pathExpansion);
        }
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
        String pathCopy = path.toString();
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        candidate = new CandidateURI(uuri, pathCopy + 'L', null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
    }
    
    public void testPathologicalPath()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new PathologicalPathDecideRule("PATHOLOGICAL"));
        final int max =
            PathologicalPathDecideRule.DEFAULT_REPETITIONS.intValue();
        String uri = "http://archive.org/";
        final String segment = "abc/";
        for (int i = 1; i < max; i++) {
            uri = uri + segment;
        }
        final String baseUri = uri;
        UURI uuri = UURIFactory.getInstance(uri);
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        uuri = UURIFactory.getInstance(baseUri + segment);
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        uuri = UURIFactory.getInstance(baseUri + segment + segment);
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.REJECT + " but got " + decision,
            decision == DecideRule.REJECT);
    }
    
    public void testTooManyPathSegments()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new TooManyPathSegmentsDecideRule("SEGMENTS"));
        final int max =
            TooManyPathSegmentsDecideRule.DEFAULT_MAX_PATH_DEPTH.intValue();
        StringBuffer baseUri = new StringBuffer("http://archive.org");
        for (int i = 0; i < max; i++) {
            baseUri.append('/');
            baseUri.append(Integer.toString(i + 1));
        }
        UURI uuri = UURIFactory.getInstance(baseUri.toString());
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        baseUri.append("/x");
        uuri = UURIFactory.getInstance(baseUri.toString());
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.REJECT + " but got " + decision,
            decision == DecideRule.REJECT);
    }
    
    public void testMatchesFilePattern()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new MatchesFilePatternDecideRule("FILE_PATTERN"));
        StringBuffer baseUri = new StringBuffer("http://archive.org/");
        UURI uuri = UURIFactory.getInstance(baseUri.toString() + "ms.doc");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
        uuri = UURIFactory.getInstance(baseUri.toString() + "index.html");
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
    }
    
    public void testNotMatchesFilePattern()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new NotMatchesFilePatternDecideRule("NOT_FILE_PATTERN"));
        StringBuffer baseUri = new StringBuffer("http://archive.org/");
        UURI uuri = UURIFactory.getInstance(baseUri.toString() + "ms.doc");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        uuri = UURIFactory.getInstance(baseUri.toString() + "index.html");
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    protected void testHopLimit(final int max, final char pathExpansion,
        final String defaultDecision, final String overLimitDecision)
    throws URIException {
        UURI uuri = UURIFactory.getInstance("http://archive.org");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + defaultDecision + " but got " + decision,
            decision == defaultDecision);
        StringBuffer path = new StringBuffer(max);
        for (int i = 0; i < (max - 1); i++) {
            path.append(pathExpansion);
        }
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + defaultDecision + " but got " + decision,
            decision == defaultDecision);
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + defaultDecision + " but got " + decision,
            decision == defaultDecision);
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + overLimitDecision + " but got " + decision,
            decision == overLimitDecision);       
    }
          
    public void testScopePlusOne() 
                throws URIException, InvalidAttributeValueException, 
                AttributeNotFoundException, MBeanException,
                ReflectionException {
        // first test host scope      
        ScopePlusOneDecideRule t = new ScopePlusOneDecideRule("host");
        SurtPrefixSet mSet = new SurtPrefixSet();
        mSet.add(SurtPrefixSet.prefixFromPlain("http://audio.archive.org"));
        mSet.convertAllPrefixesToHosts();
        t.surtPrefixes = mSet;
        DecideRule s = addDecideRule(t);
        s.setAttribute(new Attribute(ScopePlusOneDecideRule.ATTR_SCOPE,
            ScopePlusOneDecideRule.HOST));


        UURI uuri =
            UURIFactory.getInstance("http://audio.archive.org/examples");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("URI Expect " + DecideRule.ACCEPT + " for " + candidate +
            " but got " + decision, decision == DecideRule.ACCEPT);    
        UURI uuriOne = UURIFactory.getInstance("http://movies.archive.org");
        CandidateURI plusOne = new CandidateURI(uuriOne);
        plusOne.setVia(uuri);
        decision = this.rule.decisionFor(plusOne);
        assertTrue("PlusOne Expect " + DecideRule.ACCEPT + " for " + plusOne +
            " with via " + plusOne.flattenVia() + " but got " + decision,
            decision == DecideRule.ACCEPT);
        UURI uuriTwo = UURIFactory.getInstance("http://sloan.archive.org");
        CandidateURI plusTwo = new CandidateURI(uuriTwo);
        plusTwo.setVia(uuriOne);
        decision = this.rule.decisionFor(plusTwo);
        assertTrue("PlusTwo Expect " + DecideRule.PASS + " for " + plusTwo +
            " with via " + plusTwo.flattenVia() + " but got " + decision,
            decision == DecideRule.PASS);        
        

        //now test domain scope
        ScopePlusOneDecideRule u = new ScopePlusOneDecideRule("domain");
        SurtPrefixSet mSet1 = new SurtPrefixSet();
        mSet1.add(SurtPrefixSet.prefixFromPlain("archive.org"));
        mSet1.convertAllPrefixesToDomains();
        u.surtPrefixes = mSet1;
        DecideRule v = addDecideRule(u);
        v.setAttribute(new Attribute(ScopePlusOneDecideRule.ATTR_SCOPE,
            ScopePlusOneDecideRule.DOMAIN));
        
        decision = this.rule.decisionFor(candidate);
        assertTrue("Domain: URI Expect " + DecideRule.ACCEPT + " for " +
            candidate + " but got " + decision, decision == DecideRule.ACCEPT);    
        decision = this.rule.decisionFor(plusOne);
        assertTrue("Domain: PlusOne Expect " + DecideRule.ACCEPT + " for " +
            plusOne + " with via "  + plusOne.flattenVia() + " but got " +
            decision, decision == DecideRule.ACCEPT);
        decision = this.rule.decisionFor(plusTwo);
        assertTrue("Domain: PlusTwo Expect " + DecideRule.ACCEPT + " for " +
            plusTwo + " with via " + plusTwo.flattenVia() + " but got " +
            decision, decision == DecideRule.ACCEPT);        
        UURI uuriThree = UURIFactory.getInstance("http://sloan.org");
        CandidateURI plusThree = new CandidateURI(uuriThree);
        plusThree.setVia(uuriTwo);
        decision = this.rule.decisionFor(plusThree);
        assertTrue("Domain: PlusThree Expect " + DecideRule.ACCEPT + " for " +
            plusThree + " with via " + plusThree.flattenVia() + " but got " +
            decision, decision == DecideRule.ACCEPT);        
        UURI uuriFour = UURIFactory.getInstance("http://example.com");
        CandidateURI plusFour = new CandidateURI(uuriFour);
        plusFour.setVia(uuriThree);
        decision = this.rule.decisionFor(plusFour);                
        assertTrue("Domain: PlusFour Expect " + DecideRule.PASS + " for " +
            plusFour + " with via " + plusFour.flattenVia() + " but got " +
            decision, decision == DecideRule.PASS);        
    }     
    
    public void testFilter()
    throws InvalidAttributeValueException, URIException, AttributeNotFoundException, MBeanException, ReflectionException {
    	FilterDecideRule dr = new FilterDecideRule(
			"FilterDecideRule(ContentTypeRegExpFilter)");
        addDecideRule(dr);
        StringBuffer baseUri = new StringBuffer();
        UURI uuri = UURIFactory.getInstance("http://example.com/foo");
        CrawlURI curi = new CrawlURI(uuri);
        curi.setContentType("text/html");
        Object decision = this.rule.decisionFor(curi);
        // default for unconfigured FilterDecideRule is true from (empty)
        // filters, then ACCEPT because of true
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
        ContentTypeRegExpFilter filt = 
        	new ContentTypeRegExpFilter("ContentTypeRegExpFilter","app.*");
        dr.filters.addElement(null,filt);
        decision = this.rule.decisionFor(curi);
        // filter should now return false, making decision REJECT
        assertTrue("Expect " + DecideRule.REJECT + " but got " + decision,
            decision == DecideRule.REJECT);
        curi.setContentType("application/octet-stream");
        decision = this.rule.decisionFor(curi);
        // filter should now return true, making decision ACCEPT
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
                decision == DecideRule.ACCEPT);
        // change true answer to "PASS"; use String to simulate settings non-identity
        dr.setAttribute(new Attribute(FilterDecideRule.ATTR_TRUE_DECISION,"PASS"));
        decision = this.rule.decisionFor(curi);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
                decision == DecideRule.PASS);       
    }
    
    protected DecideRule addDecideRule(DecideRule dr)
    throws InvalidAttributeValueException {
        MapType rules = this.rule.getRules(null);
        rules.addElement(null, dr);
        return dr;
    }
    
    public void testContentTypeMatchesRegexpDecideRule() throws Exception{
        ContentTypeMatchesRegExpDecideRule dr = new ContentTypeMatchesRegExpDecideRule("CTMREDRtest");
        DecideRule v = addDecideRule(dr);
        
        v.setAttribute(new Attribute(MatchesRegExpDecideRule.ATTR_REGEXP,"text/html"));
        UURI uuri = UURIFactory.getInstance("http://www.archive.org");
        CrawlURI crawlUri = new CrawlURI(uuri);

        // no content type - let curi pass
        Object decision = this.rule.decisionFor(crawlUri);
        assertTrue("URI Expect " + DecideRule.PASS + " for " + crawlUri +
                " but got " + decision, decision == DecideRule.PASS);
        
        // non-matching content type - let curi pass
        crawlUri.setContentType("application/pdf");
        decision = this.rule.decisionFor(crawlUri);
        assertTrue("URI Expect " + DecideRule.PASS + " for " + crawlUri +
                " but got " + decision, decision == DecideRule.PASS);
          
        // matching content type - accept curi
        crawlUri.setContentType("text/html");
        decision = this.rule.decisionFor(crawlUri);
        assertTrue("URI Expect " + DecideRule.ACCEPT + " for " + crawlUri +
                " but got " + decision, decision == DecideRule.ACCEPT);
    }
    
    public void testContentTypeNotMatchesRegexpDecideRule() throws Exception{
        ContentTypeNotMatchesRegExpDecideRule dr = new ContentTypeNotMatchesRegExpDecideRule("CTNMREDRtest");
        DecideRule v = addDecideRule(dr);
        
        v.setAttribute(new Attribute(MatchesRegExpDecideRule.ATTR_REGEXP,"text/html"));
        UURI uuri = UURIFactory.getInstance("http://www.archive.org");
        CrawlURI crawlUri = new CrawlURI(uuri);

        // no content type - let curi pass
        Object decision = this.rule.decisionFor(crawlUri);
        assertTrue("URI Expect " + DecideRule.PASS + " for " + crawlUri +
                " but got " + decision, decision == DecideRule.PASS);
        
        // matching content type - let curi pass
        crawlUri.setContentType("text/html");
        decision = this.rule.decisionFor(crawlUri);
        assertTrue("URI Expect " + DecideRule.PASS + " for " + crawlUri +
                " but got " + decision, decision == DecideRule.PASS);
        
        // non-matching content type - accept curi
        crawlUri.setContentType("application/pdf");
        decision = this.rule.decisionFor(crawlUri);
        assertTrue("URI Expect " + DecideRule.ACCEPT + " for " + crawlUri +
                " but got " + decision, decision == DecideRule.ACCEPT);
    }
}
