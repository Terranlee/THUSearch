/* Copyright (C) 2003 Internet Archive.
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
 * FetchDNS
 * Created on Jun 5, 2003
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/crawler/fetcher/FetchDNS.java,v 1.29.4.1 2007/01/13 01:31:17 stack-sf Exp $
 */
package org.archive.crawler.fetcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;
import org.archive.util.InetAddressUtil;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


/**
 * Processor to resolve 'dns:' URIs.
 * 
 * TODO: Refactor to use org.archive.util.DNSJavaUtils.
 *
 * @author multiple
 */
public class FetchDNS extends Processor
implements CoreAttributeConstants, FetchStatusCodes {
	private static final long serialVersionUID = 4686199203459704426L;

	private Logger logger = Logger.getLogger(this.getClass().getName());

    // Defaults.
    private short ClassType = DClass.IN;
    private short TypeType = Type.A;
    protected InetAddress serverInetAddr = null;
    
    private static final String ATTR_ACCEPT_NON_DNS_RESOLVES =
        "accept-non-dns-resolves";
    private static final Boolean DEFAULT_ACCEPT_NON_DNS_RESOLVES =
        Boolean.FALSE;
    private static final long DEFAULT_TTL_FOR_NON_DNS_RESOLVES
        = 6 * 60 * 60; // 6 hrs
    
    private byte [] reusableBuffer = new byte[1024];

    /** 
     * Create a new instance of FetchDNS.
     *
     * @param name the name of this attribute.
     */
    public FetchDNS(String name) {
        super(name, "DNS Fetcher. Handles DNS lookups.");
        org.archive.crawler.settings.Type e =
            addElementToDefinition(new SimpleType(ATTR_ACCEPT_NON_DNS_RESOLVES,
                "If a DNS lookup fails, whether or not to fallback to " +
                "InetAddress resolution, which may use local 'hosts' files " +
                "or other mechanisms.", DEFAULT_ACCEPT_NON_DNS_RESOLVES));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(FetchHTTP.ATTR_DIGEST_CONTENT,
                FetchHTTP.DESC_DIGEST_CONTENT, FetchHTTP.DEFAULT_DIGEST_CONTENT));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(
                FetchHTTP.ATTR_DIGEST_ALGORITHM, 
                FetchHTTP.DESC_DIGEST_ALGORITHM,
                FetchHTTP.DEFAULT_DIGEST_ALGORITHM,
                FetchHTTP.DIGEST_ALGORITHMS));
        e.setExpertSetting(true);
    }

    protected void innerProcess(CrawlURI curi) {
        if (!curi.getUURI().getScheme().equals("dns")) {
            // Only handles dns
            return;
        }
        Record[] rrecordSet = null; // Retrieved dns records
        String dnsName = null;
        try {
            dnsName = curi.getUURI().getReferencedHost();
        } catch (URIException e) {
            logger.log(Level.SEVERE, "Failed parse of dns record " + curi, e);
        }
        
        if(dnsName == null) {
            curi.setFetchStatus(S_UNFETCHABLE_URI);
            return;
        }

        // Make sure we're in "normal operating mode", e.g. a cache +
        // controller exist to assist us.
        CrawlHost targetHost = null;
        if (getController() != null &&
                getController().getServerCache() != null) {
            targetHost = getController().getServerCache().getHostFor(dnsName);
        } else {
            // Standalone operation (mostly for test cases/potential other uses)
            targetHost = new CrawlHost(dnsName);
        }
        if (isQuadAddress(curi, dnsName, targetHost)) {
        	// We're done processing.
        	return;
        }
        
        // Do actual DNS lookup.
        curi.putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());

        // Try to get the records for this host (assume domain name)
        // TODO: Bug #935119 concerns potential hang here
        try {
            rrecordSet = (new Lookup(dnsName, TypeType, ClassType)).run();
        } catch (TextParseException e) {
            rrecordSet = null;
        }
        curi.setContentType("text/dns");
        if (rrecordSet != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Found recordset for " + dnsName);
            }
        	storeDNSRecord(curi, dnsName, targetHost, rrecordSet);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Failed find of recordset for " + dnsName);
            }
            if (((Boolean)getUncheckedAttribute(null,
                    ATTR_ACCEPT_NON_DNS_RESOLVES)).booleanValue()) {
                // Do lookup that bypasses javadns.
                InetAddress address = null;
                try {
                    address = InetAddress.getByName(dnsName);
                } catch (UnknownHostException e1) {
                    address = null;
                }
                if (address != null) {
                    targetHost.setIP(address, DEFAULT_TTL_FOR_NON_DNS_RESOLVES);
                    curi.setFetchStatus(S_GETBYNAME_SUCCESS);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Found address for " + dnsName +
                            " using native dns.");
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Failed find of address for " + dnsName +
                            " using native dns.");
                    }
                    setUnresolvable(curi, targetHost);
                }
            } else {
                setUnresolvable(curi, targetHost);
            }
        }
        curi.putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
    }
    
    protected void storeDNSRecord(final CrawlURI curi, final String dnsName,
    		final CrawlHost targetHost, final Record[] rrecordSet) {
        // Get TTL and IP info from the first A record (there may be
        // multiple, e.g. www.washington.edu) then update the CrawlServer
        ARecord arecord = getFirstARecord(rrecordSet);
        if (arecord == null) {
            throw new NullPointerException("Got null arecord for " +
                dnsName);
        }
        targetHost.setIP(arecord.getAddress(), arecord.getTTL());
        try {
        	recordDNS(curi, rrecordSet);
            curi.setFetchStatus(S_DNS_SUCCESS);
            curi.putString(A_DNS_SERVER_IP_LABEL, ResolverConfig.getCurrentConfig().server());
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Failed store of DNS Record for " +
        		curi.toString(), e);
        	setUnresolvable(curi, targetHost);
        }
    }
    
    protected boolean isQuadAddress(final CrawlURI curi, final String dnsName,
			final CrawlHost targetHost) {
		boolean result = false;
		Matcher matcher = InetAddressUtil.IPV4_QUADS.matcher(dnsName);
		// If it's an ip no need to do a lookup
		if (matcher == null || !matcher.matches()) {
			return result;
		}
		
		result = true;
		// Ideally this branch would never be reached: no CrawlURI
		// would be created for numerical IPs
		if (logger.isLoggable(Level.WARNING)) {
			logger.warning("Unnecessary DNS CrawlURI created: " + curi);
		}
		try {
			targetHost.setIP(InetAddress.getByAddress(dnsName, new byte[] {
					(byte) (new Integer(matcher.group(1)).intValue()),
					(byte) (new Integer(matcher.group(2)).intValue()),
					(byte) (new Integer(matcher.group(3)).intValue()),
					(byte) (new Integer(matcher.group(4)).intValue()) }),
					CrawlHost.IP_NEVER_EXPIRES); // Never expire numeric IPs
			curi.setFetchStatus(S_DNS_SUCCESS);
		} catch (UnknownHostException e) {
			logger.log(Level.SEVERE, "Should never be " + e.getMessage(), e);
			setUnresolvable(curi, targetHost);
		}
		return result;
	}
    
    protected void recordDNS(final CrawlURI curi, final Record[] rrecordSet)
	throws IOException {
		final byte[] dnsRecord =
			getDNSRecord(curi.getLong(A_FETCH_BEGAN_TIME), rrecordSet);
		HttpRecorder rec = HttpRecorder.getHttpRecorder();
        
        // Shall we get a digest on the content downloaded?
		boolean digestContent  = ((Boolean)getUncheckedAttribute(curi,
                FetchHTTP.ATTR_DIGEST_CONTENT)).booleanValue();
        String algorithm = null; 
        if (digestContent) {
            algorithm = ((String)getUncheckedAttribute(curi,
                FetchHTTP.ATTR_DIGEST_ALGORITHM));
            rec.getRecordedInput().setDigest(algorithm);
        } else {
            // clear
            rec.getRecordedInput().setDigest((MessageDigest)null);
        }
        
		curi.setHttpRecorder(rec);
		InputStream is = curi.getHttpRecorder().inputWrap(
				new ByteArrayInputStream(dnsRecord));
        if(digestContent) {
            rec.getRecordedInput().startDigest();
        }
		// Reading from the wrapped stream, behind the scenes, will write
		// files into scratch space
		try {
			while (is.read(this.reusableBuffer) != -1) {
				continue;
			}
		} finally {
			is.close();
			rec.closeRecorders();
		}
		curi.setContentSize(dnsRecord.length);
        if (digestContent) {
            curi.setContentDigest(algorithm,
                rec.getRecordedInput().getDigestValue());
        }
	}
    
    protected byte [] getDNSRecord(final long fetchStart,
    		final Record[] rrecordSet)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Start the record with a 14-digit date per RFC 2540
        byte[] fetchDate = ArchiveUtils.get14DigitDate(fetchStart).getBytes();
        baos.write(fetchDate);
        // Don't forget the newline
        baos.write("\n".getBytes());
        int recordLength = fetchDate.length + 1;
        if (rrecordSet != null) {
            for (int i = 0; i < rrecordSet.length; i++) {
                byte[] record = rrecordSet[i].toString().getBytes();
                recordLength += record.length;
                baos.write(record);
                // Add the newline between records back in
                baos.write("\n".getBytes());
                recordLength += 1;
            }
        }
        return baos.toByteArray();
    }
    
    protected void setUnresolvable(CrawlURI curi, CrawlHost host) {
        host.setIP(null, 0);
        curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE); 
    }
    
    protected ARecord getFirstARecord(Record[] rrecordSet) {
        ARecord arecord = null;
        if (rrecordSet == null || rrecordSet.length == 0) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("rrecordSet is null or zero length: " +
                    rrecordSet);
            }
            return arecord;
        }
        for (int i = 0; i < rrecordSet.length; i++) {
            if (rrecordSet[i].getType() != Type.A) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("Record " + Integer.toString(i) +
                        " is not A type but " + rrecordSet[i].getType());
                }
                continue;
            }
            arecord = (ARecord) rrecordSet[i];
            break;
        }
        return arecord;
    }
}