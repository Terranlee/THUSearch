
package st.ata.util;

import java.io.InputStream;
import java.util.Date;


/** Adds mutating operations to {@link AList}. */

public interface MutableAList extends AList {
    public void putObject(String key, Object val);

    public void putInt(String key, int value);

    public void putLong(String key, long value);

    public void putString(String key, String value);

    public void putAList(String key, AList value);

    public void putDate(String key, Date value);

    public void putInputStream(String key, InputStream value);

    public void putIntArray(String key, int[] value);

    public void putLongArray(String key, long[] value);

    public void putStringArray(String key, String[] value);

    public void putAListArray(String key, AList[] value);

    public void putDateArray(String key, Date[] value);

    public void putInputStreamArray(String key, InputStream[] value);
}
