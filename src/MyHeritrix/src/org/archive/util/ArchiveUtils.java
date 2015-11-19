/*
 * ArchiveUtils
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/util/ArchiveUtils.java,v 1.38 2007/01/23 00:29:48 gojomo Exp $
 *
 * Created on Jul 7, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

/**
 * Miscellaneous useful methods.
 *
 * @contributor gojomo & others
 */
public class ArchiveUtils {
    private static final Logger LOGGER = Logger.getLogger(ArchiveUtils.class.getName());

    /**
     * Arc-style date stamp in the format yyyyMMddHHmm and UTC time zone.
     */
    private static final ThreadLocal<SimpleDateFormat> 
        TIMESTAMP12 = threadLocalDateFormat("yyyyMMddHHmm");;
    
    /**
     * Arc-style date stamp in the format yyyyMMddHHmmss and UTC time zone.
     */
    private static final ThreadLocal<SimpleDateFormat> 
       TIMESTAMP14 = threadLocalDateFormat("yyyyMMddHHmmss");
    /**
     * Arc-style date stamp in the format yyyyMMddHHmmssSSS and UTC time zone.
     */
    private static final ThreadLocal<SimpleDateFormat> 
        TIMESTAMP17 = threadLocalDateFormat("yyyyMMddHHmmssSSS");

    /**
     * Log-style date stamp in the format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * UTC time zone is assumed.
     */
    private static final ThreadLocal<SimpleDateFormat> 
        TIMESTAMP17ISO8601Z = threadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    /**
     * Log-style date stamp in the format yyyy-MM-dd'T'HH:mm:ss'Z'
     * UTC time zone is assumed.
     */
    private static final ThreadLocal<SimpleDateFormat>
        TIMESTAMP14ISO8601Z = threadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    /**
     * Default character to use padding strings.
     */
    private static final char DEFAULT_PAD_CHAR = ' ';

    /** milliseconds in an hour */ 
    private static final int HOUR_IN_MS = 60 * 60 * 1000;
    /** milliseconds in a day */
    private static final int DAY_IN_MS = 24 * HOUR_IN_MS;
    
    private static ThreadLocal<SimpleDateFormat> threadLocalDateFormat(final String pattern) {
        ThreadLocal<SimpleDateFormat> tl = new ThreadLocal<SimpleDateFormat>() {
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat df = new SimpleDateFormat(pattern);
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                return df;
            }
        };
        return tl;
    }
    
    public static int MAX_INT_CHAR_WIDTH =
        Integer.toString(Integer.MAX_VALUE).length();
    
    /**
     * Utility function for creating arc-style date stamps
     * in the format yyyMMddHHmmssSSS.
     * Date stamps are in the UTC time zone
     * @return the date stamp
     */
    public static String get17DigitDate(){
        return TIMESTAMP17.get().format(new Date());
    }

    /**
     * Utility function for creating arc-style date stamps
     * in the format yyyMMddHHmmss.
     * Date stamps are in the UTC time zone
     * @return the date stamp
     */
    public static String get14DigitDate(){
        return TIMESTAMP14.get().format(new Date());
    }

    /**
     * Utility function for creating arc-style date stamps
     * in the format yyyMMddHHmm.
     * Date stamps are in the UTC time zone
     * @return the date stamp
     */
    public static String get12DigitDate(){
        return TIMESTAMP12.get().format(new Date());
    }

    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. Use current time. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * 
     * @return the date stamp
     */
    public static String getLog17Date(){
        return TIMESTAMP17ISO8601Z.get().format(new Date());
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * @param date Date to format.
     * 
     * @return the date stamp
     */
    public static String getLog17Date(long date){
        return TIMESTAMP17ISO8601Z.get().format(new Date(date));
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. Use current time. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss'Z'
     * 
     * @return the date stamp
     */
    public static String getLog14Date(){
        return TIMESTAMP14ISO8601Z.get().format(new Date());
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss'Z'
     * @param date long timestamp to format.
     * 
     * @return the date stamp
     */
    public static String getLog14Date(long date){
        return TIMESTAMP14ISO8601Z.get().format(new Date(date));
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss'Z'
     * @param date Date to format.
     * 
     * @return the date stamp
     */
    public static String getLog14Date(Date date){
        return TIMESTAMP14ISO8601Z.get().format(date);
    }
    
    /**
     * Utility function for creating arc-style date stamps
     * in the format yyyyMMddHHmmssSSS.
     * Date stamps are in the UTC time zone
     *
     * @param date milliseconds since epoc
     * @return the date stamp
     */
    public static String get17DigitDate(long date){
        return TIMESTAMP17.get().format(new Date(date));
    }
    
    public static String get17DigitDate(Date date){
        return TIMESTAMP17.get().format(date);
    }

    /**
     * Utility function for creating arc-style date stamps
     * in the format yyyyMMddHHmmss.
     * Date stamps are in the UTC time zone
     *
     * @param date milliseconds since epoc
     * @return the date stamp
     */
    public static String get14DigitDate(long date){
        return TIMESTAMP14.get().format(new Date(date));
    }

    public static String get14DigitDate(Date d) {
        return TIMESTAMP14.get().format(d);
    }

    /**
     * Utility function for creating arc-style date stamps
     * in the format yyyyMMddHHmm.
     * Date stamps are in the UTC time zone
     *
     * @param date milliseconds since epoc
     * @return the date stamp
     */
    public static String get12DigitDate(long date){
        return TIMESTAMP12.get().format(new Date(date));
    }
    
    public static String get12DigitDate(Date d) {
        return TIMESTAMP12.get().format(d);
    }
    
    /**
     * Parses an ARC-style date.  If passed String is < 12 characters in length,
     * we pad.  At a minimum, String should contain a year (>=4 characters).
     * Parse will also fail if day or month are incompletely specified.  Depends
     * on the above getXXDigitDate methods.
     * @param A 4-17 digit date in ARC style (<code>yyyy</code> to
     * <code>yyyyMMddHHmmssSSS</code>) formatting.  
     * @return A Date object representing the passed String. 
     * @throws ParseException
     */
    public static Date getDate(String d) throws ParseException {
        Date date = null;
        if (d == null) {
            throw new IllegalArgumentException("Passed date is null");
        }
        switch (d.length()) {
        case 14:
            date = ArchiveUtils.parse14DigitDate(d);
            break;

        case 17:
            date = ArchiveUtils.parse17DigitDate(d);
            break;

        case 12:
            date = ArchiveUtils.parse12DigitDate(d);
            break;
           
        case 0:
        case 1:
        case 2:
        case 3:
            throw new ParseException("Date string must at least contain a" +
                "year: " + d, d.length());
            
        default:
            if (!(d.startsWith("19") || d.startsWith("20"))) {
                throw new ParseException("Unrecognized century: " + d, 0);
            }
            if (d.length() < 8 && (d.length() % 2) != 0) {
                throw new ParseException("Incomplete month/date: " + d,
                    d.length());
            }
            StringBuilder sb = new StringBuilder(d);
            if (sb.length() < 8) {
                for (int i = sb.length(); sb.length() < 8; i += 2) {
                    sb.append("01");
                }
            }
            if (sb.length() < 12) {
                for (int i = sb.length(); sb.length() < 12; i++) {
                    sb.append("0");
                }
            }
            date = ArchiveUtils.parse12DigitDate(sb.toString());
        }

        return date;
    }

    /**
     * Utility function for parsing arc-style date stamps
     * in the format yyyMMddHHmmssSSS.
     * Date stamps are in the UTC time zone.  The whole string will not be
     * parsed, only the first 17 digits.
     *
     * @param date an arc-style formatted date stamp
     * @return the Date corresponding to the date stamp string
     * @throws ParseException if the inputstring was malformed
     */
    public static Date parse17DigitDate(String date) throws ParseException {
        return TIMESTAMP17.get().parse(date);
    }

    /**
     * Utility function for parsing arc-style date stamps
     * in the format yyyMMddHHmmss.
     * Date stamps are in the UTC time zone.  The whole string will not be
     * parsed, only the first 14 digits.
     *
     * @param date an arc-style formatted date stamp
     * @return the Date corresponding to the date stamp string
     * @throws ParseException if the inputstring was malformed
     */
    public static Date parse14DigitDate(String date) throws ParseException{
        return TIMESTAMP14.get().parse(date);
    }

    /**
     * Utility function for parsing arc-style date stamps
     * in the format yyyMMddHHmm.
     * Date stamps are in the UTC time zone.  The whole string will not be
     * parsed, only the first 12 digits.
     *
     * @param date an arc-style formatted date stamp
     * @return the Date corresponding to the date stamp string
     * @throws ParseException if the inputstring was malformed
     */
    public static Date parse12DigitDate(String date) throws ParseException{
        return TIMESTAMP12.get().parse(date);
    }
    
    /**
     * Convert 17-digit date format timestamps (as found in crawl.log, for
     * example) into a GregorianCalendar object. + * Useful so you can convert
     * into milliseconds-since-epoch. Note: it is possible to compute
     * milliseconds-since-epoch + * using {@link #parse17DigitDate}.UTC(), but
     * that method is deprecated in favor of using Calendar.getTimeInMillis(). + *
     * <p/>I probably should have dug into all the utility methods in
     * DateFormat.java to parse the timestamp, but this was + * easier. If
     * someone wants to fix this to use those methods, please have at it! <p/>
     * Mike Schwartz, schwartz at CodeOnTheRoad dot com.
     * 
     * @param timestamp17String
     * @return Calendar set to <code>timestamp17String</code>.
     */
    public static Calendar timestamp17ToCalendar(String timestamp17String) {
        GregorianCalendar calendar = new GregorianCalendar();
        int year = Integer.parseInt(timestamp17String.substring(0, 4));
        int dayOfMonth = Integer.parseInt(timestamp17String.substring(6, 8));
        // Month is 0-based
        int month = Integer.parseInt(timestamp17String.substring(4, 6)) - 1;
        int hourOfDay = Integer.parseInt(timestamp17String.substring(8, 10));
        int minute = Integer.parseInt(timestamp17String.substring(10, 12));
        int second = Integer.parseInt(timestamp17String.substring(12, 14));
        int milliseconds = Integer
                .parseInt(timestamp17String.substring(14, 17));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, milliseconds);
        return calendar;
    }
    
    /**
     * @param timestamp A 14-digit timestamp or the suffix for a 14-digit
     * timestamp: E.g. '20010909014640' or '20010101' or '1970'.
     * @return Seconds since the epoch as a string zero-pre-padded so always
     * Integer.MAX_VALUE wide (Makes it so sorting of resultant string works
     * properly).
     * @throws ParseException 
     */
    public static String secondsSinceEpoch(String timestamp)
    throws ParseException {
        return zeroPadInteger((int)
            (getSecondsSinceEpoch(timestamp).getTime()/1000));
    }
    
    /**
     * @param timestamp A 14-digit timestamp or the suffix for a 14-digit
     * timestamp: E.g. '20010909014640' or '20010101' or '1970'.
     * @return A date.
     * @see #secondsSinceEpoch(String)
     * @throws ParseException 
     */
    public static Date getSecondsSinceEpoch(String timestamp)
    throws ParseException {
        if (timestamp.length() < 14) {
            if (timestamp.length() < 10 && (timestamp.length() % 2) == 1) {
                throw new IllegalArgumentException("Must have year, " +
                    "month, date, hour or second granularity: " + timestamp);
            }
            if (timestamp.length() == 4) {
                // Add first month and first date.
                timestamp = timestamp + "01010000";
            }
            if (timestamp.length() == 6) {
                // Add a date of the first.
                timestamp = timestamp + "010000";
            }
            if (timestamp.length() < 14) {
                timestamp = timestamp +
                    ArchiveUtils.padTo("", 14 - timestamp.length(), '0');
            }
        }
        return ArchiveUtils.parse14DigitDate(timestamp);
    }
    
    /**
     * @param i Integer to add prefix of zeros too.  If passed
     * 2005, will return the String <code>0000002005</code>. String
     * width is the width of Integer.MAX_VALUE as a string (10
     * digits).
     * @return Padded String version of <code>i</code>.
     */
    public static String zeroPadInteger(int i) {
        return ArchiveUtils.padTo(Integer.toString(i),
                MAX_INT_CHAR_WIDTH, '0');
    }

    /** 
     * Convert an <code>int</code> to a <code>String</code>, and pad it to
     * <code>pad</code> spaces.
     * @param i the int
     * @param pad the width to pad to.
     * @return String w/ padding.
     */
    public static String padTo(final int i, final int pad) {
        String n = Integer.toString(i);
        return padTo(n, pad);
    }
    
    /** 
     * Pad the given <code>String</code> to <code>pad</code> characters wide
     * by pre-pending spaces.  <code>s</code> should not be <code>null</code>.
     * If <code>s</code> is already wider than <code>pad</code> no change is
     * done.
     *
     * @param s the String to pad
     * @param pad the width to pad to.
     * @return String w/ padding.
     */
    public static String padTo(final String s, final int pad) {
        return padTo(s, pad, DEFAULT_PAD_CHAR);
    }

    /** 
     * Pad the given <code>String</code> to <code>pad</code> characters wide
     * by pre-pending <code>padChar</code>.
     * 
     * <code>s</code> should not be <code>null</code>. If <code>s</code> is
     * already wider than <code>pad</code> no change is done.
     *
     * @param s the String to pad
     * @param pad the width to pad to.
     * @param padChar The pad character to use.
     * @return String w/ padding.
     */
    public static String padTo(final String s, final int pad,
            final char padChar) {
        String result = s;
        int l = s.length();
        if (l < pad) {
            StringBuffer sb = new StringBuffer(pad);
            while(l < pad) {
                sb.append(padChar);
                l++;
            }
            sb.append(s);
            result = sb.toString();
        }
        return result;
    }

    /** check that two byte arrays are equal.  They may be <code>null</code>.
     *
     * @param lhs a byte array
     * @param rhs another byte array.
     * @return <code>true</code> if they are both equal (or both
     * <code>null</code>)
     */
    public static boolean byteArrayEquals(final byte[] lhs, final byte[] rhs) {
        if (lhs == null && rhs != null || lhs != null && rhs == null) {
            return false;
        }
        if (lhs==rhs) {
            return true;
        }
        if (lhs.length != rhs.length) {
            return false;
        }
        for(int i = 0; i<lhs.length; i++) {
            if (lhs[i]!=rhs[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a double to a string.
     * @param val The double to convert
     * @param precision How many characters to include after '.'
     * @return the double as a string.
     */
    public static String doubleToString(double val, int maxFractionDigits){
        return doubleToString(val, maxFractionDigits, 0);
    }

    private static String doubleToString(double val, int maxFractionDigits, int minFractionDigits) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.US); 
        f.setMaximumFractionDigits(maxFractionDigits);
        f.setMinimumFractionDigits(minFractionDigits);
        return f.format(val); 
    }

    /**
     * Takes a byte size and formats it for display with 'friendly' units. 
     * <p>
     * This involves converting it to the largest unit 
     * (of B, KB, MB, GB, TB) for which the amount will be > 1.
     * <p>
     * Additionally, at least 2 significant digits are always displayed. 
     * <p>
     * Displays as bytes (B): 0-1023
     * Displays as kilobytes (KB): 1024 - 2097151 (~2Mb)
     * Displays as megabytes (MB): 2097152 - 4294967295 (~4Gb)
     * Displays as gigabytes (GB): 4294967296 - infinity
     * <p>
     * Negative numbers will be returned as '0 B'.
     *
     * @param amount the amount of bytes
     * @return A string containing the amount, properly formated.
     */
    public static String formatBytesForDisplay(long amount) {
        double displayAmount = (double) amount;
        int unitPowerOf1024 = 0; 

        if(amount <= 0){
            return "0 B";
        }
        
        while(displayAmount>=1024 && unitPowerOf1024 < 4) {
            displayAmount = displayAmount / 1024;
            unitPowerOf1024++;
        }
        
        // TODO: get didactic, make these KiB, MiB, GiB, TiB
        final String[] units = { " B", " KB", " MB", " GB", " TB" };
        
        // ensure at least 2 significant digits (#.#) for small displayValues
        int fractionDigits = (displayAmount < 10) ? 1 : 0; 
        return doubleToString(displayAmount, fractionDigits, fractionDigits) 
                   + units[unitPowerOf1024];
    }

    /**
     * Convert milliseconds value to a human-readable duration
     * @param time
     * @return Human readable string version of passed <code>time</code>
     */
    public static String formatMillisecondsToConventional(long time) {
        return formatMillisecondsToConventional(time,true);
    }
    
    /**
     * Convert milliseconds value to a human-readable duration
     * @param time
     * @param toMs whether to print to the ms
     * @return Human readable string version of passed <code>time</code>
     */
    public static String formatMillisecondsToConventional(long time, boolean toMs) {
        StringBuffer sb = new StringBuffer();
        if(time<0) {
            sb.append("-");
        }
        long absTime = Math.abs(time);
        if(!toMs && absTime < 1000) {
            return "0s";
        }
        if(absTime > DAY_IN_MS) {
            // days
            sb.append(absTime / DAY_IN_MS + "d");
            absTime = absTime % DAY_IN_MS;
        }
        if (absTime > HOUR_IN_MS) {
            //got hours.
            sb.append(absTime / HOUR_IN_MS + "h");
            absTime = absTime % HOUR_IN_MS;
        }
        if (absTime > 60000) {
            sb.append(absTime / 60000 + "m");
            absTime = absTime % 60000;
        }
        if (absTime > 1000) {
            sb.append(absTime / 1000 + "s");
            absTime = absTime % 1000;
        }
        if(toMs) {
            sb.append(absTime + "ms");
        }
        return sb.toString();
    }


    /**
     * Generate a long UID based on the given class and version number.
     * Using this instead of the default will assume serialization
     * compatibility across class changes unless version number is
     * intentionally bumped.
     *
     * @param class1
     * @param version
     * @return UID based off class and version number.
     */
    public static long classnameBasedUID(Class<?> class1, int version) {
        String callingClassname = class1.getName();
        return (long)callingClassname.hashCode() << 32 + version;
    }
    
    /**
     * Copy the raw bytes of a long into a byte array, starting at
     * the specified offset.
     * 
     * @param l
     * @param array
     * @param offset
     */
    public static void longIntoByteArray(long l, byte[] array, int offset) {
        int i, shift;
                  
        for(i = 0, shift = 56; i < 8; i++, shift -= 8)
        array[offset+i] = (byte)(0xFF & (l >> shift));
    }
    
    public static long byteArrayIntoLong(byte [] bytearray) {
        return byteArrayIntoLong(bytearray, 0);
    }
    
    /**
     * Byte array into long.
     * @param bytearray Array to convert to a long.
     * @param offset Offset into array at which we start decoding the long.
     * @return Long made of the bytes of <code>array</code> beginning at
     * offset <code>offset</code>.
     * @see #longIntoByteArray(long, byte[], int)
     */
    public static long byteArrayIntoLong(byte [] bytearray,
            int offset) {
        long result = 0;
        for (int i = offset; i < 8 /*Bytes in long*/; i++) {
            result = (result << 8 /*Bits in byte*/) |
                (0xff & (byte)(bytearray[i] & 0xff));
        }
        return result;
    }

    /**
     * Given a string that may be a plain host or host/path (without
     * URI scheme), add an implied http:// if necessary. 
     * 
     * @param u string to evaluate
     * @return string with http:// added if no scheme already present
     */
    public static String addImpliedHttpIfNecessary(String u) {
        if(u.indexOf(':') == -1 || u.indexOf('.') < u.indexOf(':')) {
            // No scheme present; prepend "http://"
            u = "http://" + u;
        }
        return u;
    }

    /**
     * Verify that the array begins with the prefix. 
     * 
     * @param array
     * @param prefix
     * @return true if array is identical to prefix for the first prefix.length
     * positions 
     */
    public static boolean startsWith(byte[] array, byte[] prefix) {
        if(prefix.length>array.length) {
            return false;
        }
        for(int i = 0; i < prefix.length; i++) {
            if(array[i]!=prefix[i]) {
                return false; 
            }
        }
        return true; 
    }

    /**
     * Utility method to get a String singleLineReport from Reporter
     * @param rep  Reporter to get singleLineReport from
     * @return String of report
     */
    public static String singleLineReport(Reporter rep) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            rep.singleLineReportTo(pw);
        } catch (IOException e) {
            // not really possible
            e.printStackTrace();
        }
        pw.flush();
        return sw.toString();
    }

    /**
     * Compose the requested report into a String. DANGEROUS IF REPORT
     * CAN BE LARGE.
     * 
     * @param rep Reported
     * @param name String name of report to compose
     * @return String of report
     */
    public static String writeReportToString(Reporter rep, String name) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        rep.reportTo(name,pw);
        pw.flush();
        return sw.toString();
    }
    
    public static Set<String> TLDS;
    
    static {
        TLDS = new HashSet<String>();
        InputStream is = ArchiveUtils.class.getResourceAsStream("tlds-alpha-by-domain.txt");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line; 
            while((line = reader.readLine())!=null) {
                if (line.startsWith("#")) {
                    continue;
                }
                TLDS.add(line.trim().toLowerCase()); 
            }
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE,"TLD list unavailable",e);
        } finally {
            IOUtils.closeQuietly(is); 
        }
    }
    /**
     * Return whether the given string represents a known 
     * top-level-domain (like "com", "org", etc.) per IANA
     * as of 20100419
     * 
     * @param dom candidate string
     * @return boolean true if recognized as TLD
     */
    public static boolean isTld(String dom) {
        return TLDS.contains(dom.toLowerCase());
    }
}

