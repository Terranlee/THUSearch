
package st.ata.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

// Tested by TestX.java

/** A collection of useful static methods. */
@SuppressWarnings("unchecked")
public final class X {
    public static final int DEBUG = 2;

    /** Throws a runtime exception with message <code>m</code>. */
    public static void fail(String m) {
        RuntimeException e = new RuntimeException(m);
        popTrace(e, 1);
        throw e;
    }

    /**
     * Throws a runtime exception with message <code>systemProperty</code>.
     * @param systemProperty a <code>String</code> value which specifies
     *        a boolean system property, which if true will cause
     *        an exception to be thrown.
     */
    public static void testFailure(String systemProperty) {
        if (!Boolean.getBoolean(systemProperty)) {
            return;
        }

        RuntimeException e = new RuntimeException(systemProperty);
        popTrace(e, 1);
        throw e;
    }

    /** Throws a runtime exception if <code>b</code> is not true. */
    public static void check(boolean b) {
        if (b) return;
        RuntimeException e = new RuntimeException("assertion failure");
        popTrace(e, 1);
         throw e;
    }

    /** Throws a runtime exception if <code>b</code> is not true. */
    public static void check(boolean b, String m) {
        if (b) return;
        RuntimeException e = new RuntimeException(m);
        popTrace(e, 1);
        throw e;
    }

    /** Throws an illegal argument exception if <code>b</code> is not true. */
    public static void checkargs(boolean b) {
        if (b) return;
        RuntimeException e = new IllegalArgumentException();
        popTrace(e, 1);
        throw e;
    }

    /** Throws an illegal state exception if <code>b</code> is not true. */
    public static void checkstate(boolean b) {
        if (b) return;
        RuntimeException e = new IllegalStateException();
        popTrace(e, 1);
        throw e;
    }

    /**
     *  Returns an {@link UndeclaredThrowableException}
     *  wrapping <code>e</code>.
     */

    public static RuntimeException toRTE(Exception e) {
        RuntimeException rte = new UndeclaredThrowableException(e);
        popTrace(rte, 1);
        return rte;
    }

    /** Same as <c>ut(b,&nbsp;"")</c>. */
    public static void ut(boolean b) { ut(b, ""); }

    /** Test condition during unit testing.  If <c>b</c> is true, does
    nothing.  If <c>b</c> is not true, prints (to
    <c>System.out</c>)
    <c>"Unit&nbsp;test&nbsp;failure:&nbsp;"&nbsp;+&nbsp;m</c> and
    a stack trace then returns. */
    public static void ut(boolean b, String m) {
    if (! b) {
        try {
                if (b) return;
                RuntimeException e
                    = new RuntimeException("Unit test failure: " + m);
                popTrace(e, 1);
                throw e;
        } catch (RuntimeException e) {
        System.out.println("");
        e.printStackTrace(System.out);
        }
    }
    }
    /**
     * print out the programName and arguments used
     */
    public static void printArgs(String programName, String[] args) {
        System.out.print(programName);
        for (int i=0; i<args.length; i++){
            System.out.print(" ");
            System.out.print(args[i]);
        }
    }
    public static void noimpl() {
        RuntimeException e = new RuntimeException("Not implemented yet.");
        popTrace(e, 1);
        throw e;
    }

    /** Returns a full description of a {@link Throwable}.  This full
     *  description includes a stack trace.  This method will never
     *  throw an error or exception: if something bad happens, it
     *  simply returns null. */
    public static String getFullDescription(Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter o = new PrintWriter(sw);
            t.printStackTrace(o);
            o.flush();
            return sw.toString();
        } catch (Throwable ignore) { return null; }
    }

    /** Removes the top <code>n</code> stack-trace elements from
     *  <code>t</code>.  This is useful inside methods like {@link
     *  #fail} to help debuggers more quickly identify the location of
     *  a failure. */
    public static void popTrace(Throwable t, int n) {
        /*StackTraceElement[] ot = t.getStackTrace();
        int len = ot.length - n;
        StackTraceElement[] nt = new StackTraceElement[len];
        System.arraycopy(ot, n, nt, 0, len);
        t.setStackTrace(nt);*/
    }

    public static int decodeInt(byte[] buf, int offset) {
        return ((buf[offset+3]&0xff)<<24
                | (buf[offset+2]&0xff)<<16
                | (buf[offset+1]&0xff)<<8
                | (buf[offset]&0xff));
    }

    public static int decodeShort(byte[] buf, int offset) {
        return ((buf[offset+1]&0xff)<<8
                | (buf[offset]&0xff));
    }

    public static void encodeShort(byte[] buf, int offset, int val) {
        X.check(val<=Short.MAX_VALUE && val>=Short.MIN_VALUE);
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset] = (byte)val;
    }

    public static void encodeInt(byte[] buf, int offset, int val) {
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset] = (byte)val;
    }

    public static long decodeLong(byte[] buf, int offset) {
        long lo = decodeInt(buf, offset) & 0xffffffffL;
        long hi = decodeInt(buf, offset+4);
        return (hi<<32) | lo;
    }

    public static void encodeLong(byte[] buf, int offset, long val) {
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset++] = (byte)val; val >>= 8;
        buf[offset] = (byte)val;
    }


    /**
     *  returns the printable representation of <code>data</code>
     *  after escaping non-printable characters in C style.
     */
    public static String printable(byte[] data) {
        return printable(data, 0, data.length);
    }

    /**
     *  returns the printable representation of <code>data</code>
     *  from <code>start</code> (inclusive) to <code>end</code> (exclusive).
     *  after escaping non-printable characters in C style.
     *  <code>data</code> may not be <code>null</code> and
     *  <code>start</code> must be smaller or equal to <code>end</code>
     *  Both <code>start</code> and <code>end</code> are bounded by
     *  <code>0</code> and <code>data.length</code> bot inclusive.
     */

    public static String printable(byte[] data, int start, int end) {
        checkargs(data != null);
        checkargs(start <= end);
        checkargs(start >= 0);
        checkargs(end <= data.length);
        StringBuffer sb = new StringBuffer();

        for (int i = start; i < end; i++) {
            final byte b = data[i];
            if (b < 0x20 || b > 0x7e) {
                switch (b) {
                case '\r':
                    sb.append("\\r");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append('\\');
                    sb.append(b >>> 6);
                    sb.append((b >>> 3) & 0x07);
                    sb.append(b & 0x07);
                    break;
                }
            } else {
                sb.append((char)b);
            }
        }
        return sb.toString();
    }

    public static void log(String ctxt, Level level, String msg, Throwable t) {
        Logger.getLogger("st."+ctxt).log(level,  ctxt+": "+msg, t);
    }
    public static void log(String ctxt, Level level, String msg) {
        Logger.getLogger("st."+ctxt).log(level, ctxt+": "+msg);
    }
    public static ArrayList dupElim(ArrayList al, Comparator cm) {
        if (al.size()<2)
            return al;
        Collections.sort(al, cm);
        Object prev =  al.get(0);
        ArrayList n = new ArrayList();
        n.add(prev);
        for (int i = 1; i < al.size(); i++) {
            if (!prev.equals(al.get(i)))
                n.add(al.get(i));
            prev =  al.get(i);
        }
        return n;
    }

}
