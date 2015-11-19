/* $Id: ARCReader.java 6786 2010-03-10 00:42:08Z szznax $
 *
 * Created on May 1, 2004
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
package org.archive.io.arc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.RecoverableIOException;
import org.archive.io.WriterPoolMember;
import org.archive.util.ArchiveUtils;
import org.archive.util.InetAddressUtil;
import org.archive.util.TextUtils;


/**
 * Get an iterator on an ARC file or get a record by absolute position.
 *
 * ARC files are described here:
 * <a href="http://www.archive.org/web/researcher/ArcFileFormat.php">Arc
 * File Format</a>.
 *
 * <p>This class knows how to parse an ARC file.  Pass it a file path
 * or an URL to an ARC. It can parse ARC Version 1 and 2.
 *
 * <p>Iterator returns <code>ARCRecord</code>
 * though {@link Iterator#next()} is returning
 * java.lang.Object.  Cast the return.
 *
 * <p>Profiling java.io vs. memory-mapped ByteBufferInputStream shows the
 * latter slightly slower -- but not by much.  TODO: Test more.  Just
 * change {@link #getInputStream(File, long)}.
 *
 * @author stack
 * @version $Date: 2010-03-10 00:42:08 +0000 (Wed, 10 Mar 2010) $ $Revision: 6786 $
 */
public abstract class ARCReader extends ArchiveReader
implements ARCConstants {
    Logger logger = Logger.getLogger(ARCReader.class.getName());
    
    /**
     * Set to true if we are aligned on first record of Archive file.
     * We used depend on offset. If offset was zero, then we were
     * aligned on first record.  This is no longer necessarily the case when
     * Reader is created at an offset into an Archive file: The offset is zero
     * but its relative to where we started reading.
     */
    private boolean alignedOnFirstRecord = true;
    
    /**
     * Assumed maximum size of a record meta header line.
     *
     * This 100k which seems massive but its the same as the LINE_LENGTH from
     * <code>alexa/include/a_arcio.h</code>:
     * <pre>
     * #define LINE_LENGTH     (100*1024)
     * </pre>
     */
    private static final int MAX_HEADER_LINE_LENGTH = 1024 * 100;

    /**
     * An array of the header field names found in the ARC file header on
     * the 3rd line.
     * 
     * We used to read these in from the arc file first record 3rd line but
     * now we hardcode them for sake of improved performance.
     */
    public final static String [] HEADER_FIELD_NAME_KEYS = {
        URL_FIELD_KEY,
        IP_HEADER_FIELD_KEY,
        DATE_FIELD_KEY,
        MIMETYPE_FIELD_KEY,
        LENGTH_FIELD_KEY
    };
    
    private boolean parseHttpHeaders = true;
        
    ARCReader() {
    	super();
    }
    
    /**
     * Skip over any trailing new lines at end of the record so we're lined up
     * ready to read the next.
     * @param record
     * @throws IOException
     */
    protected void gotoEOR(ArchiveRecord record) throws IOException {
        if (getIn().available() <= 0) {
            return;
        }
        
        // Remove any trailing LINE_SEPARATOR
        int c = -1;
        while (getIn().available() > 0) {
            if (getIn().markSupported()) {
                getIn().mark(1);
            }
            c = getIn().read();
            if (c != -1) {
                if (c == LINE_SEPARATOR) {
                    continue;
                }
                if (getIn().markSupported()) {
                    // We've overread.  We're probably in next record.  There is
                    // no way of telling for sure. It may be dross at end of
                    // current record. Backup.
                	getIn().reset();
                    break;
                }
                ArchiveRecordHeader h = (getCurrentRecord() != null)?
                    record.getHeader(): null;
                throw new IOException("Read " + (char)c +
                    " when only " + LINE_SEPARATOR + " expected. " + 
                    getReaderIdentifier() + ((h != null)?
                        h.getHeaderFields().toString(): ""));
            }
        }
    }
    
    /**
     * Create new arc record.
     *
     * Encapsulate housekeeping that has to do w/ creating a new record.
     *
     * <p>Call this method at end of constructor to read in the
     * arcfile header.  Will be problems reading subsequent arc records
     * if you don't since arcfile header has the list of metadata fields for
     * all records that follow.
     * 
     * <p>When parsing through ARCs writing out CDX info, we spend about
     * 38% of CPU in here -- about 30% of which is in getTokenizedHeaderLine
     * -- of which 16% is reading.
     *
     * @param is InputStream to use.
     * @param offset Absolute offset into arc file.
     * @return An arc record.
     * @throws IOException
     */
    protected ARCRecord createArchiveRecord(InputStream is, long offset)
    throws IOException {
        ArrayList<String> firstLineValues = new ArrayList<String>(20);
        getTokenizedHeaderLine(is, firstLineValues);
        int bodyOffset = 0;
        if (offset == 0 && isAlignedOnFirstRecord()) {
            // If offset is zero and we were aligned at first record on
            // creation (See #alignedOnFirstRecord for more on this), then no
            // records have been read yet and we're reading our first one, the
            // record of ARC file meta info.  Its special.  In ARC versions
            // 1.x, first record has three lines of meta info. We've just read
            // the first line. There are two more.  The second line has misc.
            // info.  We're only interested in the first field, the version
            // number.  The third line is the list of field names. Here's what
            // ARC file version 1.x meta content looks like:
            //
            // filedesc://testIsBoundary-JunitIAH200401070157520.arc 0.0.0.0 \\
            //      20040107015752 text/plain 77
            // 1 0 InternetArchive
            // URL IP-address Archive-date Content-type Archive-length
            //
            ArrayList<String> secondLineValues = new ArrayList<String>(20);
            bodyOffset += getTokenizedHeaderLine(is, secondLineValues);
            setVersion((String)secondLineValues.get(0) +
                "." + (String)secondLineValues.get(1));
            // Just read over the 3rd line.  We used to parse it and use
            // values found here but now we just hardcode them to avoid
            // having to read this 3rd line even for random arc file accesses.
            bodyOffset += getTokenizedHeaderLine(is, null);
        }

        try {
            currentRecord(new ARCRecord(is, 
                    (ArchiveRecordHeader)computeMetaData(
                            Arrays.asList(HEADER_FIELD_NAME_KEYS),
                            firstLineValues, getVersion(), offset), 
                            bodyOffset, isDigest(), isStrict(), 
                            isParseHttpHeaders()));
        } catch (IOException e) {
            if (e instanceof RecoverableIOException) {
                // Don't mess with RecoverableIOExceptions.  Let them out.
                throw e;
            }
            IOException newE = new IOException(e.getMessage() + " (Offset " +
                    offset + ").");
            newE.setStackTrace(e.getStackTrace());
            throw newE;
        }
        return (ARCRecord)getCurrentRecord();
    }
    
    /**
     * Returns version of this ARC file.  Usually read from first record of ARC.
     * If we're reading without having first read the first record -- e.g.
     * random access into middle of an ARC -- then version will not have been
     * set.  For now, we return a default, version 1.1.  Later, if more than
     * just one version of ARC, we could look at such as the meta line to see
     * what version of ARC this is.
     * @return Version of this ARC file.
     */
    public String getVersion() {
        return (super.getVersion() == null)? "1.1": super.getVersion();
    }

    /**
     * Get a record header line as list of tokens.
     *
     * We keep reading till we find a LINE_SEPARATOR or we reach the end
     * of file w/o finding a LINE_SEPARATOR or the line length is crazy.
     *
     * @param stream InputStream to read from.
     * @param list Empty list that gets filled w/ string tokens.
     * @return Count of characters read.
     * @exception IOException If problem reading stream or no line separator
     * found or EOF before EOL or we didn't get minimum header fields.
     */
    private int getTokenizedHeaderLine(final InputStream stream,
            List<String> list) throws IOException {
        // Preallocate usual line size.
        StringBuilder buffer = new StringBuilder(2048 + 20);
        int read = 0;
        int previous = -1;
        for (int c = -1; true;) {
        	previous = c;
            c = stream.read();
            if (c == -1) {
                throw new RecoverableIOException("Hit EOF before header EOL.");
            }
            c &= 0xff; 
            read++;
            if (read > MAX_HEADER_LINE_LENGTH) {
                throw new IOException("Header line longer than max allowed " +
                    " -- " + String.valueOf(MAX_HEADER_LINE_LENGTH) +
                    " -- or passed buffer doesn't contain a line (Read: " +
                    buffer.length() + ").  Here's" +
                    " some of what was read: " +
                    buffer.substring(0, Math.min(buffer.length(), 256)));
            }

            if (c == LINE_SEPARATOR) {
                if (buffer.length() == 0) {
                    // Empty line at start of buffer.  Skip it and try again.
                    continue;
                }

                if (list != null) {
                    list.add(buffer.toString());
                }
                // LOOP TERMINATION.
                break;
            } else if (c == HEADER_FIELD_SEPARATOR) {
            	if (!isStrict() && previous == HEADER_FIELD_SEPARATOR) {
            		// Early ARCs sometimes had multiple spaces between fields.
            		continue;
            	}
                if (list != null) {
                    list.add(buffer.toString());
                }
                // reset to empty
                buffer.setLength(0);
            } else {
                buffer.append((char)c);
            }
        }

        // List must have at least 3 elements in it and no more than 10.  If
        // it has other than this, then bogus parse.
        if (list != null && (list.size() < 3 || list.size() > 100)) {
            throw new IOException("Unparseable header line: " + list);
        }

        return read;
    }

    /**
     * Compute metadata fields.
     *
     * Here we check the meta field has right number of items in it.
     *
     * @param keys Keys to use composing headerFields map.
     * @param values Values to set into the headerFields map.
     * @param v The version of this ARC file.
     * @param offset Offset into arc file.
     *
     * @return Metadata structure for this record.
     *
     * @exception IOException  If no. of keys doesn't match no. of values.
     */
    private ARCRecordMetaData computeMetaData(List<String> keys,
    		List<String> values, String v, long offset)
    throws IOException {
        if (keys.size() != values.size()) {
            List<String> originalValues = values;
            if (!isStrict()) {
                values = fixSpaceInURL(values, keys.size());
                // If values still doesn't match key size, try and do
                // further repair.
	            if (keys.size() != values.size()) {
	            	// Early ARCs had a space in mimetype.
	            	if (values.size() == (keys.size() + 1) &&
	            			values.get(4).toLowerCase().startsWith("charset=")) {
	            		List<String> nuvalues =
	            			new ArrayList<String>(keys.size());
	            		nuvalues.add(0, values.get(0));
	            		nuvalues.add(1, values.get(1));
	            		nuvalues.add(2, values.get(2));
	            		nuvalues.add(3, values.get(3) + values.get(4));
	            		nuvalues.add(4, values.get(5));
	            		values = nuvalues;
	            	} else if((values.size() + 1) == keys.size() &&
                            isLegitimateIPValue(values.get(1)) &&
                            isDate(values.get(2)) && isNumber(values.get(3))) {
                        // Mimetype is empty.
                        List<String> nuvalues =
                            new ArrayList<String>(keys.size());
                        nuvalues.add(0, values.get(0));
                        nuvalues.add(1, values.get(1));
                        nuvalues.add(2, values.get(2));
                        nuvalues.add(3, "-");
                        nuvalues.add(4, values.get(3));
                        values = nuvalues;
                    }
	            }
        	}
            if (keys.size() != values.size()) {
                throw new IOException("Size of field name keys does" +
                    " not match count of field values: " + values);
            }
            // Note that field was fixed on stderr.
            logStdErr(Level.WARNING, "Fixed spaces in metadata line at " +
            	"offset " + offset +
                " Original: " + originalValues + ", New: " + values);
        }
        
        Map<Object, Object> headerFields =
        	new HashMap<Object, Object>(keys.size() + 2);
        for (int i = 0; i < keys.size(); i++) {
            headerFields.put(keys.get(i), values.get(i));
        }
        
        // Add a check for tabs in URLs.  If any, replace with '%09'.
        // See https://sourceforge.net/tracker/?group_id=73833&atid=539099&func=detail&aid=1010966,
        // [ 1010966 ] crawl.log has URIs with spaces in them.
        String url = (String)headerFields.get(URL_FIELD_KEY);
        if (url != null && url.indexOf('\t') >= 0) {
            headerFields.put(URL_FIELD_KEY,
                TextUtils.replaceAll("\t", url, "%09"));
        }

        headerFields.put(VERSION_FIELD_KEY, v);
        headerFields.put(ABSOLUTE_OFFSET_KEY, new  Long(offset));

        return new ARCRecordMetaData(getReaderIdentifier(), headerFields);
    }
    
    protected boolean isDate(final String date) {
        if (date.length() != 14) {
            return false;
        }
        return isNumber(date);
    }
    
    protected boolean isNumber(final String n) {
        for (int i = 0; i < n.length(); i++) {
            if (!Character.isDigit(n.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    protected boolean isLegitimateIPValue(final String ip) {
        if ("-".equals(ip)) {
            return true;
        }
        Matcher m = InetAddressUtil.IPV4_QUADS.matcher(ip);
        return m != null && m.matches();
    }
    
    /**
     * Fix space in URLs.
     * The ARCWriter used to write into the ARC URLs with spaces in them.
     * See <a
     * href="https://sourceforge.net/tracker/?group_id=73833&atid=539099&func=detail&aid=1010966">[ 1010966 ]
     * crawl.log has URIs with spaces in them</a>.
     * This method does fix up on such headers converting all spaces found
     * to '%20'.
     * @param values List of metadata values.
     * @param requiredSize Expected size of resultant values list.
     * @return New list if we successfully fixed up values or original if
     * fixup failed.
     */
    protected List<String> fixSpaceInURL(List<String> values, int requiredSize) {
        // Do validity check. 3rd from last is a date of 14 numeric
        // characters. The 4th from last is IP, all before the IP
        // should be concatenated together with a '%20' joiner.
        // In the below, '4' is 4th field from end which has the IP.
        if (!(values.size() > requiredSize) || values.size() < 4) {
            return values;
        }
        // Test 3rd field is valid date.
        if (!isDate((String) values.get(values.size() - 3))) {
            return values;
        }

        // Test 4th field is valid IP.
        if (!isLegitimateIPValue((String) values.get(values.size() - 4))) {
            return values;
        }

        List<String> newValues = new ArrayList<String>(requiredSize);
        StringBuffer url = new StringBuffer();
        for (int i = 0; i < (values.size() - 4); i++) {
            if (i > 0) {
                url.append("%20");
            }
            url.append(values.get(i));
        }
        newValues.add(url.toString());
        for (int i = values.size() - 4; i < values.size(); i++) {
            newValues.add(values.get(i));
        }
        return newValues;
    }
    
	protected boolean isAlignedOnFirstRecord() {
		return alignedOnFirstRecord;
	}

	protected void setAlignedOnFirstRecord(boolean alignedOnFirstRecord) {
		this.alignedOnFirstRecord = alignedOnFirstRecord;
	}
	
    /**
     * @return Returns the parseHttpHeaders.
     */
    public boolean isParseHttpHeaders() {
        return this.parseHttpHeaders;
    }
    
    /**
     * @param parse The parseHttpHeaders to set.
     */
    public void setParseHttpHeaders(boolean parse) {
        this.parseHttpHeaders = parse;
    }
    
	public String getFileExtension() {
		return ARC_FILE_EXTENSION;
	}
	
	public String getDotFileExtension() {
		return DOT_ARC_FILE_EXTENSION;
	}
	
	protected boolean output(final String format) 
	throws IOException, java.text.ParseException {
		boolean result = super.output(format);
		if(!result && (format.equals(NOHEAD) || format.equals(HEADER))) {
			throw new IOException(format +
				" format only supported for single Records");
		}
		return result;
	}
    
    public boolean outputRecord(final String format) throws IOException {
		boolean result = super.outputRecord(format);
		if (result) {
			return result;
		}
		if (format.equals(NOHEAD)) {
			// No point digesting if dumping content.
			setDigest(false);
			ARCRecord r = (ARCRecord) get();
			r.skipHttpHeader();
			r.dump();
			result = true;
		} else if (format.equals(HEADER)) {
			// No point digesting if dumping content.
			setDigest(false);
			ARCRecord r = (ARCRecord) get();
			r.dumpHttpHeader();
			result = true;
		}

		return result;
	}

    public void dump(final boolean compress)
    throws IOException, java.text.ParseException {
        // No point digesting if we're doing a dump.
        setDigest(false);
        boolean firstRecord = true;
        ARCWriter writer = null;
        for (Iterator<ArchiveRecord> ii = iterator(); ii.hasNext();) {
            ARCRecord r = (ARCRecord)ii.next();
            // We're to dump the arc on stdout.
            // Get the first record's data if any.
            ARCRecordMetaData meta = r.getMetaData();
            if (firstRecord) {
                firstRecord = false;
                // Get an ARCWriter.
                ByteArrayOutputStream baos =
                    new ByteArrayOutputStream(r.available());
                // This is slow but done only once at top of ARC.
                while (r.available() > 0) {
                    baos.write(r.read());
                }
                List<String> listOfMetadata = new ArrayList<String>();
                listOfMetadata.add(baos.toString(WriterPoolMember.UTF8));
                // Assume getArc returns full path to file.  ARCWriter
                // or new File will complain if it is otherwise.
                writer = new ARCWriter(new AtomicInteger(), System.out,
                    new File(meta.getArc()),
                    compress, meta.getDate(), listOfMetadata);
                continue;
            }
            
            writer.write(meta.getUrl(), meta.getMimetype(), meta.getIp(),
                ArchiveUtils.parse14DigitDate(meta.getDate()).getTime(),
                (int)meta.getLength(), r);
        }
        // System.out.println(System.currentTimeMillis() - start);
    }
    
    /**
     * @return an ArchiveReader that will delete a local file on close.  Used
     * when we bring Archive files local and need to clean up afterward.
     */
    public ARCReader getDeleteFileOnCloseReader(final File f) {
        final ARCReader d = this;
        return new ARCReader() {
            private final ARCReader delegate = d;
            private File archiveFile = f;
            
            public void close() throws IOException {
                this.delegate.close();
                if (this.archiveFile != null) {
                    if (archiveFile.exists()) {
                        archiveFile.delete();
                    }
                    this.archiveFile = null;
                }
            }
            
            public ArchiveRecord get(long o) throws IOException {
                return this.delegate.get(o);
            }
            
            public boolean isDigest() {
                return this.delegate.isDigest();
            }
            
            public boolean isStrict() {
                return this.delegate.isStrict();
            }
            
            public Iterator<ArchiveRecord> iterator() {
                return this.delegate.iterator();
            }
            
            public void setDigest(boolean d) {
                this.delegate.setDigest(d);
            }
            
            public void setStrict(boolean s) {
                this.delegate.setStrict(s);
            }
            
            public List validate() throws IOException {
                return this.delegate.validate();
            }

            @Override
            public ArchiveRecord get() throws IOException {
                return this.delegate.get();
            }

            @Override
            public String getVersion() {
                return this.delegate.getVersion();
            }

            @Override
            public List validate(int noRecords) throws IOException {
                return this.delegate.validate(noRecords);
            }

            @Override
            protected ARCRecord createArchiveRecord(InputStream is,
                    long offset)
            throws IOException {
                return this.delegate.createArchiveRecord(is, offset);
            }

            @Override
            protected void gotoEOR(ArchiveRecord record) throws IOException {
                this.delegate.gotoEOR(record);
            }

            @Override
            public void dump(boolean compress)
            throws IOException, java.text.ParseException {
                this.delegate.dump(compress);
            }

            @Override
            public String getDotFileExtension() {
                return this.delegate.getDotFileExtension();
            }

            @Override
            public String getFileExtension() {
                return this.delegate.getFileExtension();
            }
        };
    }
    
    // Static methods follow.

    /**
     *
     * @param formatter Help formatter instance.
     * @param options Usage options.
     * @param exitCode Exit code.
     */
    private static void usage(HelpFormatter formatter, Options options,
            int exitCode) {
        formatter.printHelp("java org.archive.io.arc.ARCReader" +
            " [--digest=true|false] \\\n" +
            " [--format=cdx|cdxfile|dump|gzipdump|header|nohead]" +
            " [--offset=#] \\\n[--strict] [--parse] ARC_FILE|ARC_URL",
                options);
        System.exit(exitCode);
    }

    /**
     * Write out the arcfile.
     * 
     * @param reader
     * @param format Format to use outputting.
     * @throws IOException
     * @throws java.text.ParseException
     */
    protected static void output(ARCReader reader, String format)
    throws IOException, java.text.ParseException {
    	if (!reader.output(format)) {
            throw new IOException("Unsupported format: " + format);
    	}
    }

    /**
     * Generate a CDX index file for an ARC file.
     *
     * @param urlOrPath The ARC file to generate a CDX index for
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void createCDXIndexFile(String urlOrPath)
    throws IOException, java.text.ParseException {
    	ARCReader r = ARCReaderFactory.get(urlOrPath);
    	r.setStrict(false);
    	r.setParseHttpHeaders(true);
    	r.setDigest(true);
    	output(r, CDX_FILE);
    }

    /**
     * Command-line interface to ARCReader.
     *
     * Here is the command-line interface:
     * <pre>
     * usage: java org.archive.io.arc.ARCReader [--offset=#] ARCFILE
     *  -h,--help      Prints this message and exits.
     *  -o,--offset    Outputs record at this offset into arc file.</pre>
     *
     * <p>See in <code>$HERITRIX_HOME/bin/arcreader</code> for a script that'll
     * take care of classpaths and the calling of ARCReader.
     *
     * <p>Outputs using a pseudo-CDX format as described here:
     * <a href="http://www.archive.org/web/researcher/cdx_legend.php">CDX
     * Legent</a> and here
     * <a href="http://www.archive.org/web/researcher/example_cdx.php">Example</a>.
     * Legend used in below is: 'CDX b e a m s c V (or v if uncompressed) n g'.
     * Hash is hard-coded straight SHA-1 hash of content.
     *
     * @param args Command-line arguments.
     * @throws ParseException Failed parse of the command line.
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void main(String [] args)
    throws ParseException, IOException, java.text.ParseException {
        Options options = getOptions();
        options.addOption(new Option("p","parse", false, "Parse headers."));
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
        long offset = -1;
        boolean digest = false;
        boolean strict = false;
        boolean parse = false;
        String format = CDX;
        for (int i = 0; i < cmdlineOptions.length; i++) {
            switch(cmdlineOptions[i].getId()) {
                case 'h':
                    usage(formatter, options, 0);
                    break;

                case 'o':
                    offset =
                        Long.parseLong(cmdlineOptions[i].getValue());
                    break;
                    
                case 's':
                    strict = true;
                    break;
                    
                case 'p':
                	parse = true;
                    break;
                    
                case 'd':
                	digest = getTrueOrFalse(cmdlineOptions[i].getValue());
                    break;
                    
                case 'f':
                    format = cmdlineOptions[i].getValue().toLowerCase();
                    boolean match = false;
                    // List of supported formats.
                    final String [] supportedFormats =
                		{CDX, DUMP, GZIP_DUMP, HEADER, NOHEAD, CDX_FILE};
                    for (int ii = 0; ii < supportedFormats.length; ii++) {
                        if (supportedFormats[ii].equals(format)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        usage(formatter, options, 1);
                    }
                    break;

                default:
                    throw new RuntimeException("Unexpected option: " +
                        + cmdlineOptions[i].getId());
            }
        }
        
        if (offset >= 0) {
            if (cmdlineArgs.size() != 1) {
                System.out.println("Error: Pass one arcfile only.");
                usage(formatter, options, 1);
            }
            ARCReader arc = ARCReaderFactory.get((String)cmdlineArgs.get(0),
            	offset);
            arc.setStrict(strict);
            // We must parse headers if we need to skip them.
            if (format.equals(NOHEAD) || format.equals(HEADER)) {
                parse = true;
            }
            arc.setParseHttpHeaders(parse);
            outputRecord(arc, format);
        } else {
            for (Iterator i = cmdlineArgs.iterator(); i.hasNext();) {
                String urlOrPath = (String)i.next();
                try {
                	ARCReader r = ARCReaderFactory.get(urlOrPath);
                	r.setStrict(strict);
                	r.setParseHttpHeaders(parse);
                	r.setDigest(digest);
                    output(r, format);
                } catch (RuntimeException e) {
                    // Write out name of file we failed on to help with
                    // debugging.  Then print stack trace and try to keep
                    // going.  We do this for case where we're being fed
                    // a bunch of ARCs; just note the bad one and move
                    // on to the next.
                    System.err.println("Exception processing " + urlOrPath +
                        ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        }
    }
}
