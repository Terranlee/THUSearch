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
 * PaddingStringBuffer.java
 * Created on Oct 23, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * StringBuffer-like utility which can add spaces to reach a certain column.  It
 * allows you to append {@link String}, <code>long</code> and <code>int</code>s
 * to the buffer.
 * <p>
 * Note: This class counts from 1, not 0.
 * <p>
 * It uses a StringBuffer behind the scenes.
 * <p>
 * To write a string with multiple lines, it is advisible to use the
 * {@link #newline() newline()} function. Regular appending of strings with
 * newlines (\n) character should be safe though. Right appending of strings
 * with such characters is <i>not</i> safe.
 *
 * @author Gordon Mohr
 */
public final class PaddingStringBuffer {
    // The buffer.
    StringBuffer buffer;
    // Location in current line
    int linePos;

    /** 
     * Create a new PaddingStringBuffer
     *
     */
    public PaddingStringBuffer() {
        buffer = new StringBuffer();
        linePos=0;
    }

    /** append a string directly to the buffer
     * @param string the string to append
     * @return This wrapped buffer w/ the passed string appended.
     */
    public PaddingStringBuffer append(String string) {
        buffer.append(string);
        if ( string.indexOf('\n') == -1 ){
            linePos+=string.length();
        } else {
            while ( string.indexOf('\n') == -1 ){
                string = string.substring(string.indexOf('\n'));
            }
            linePos=string.length();
        }
        return this;
    }

    /**
     * Append a string, right-aligned to the given columm.  If the buffer
     * length is already greater than the column specified, it simply appends
     * the string
     *
     * @param col the column to right-align to
     * @param string the string, must not contain multiple lines.
     * @return This wrapped buffer w/ append string, right-aligned to the
     * given column.
     */
    public PaddingStringBuffer raAppend(int col, String string) {
        padTo(col-string.length());
        append(string);
        return this;
    }

    /** Pad to a given column.  If the buffer size is already greater than the
     * column, nothing is done.
     * @param col
     * @return The buffer padded to <code>i</code>.
     */
    public PaddingStringBuffer padTo(int col) {
        while(linePos<col) {
            buffer.append(" ");
            linePos++;
        }
        return this;
    }

    /** append an <code>int</code> to the buffer.
     * @param i the int to append
     * @return This wrapped buffer with <code>i</code> appended.
     */
    public PaddingStringBuffer append(int i) {
        append(Integer.toString(i));
        return this;
    }


    /**
     * Append an <code>int</code> right-aligned to the given column.  If the
     * buffer length is already greater than the column specified, it simply
     * appends the <code>int</code>.
     *
     * @param col the column to right-align to
     * @param i   the int to append
     * @return This wrapped buffer w/ appended int, right-aligned to the
     *         given column.
     */
    public PaddingStringBuffer raAppend(int col, int i) {
        return raAppend(col,Integer.toString(i));
    }

    /** append a <code>long</code> to the buffer.
     * @param lo the <code>long</code> to append
     * @return This wrapped buffer w/ appended long.
     */
    public PaddingStringBuffer append(long lo) {
        append(Long.toString(lo));
        return this;
    }

    /**Append a <code>long</code>, right-aligned to the given column.  If the
     * buffer length is already greater than the column specified, it simply
     * appends the <code>long</code>.
     * @param col the column to right-align to
     * @param lo the long to append
     * @return This wrapped buffer w/ appended long, right-aligned to the
     * given column.
     */
    public PaddingStringBuffer raAppend(int col, long lo) {
        return raAppend(col,Long.toString(lo));
    }

    /** reset the buffer back to empty */
    public void reset() {
        buffer = new StringBuffer();
        linePos = 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return buffer.toString();
    }

    /**
     * Forces a new line in the buffer.
     */
    public PaddingStringBuffer newline() {
        buffer.append("\n");
        linePos = 0;
        return this;
    }

}
