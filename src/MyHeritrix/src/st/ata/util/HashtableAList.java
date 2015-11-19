
package st.ata.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;


// Tested by TestHashtableAList

/** Implementation of {@link AList} using simple hashtable. */
@SuppressWarnings({"unchecked"})
public class HashtableAList implements MutableAList, Serializable {
    private static final long serialVersionUID = 3670660167336648644L;
    
    private final Hashtable mTable = new Hashtable();

    private static class DateArray {
        public Date[] values;
        public DateArray(Date[] v) { values = v; }
        public boolean equals(Object obj) {
            if (! (obj instanceof DateArray)) return false;
            return Arrays.equals(values, ((DateArray)obj).values);
        }
    }


    /** Remove all key-value mappings. */
    public void clear() {
        close();
        mTable.clear();
    }

    public boolean containsKey(String key) {
        return mTable.containsKey(key);
    }

    /**
     * Deep Clone.
     *
     * Limited implementation
     * @return The cloned object.
     */
    public Object clone() {
        HashtableAList copy = new HashtableAList();
        String[] keys = getKeyArray();
        for (int i=0; i<keys.length; i++) {
            Object me=getObject(keys[i]);
            if (me instanceof AList)
                copy.putObject(keys[i], ((AList)me).clone());
            else  if (me instanceof AList[]) {
                AList[] from = (AList[])me;
                int count=from.length;
                for (int j=0; j<from.length; j++) {
                    if (from[j]==null) {
                        count--;
                    }
                }

                AList[] copyAList = new AList[count];
                for (int j=0; j<count; j++) {
                    if (from[j]==null) continue;
                    copyAList[j]=(AList)from[j].clone();
                }
                copy.putObject(keys[i], copyAList);
            } else  if (me instanceof String[]) {
                String[] from = (String[])me;
                String[] copyA = new String[from.length];
                for (int j=0; j<from.length; j++)
                    copyA[j]=from[j];
                copy.putObject(keys[i], copyA);
            }
            else if (me instanceof Long) {
                copy.putObject(keys[i], new Long(((Long)me).longValue()));
            } else if (me instanceof String) {
                copy.putObject(keys[i], me);
            } else
                X.noimpl();
        }
        return copy;
    }

    /** 
     * Shallow copy of fields of <code>other</code> into <code>this</code>.
     * @param other AList to copy from.
     */
    public void copyFrom(AList other) {
        Iterator keys = other.getKeys();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            switch (other.getType(key)) {
            case T_ALIST:
                putAList(key, other.getAList(key));
                break;
            case T_DATE:
                putDate(key, other.getDate(key));
                break;
            case T_INT:
                putInt(key, other.getInt(key));
                break;
            case T_LONG:
                putLong(key, other.getLong(key));
                break;
            case T_STRING:
                putString(key, other.getString(key));
                break;
            case T_INPUTSTREAM:
                putInputStream(key, other.getInputStream(key));
                break;
            case F_ARRAY | T_ALIST:
                putAListArray(key, other.getAListArray(key));
                break;
            case F_ARRAY | T_DATE:
                putDateArray(key, other.getDateArray(key));
                break;
            case F_ARRAY | T_INT:
                putIntArray(key, other.getIntArray(key));
                break;
            case F_ARRAY | T_LONG:
                putLongArray(key, other.getLongArray(key));
                break;
            case F_ARRAY | T_STRING:
                putStringArray(key, other.getStringArray(key));
                break;
            case F_ARRAY_ARRAY | T_STRING:
                putStringArrayArray(key, other.getStringArrayArray(key));
                break;
            case F_ARRAY | T_INPUTSTREAM:
                putInputStreamArray(key, other.getInputStreamArray(key));
                break;
            default:
                X.fail("Unexpected case");
            }
        }
    }
    
    public void copyKeysFrom(Iterator keys, AList other) {
        copyKeysFrom(keys, other, true);  // clobber
    }
    
    public void copyKeysFrom(Iterator keys, AList other, boolean clobber) {
        for (; keys.hasNext();) {
            String key = (String)keys.next();
            if (!clobber && containsKey(key))
                continue;
            Object value = other.getObject(key);
            // TODO: consider shallow or deep copy in some cases?
            // perhaps controlled by an additional parameter?
            if(value!=null) {
                putObject(key,value);
            }
        }
    }
    
    public Object getObject(String key) {
        return mTable.get(key);
    }

    public void putObject(String key, Object val) {
        mTable.put(key, val);
    }
    public void remove(String key) {
        mTable.remove(key);
    }
    public Iterator getKeys() {
        return mTable.keySet().iterator();
    }

    public String[] getKeyArray() {
        int i = 0;
        String keys[] = new String[mTable.size()];
        for(Iterator it = getKeys(); it.hasNext(); ++i)
            keys[i] = (String)it.next();
        return keys;
    }

    public int getInt(String key) {
        Integer v = (Integer)mTable.get(key);
        if (v == null) throw new NoSuchElementException(key);
        return v.intValue();
    }

    public long getLong(String key) {
        Long v = (Long)mTable.get(key);
        if (v == null) throw new NoSuchElementException(key);
        return v.longValue();
    }

    public String getString(String key) {
        String v = (String)mTable.get(key);
        if (v == null) throw new NoSuchElementException(key);
        return v;
    }

    public AList getAList(String key) {
        AList a = (AList) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a;
    }

    public Date getDate(String key) {
        Date v = (Date)mTable.get(key);
        if (v == null) throw new NoSuchElementException(key);
        return v;
    }

    public InputStream getInputStream(String key) {
        InputStream v = (InputStream)mTable.get(key);
        if (v == null) throw new NoSuchElementException(key);
        return v;
    }

    public int[] getIntArray(String key) {
        int[] a = (int[]) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a;
    }

    public long[] getLongArray(String key) {
        long[] a = (long[]) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a;
    }

    public String[] getStringArray(String key) {
        String[] a = (String[]) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a;
    }

    public AList[] getAListArray(String key) {
        AList[] a = (AList[]) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a;
    }

    public Date[] getDateArray(String key) {
        DateArray a = (DateArray) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a.values;
    }

    public InputStream[] getInputStreamArray(String key) {
        InputStream v[] = (InputStream [])mTable.get(key);
        if (v == null) throw new NoSuchElementException(key);
        return v;
    }

    public String[][] getStringArrayArray(String key) {
        String[][] a = (String[][]) mTable.get(key);
        if (a == null) throw new NoSuchElementException(key);
        return a;
    }


    public void putInt(String key, int value) {
        mTable.put(key, new Integer(value));
    }

    public void putLong(String key, long value) {
        mTable.put(key, new Long(value));
    }

    public void putString(String key, String value) {
        mTable.put(key, value);
    }

    public void putAList(String key, AList value) {
        mTable.put(key, value);
    }

    public void putDate(String key, Date value) {
        mTable.put(key, value);
    }

    public void putInputStream(String key, InputStream value) {
        mTable.put(key, value);
    }

    public void putIntArray(String key, int[] value) {
        mTable.put(key, value);
    }

    public void putLongArray(String key, long[] value) {
        mTable.put(key, value);
    }

    public void putStringArray(String key, String[] value) {
        mTable.put(key, value);
    }

    public void putAListArray(String key, AList[] value) {
        mTable.put(key, value);
    }

    public void putDateArray(String key, Date[] value) {
        mTable.put(key, new DateArray(value));
    }

    public void putInputStreamArray(String key, InputStream[] value) {
        mTable.put(key, value);
    }

    public void putStringArrayArray(String key, String[][] value) {
        mTable.put(key, value);
    }


    /** Deep equals.  Arrays need to have same values in same order to
     *  be considered equal.
     * @param obj
     * @return True if equals.
     */
    public boolean equals(Object obj) {
        if (! (obj instanceof HashtableAList)) return false;
        HashtableAList o = (HashtableAList)obj;
        for (Iterator i = o.getKeys(); i.hasNext(); ) {
            if (mTable.get(i.next()) == null) return false;
        }
        for (Iterator i = getKeys(); i.hasNext(); ) {
            Object k = i.next();
            Object v1 = mTable.get(k);
            Object v2 = o.mTable.get(k);
            if (! v1.equals(v2)) {
                if (v1 instanceof AList[]) {
                    if (! (v2 instanceof AList[])) return false;
                    if (! Arrays.equals((Object[])v1, (Object[])v2))
                        return false;
                } else if (v1 instanceof int[]) {
                    if (! (v2 instanceof int[])) return false;
                    if (! Arrays.equals((int[])v1, (int[])v2)) return false;
                } else if (v1 instanceof long[]) {
                    if (! (v2 instanceof long[])) return false;
                    if (! Arrays.equals((long[])v1, (long[])v2)) return false;
                } else if (v1 instanceof String[]) {
                    if (! (v2 instanceof String[])) return false;
                    if (! Arrays.equals((String[])v1, (String[])v2))
                        return false;
                } else return false;
            }
        }
        return true;
    }

    public int getType(String key) {
        Object o = mTable.get(key);
        if (o == null) return T_UNDEFINED;
        else if (o instanceof AList) return T_ALIST;
        else if (o instanceof Date) return T_DATE;
        else if (o instanceof Integer) return T_INT;
        else if (o instanceof Long) return T_LONG;
        else if (o instanceof String) return T_STRING;
        else if (o instanceof InputStream) return T_INPUTSTREAM;
        else if (o instanceof AList[]) return T_ALIST | F_ARRAY;
        else if (o instanceof DateArray) return T_DATE | F_ARRAY;
        else if (o instanceof int[]) return T_INT | F_ARRAY;
        else if (o instanceof long[]) return T_LONG | F_ARRAY;
        else if (o instanceof String[]) return T_STRING | F_ARRAY;
        else if (o instanceof InputStream[]) return T_INPUTSTREAM | F_ARRAY;
        else if (o instanceof String[][]) return T_STRING | F_ARRAY_ARRAY;
        else if (o instanceof Object[]) return T_OBJECT | F_ARRAY;
        else if (o instanceof Object) return T_OBJECT;
        else X.fail("Should not get here " + o);
        return -1;
    }

    /** Useful for creating test-tables for debugging.  The object
        should be one of an {@link AList}, {@link Date}, {@link
        Integer}, {@link Long}, {@link String}, {@link AList}[],
        {@link Date}[], <code>int[]</code>, <code>long[]</code>,
        <code>{@link String}[]</code>, <code>ZE[]</code>
        or <code>ZE[][]</code>.  In the case of <code>ZE[]</code>,
        the entry is treated as an {@link AList}. Similaryly
        if the entry is <code>ZE[][]</code> it is treated as
        {@link AList}[]. */
    public static class ZE {
        public final String key;
        public final Object val;
        public ZE(String k, Object v) { key = k; val = v; }
    }

    public void zInsert(ZE[] entries) {
        for (int i = 0; i < entries.length; i++) {
            zInsert(entries[i]);
        }
    }

    public void zInsert(ZE entry) {
        if (entry.val instanceof Date[]) {
            mTable.put(entry.key, new DateArray((Date[])entry.val));
        } else if (entry.val instanceof ZE[]) {
            HashtableAList v = new HashtableAList();
            v.zInsert((ZE[])entry.val);
            mTable.put(entry.key, v);
        } else if (entry.val instanceof ZE[][]) {
            AList v[] = new AList[((ZE[][])entry.val).length];
            for(int j = 0; j < v.length; ++j) {
                HashtableAList h = new HashtableAList();
                h.zInsert(((ZE[][])entry.val)[j]);
                v[j] = h;
            }
            mTable.put(entry.key, v);
        } else {
            mTable.put(entry.key, entry.val);
        }
    }

    public void close() {
        String[] keys = getKeyArray();
        try {
            for (int i = 0; i < keys.length; i++) {
                if (getType(keys[i]) == T_INPUTSTREAM) {
                    getInputStream(keys[i]).close();
                } else if (getType(keys[i]) == (T_INPUTSTREAM | F_ARRAY)) {
                    InputStream[] ins = getInputStreamArray(keys[i]);
                    for (int j = 0; j < ins.length; j++) {
                        ins[j].close();
                    }
                } else if (getType(keys[i]) == T_ALIST) {
                    getAList(keys[i]).close();
                } else if (getType(keys[i]) == (T_ALIST | F_ARRAY)) {
                    AList[] als = getAListArray(keys[i]);
                    for (int j = 0; j < als.length; j++) {
                        als[j].close();
                    }
                }
            }
        } catch (IOException e) {
            throw X.toRTE(e);
        }
    }

    public AList newAList() {
        return new HashtableAList();
    }

    public String toString() {
        return mTable.toString();
    }

    /**
     * Enhance given object's default String display for appearing
     * nested in a pretty AList String.
     * 
     * @param obj Object to prettify
     * @return prettified String
     */
    protected String prettyString(Object obj) {
        if (obj instanceof AList) return ((AList)obj).toPrettyString();
        else if (obj instanceof AList[]) return prettyString((AList[])obj);
        else return "<"+obj+">"; 
    }
    
    /* (non-Javadoc)
     * @see st.ata.util.AList#toPrettyString()
     */
    public String toPrettyString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        boolean needsComma = false; 
        for( String key : getKeyArray()) {
            if(needsComma) {
                builder.append(", ");
            }
            builder.append(key);
            builder.append(": ");
            builder.append(prettyString(mTable.get(key)));
            needsComma = true; 
        }
        builder.append(" }");
        return builder.toString();
    }
    
    /**
     * Provide a slightly-improved String of AList[]
     * 
     * @param alists
     * @return prettified (in square brackets) of AList[]
     */
    protected String prettyString(AList[] alists) {
        StringBuilder builder = new StringBuilder();
        builder.append("[ ");
        boolean needsComma = false; 
        for( AList alist : alists) {
            if(alist==null) continue;
            if(needsComma) {
                builder.append(", ");
            }
            builder.append(alist.toPrettyString());
            needsComma = true; 
        }
        builder.append(" ]");
        return builder.toString();
    }
}
