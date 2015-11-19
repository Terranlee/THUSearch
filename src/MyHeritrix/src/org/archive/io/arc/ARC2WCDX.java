/*
 * ARC2WCDX.java
 *
 * $Id: ARC2WCDX.java 4903 2007-02-16 01:45:10Z gojomo $
 *
 * Created on Nov 13, 2006
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.io.arc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.archive.util.ArchiveUtils;
import org.archive.util.SURT;

/**
 * Create a 'Wide' CDX from an ARC. Takes one argument, the path to the ARC.
 * Writes .wcdx.gz in same directory.
 *
 * @author gojomo
 */
public class ARC2WCDX {
    final public static String WCDX_VERSION="0.1";

    public static void main(String[] args) throws IOException {
        String arcFilename = args[0];
        createWcdx(arcFilename);
    }

    public static Object[] createWcdx(String arcFilename) throws IOException {
        ARCReader reader = ARCReaderFactory.get(arcFilename);
        Object[] retVal = createWcdx(reader);
        reader.close();
        return retVal; 
    }

    public static Object[] createWcdx(ARCReader reader) {
        reader.setDigest(true);

        String wcdxPath = reader.getReaderIdentifier().replaceAll("\\.arc(\\.gz)?$",".wcdx.gz");
        File wcdxFile = new File(wcdxPath+".open");
        PrintStream writer = null;
        long count = 0;
        try {
            writer = new PrintStream(new GZIPOutputStream(new FileOutputStream(wcdxFile)));
            
            // write header: legend + timestamp
            StringBuilder legend = new StringBuilder();
            appendField(legend,"CDX");
            appendField(legend,"surt-uri");
            appendField(legend,"b"); // ARC timestamp
            appendField(legend,"http-date");
            appendField(legend,"s"); // status code
            appendField(legend,"m"); // media type
            appendField(legend,"sha1"); // content sha1
            appendField(legend,"g"); // ARC name
            appendField(legend,"V"); // start offset
            appendField(legend,"end-offset"); // TODO: implement
            appendField(legend,"n"); // ARC record length TODO: verify
            appendField(legend,"http-content-length");
            appendField(legend,"http-last-modified");
            appendField(legend,"http-expires");
            appendField(legend,"http-etag");
            appendField(legend,"http-location");
            appendField(legend,"e"); // IP
            appendField(legend,"a"); // original URL
            // WCDX version+creation time: crude version control
            appendField(legend,WCDX_VERSION+"@"+ArchiveUtils.get14DigitDate());
            writer.println(legend.toString());

            Iterator iter = reader.iterator();
            count = 0; 
            while(iter.hasNext()) {
                ARCRecord record = (ARCRecord) iter.next();
                record.close();
                ARCRecordMetaData h = (ARCRecordMetaData) record.getHeader();
                Header[] httpHeaders = record.getHttpHeaders();
                if(httpHeaders==null) {
                    httpHeaders = new Header[0];
                }
                HeaderGroup hg = new HeaderGroup();
                hg.setHeaders(httpHeaders);
                StringBuilder builder = new StringBuilder();

                // SURT-form URI
                appendField(builder,SURT.fromURI(h.getUrl()));
                // record timestamp ('b')
                appendField(builder,h.getDate());
                // http header date
                appendTimeField(builder,hg.getFirstHeader("Date"));
                // response code ('s')
                appendField(builder,h.getStatusCode());
                // media type ('m')
                appendField(builder,h.getMimetype());
                // content checksum (like 'c', but here Base32 SHA1)
                appendField(builder,record.getDigestStr());
                // arc name ('g')
                appendField(builder,reader.getFileName());
                // compressed start offset ('V')
                appendField(builder,h.getOffset());

                // compressed end offset (?)
//            appendField(builder,
//                    reader.getInputStream() instanceof RepositionableStream
//                    ? ((GzippedInputStream)reader.getInputStream()).vPosition()
//                    : "-");
                // TODO; leave unavail for now
                appendField(builder, "-");

                // uncompressed (declared in ARC headerline) record length
                appendField(builder,h.getLength());
                // http header content-length
                appendField(builder,hg.getFirstHeader("Content-Length"));

                // http header mod-date
                appendTimeField(builder,hg.getFirstHeader("Last-Modified"));
                // http header expires
                appendTimeField(builder,hg.getFirstHeader("Expires"));
                
                // http header etag
                appendField(builder,hg.getFirstHeader("ETag"));
                // http header redirect ('Location' header?)
                appendField(builder,hg.getFirstHeader("Location"));
                // ip ('e')
                appendField(builder,h.getIp());
                // original URI
                appendField(builder,h.getUrl());
                // TODO MAYBE - a title from inside content? 

                writer.println(builder.toString());
                count++;
            }
            wcdxFile.renameTo(new File(wcdxPath));
        } catch (IOException e) {
            // soldier on: but leave '.open' wcdx file as indicator of error
            if(!wcdxFile.exists()) {
                try {
                    wcdxFile.createNewFile();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e1);
                }
            }
        } catch (RuntimeException e) {
            // soldier on: but leave '.open' wcdx file as indicator of error
            if(!wcdxFile.exists()) {
                try {
                    wcdxFile.createNewFile();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e1);
                }
            }
        } finally {
            if(writer!=null) {
                writer.close();
            }
        }
        
        return new Object[] {wcdxPath, count};
    }

    protected static void appendField(StringBuilder builder, Object obj) {
        if(builder.length()>0) {
            // prepend with delimiter
            builder.append(' ');
        }
        if(obj instanceof Header) {
            obj = ((Header)obj).getValue().trim();
        }

        builder.append((obj==null||obj.toString().length()==0)?"-":obj);
    }

    protected static void appendTimeField(StringBuilder builder, Object obj) {
        if(builder.length()>0) {
            // prepend with delimiter
            builder.append(' ');
        }
        if(obj==null) {
            builder.append("-");
            return;
        }
        if(obj instanceof Header) {
            String s = ((Header)obj).getValue().trim();
            try {
                Date date = DateUtil.parseDate(s);
                String d = ArchiveUtils.get14DigitDate(date);
                if(d.startsWith("209")) {
                    d = "199"+d.substring(3);
                }
                obj = d;
            } catch (DateParseException e) {
                builder.append('e');
                return;
            }

        }
        builder.append(obj);
    }
}

//'wide' CDX
//a original url
//b timestamp
//s resp code
//m type
//? content md5 (full 'k'? 'c'?
//g arc name
//V compressed start offset
//? compressed length
//n? uncompressed length
//? mod date
//? expires
//? server 'date' hdr
//? etag
//r redirect ('Location'?)
//e ip
//MAYBE: 
//? TITLE from HTML or other format?


