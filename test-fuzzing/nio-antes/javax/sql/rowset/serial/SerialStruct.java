package javax.sql.rowset.serial;

import java.io.Serializable;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Map;
import java.util.Vector;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialStruct -- a copy of an SQL structured type.
 *
 * <p>A {@link Struct} is an instance of a user-defined type: a type name and a list of attributes.
 * This class copies both.
 *
 * <h2>The two constructors are two different routes</h2>
 *
 * <p>The one that receives a {@link Struct} copies what already comes from the driver. The one that
 * receives an {@link SQLData} does something more interesting: it asks the object to <b>write
 * itself</b> into an {@link SQLOutputImpl}, and keeps what comes out. It is the route for keeping
 * in a column a Java object that knows how to map itself to SQL.
 *
 * <p>As in {@link SerialArray}, the attributes that do not survive the connection are converted to
 * their equivalent in this package as they are copied.
 */
public class SerialStruct implements Struct, Serializable, Cloneable {

    private static final long serialVersionUID = -8322445504027483372L;

    /** The name of the type. */
    private final String sqlTypeName;

    /** The attributes, already converted. */
    private Object[] attribs;

    /**
     * Copies a {@link Struct} from the driver.
     *
     * @throws SerialException if it is null or cannot be read
     */
    public SerialStruct(Struct in, Map<String, Class<?>> map) throws SerialException {
        if (in == null) {
            throw new SerialException("Cannot instantiate a SerialStruct object with a null Struct "
                + "object");
        }
        try {
            this.sqlTypeName = in.getSQLTypeName();
            this.attribs = convert(in.getAttributes(map == null ? null : map));
        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
    }

    /**
     * Asks the object to write itself.
     *
     * <p>See the class note: this is the route for keeping a Java object of one's own.
     */
    public SerialStruct(SQLData in, Map<String, Class<?>> map) throws SerialException {
        if (in == null) {
            throw new SerialException("Cannot instantiate a SerialStruct object with a null SQLData "
                + "object");
        }
        try {
            this.sqlTypeName = in.getSQLTypeName();
            Vector<Object> written = new Vector<Object>();
            in.writeSQL(new SQLOutputImpl(written, map));
            Object[] raw = new Object[written.size()];
            int i = 0;
            while (i < written.size()) {
                raw[i] = written.elementAt(i);
                i = i + 1;
            }
            this.attribs = convert(raw);
        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
    }

    /** The name of the type. */
    public String getSQLTypeName() throws SerialException {
        return this.sqlTypeName;
    }

    /** The attributes. A copy. */
    public Object[] getAttributes() throws SerialException {
        Object[] copy = new Object[this.attribs.length];
        System.arraycopy(this.attribs, 0, copy, 0, this.attribs.length);
        return copy;
    }

    /** Likewise; the map is ignored, see {@link SerialRef#getObject(Map)}. */
    public Object[] getAttributes(Map<String, Class<?>> map) throws SerialException {
        return getAttributes();
    }

    /** Equal if the type and the attributes match. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialStruct)) {
            return false;
        }
        SerialStruct that = (SerialStruct) obj;
        if (!this.sqlTypeName.equals(that.sqlTypeName)) {
            return false;
        }
        if (this.attribs.length != that.attribs.length) {
            return false;
        }
        int i = 0;
        while (i < this.attribs.length) {
            Object a = this.attribs[i];
            Object b = that.attribs[i];
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Consistent with {@link #equals}. */
    public int hashCode() {
        int hash = this.sqlTypeName.hashCode();
        int i = 0;
        while (i < this.attribs.length) {
            Object a = this.attribs[i];
            hash = hash * 31 + (a == null ? 0 : a.hashCode());
            i = i + 1;
        }
        return hash;
    }

    /** A copy with its own array of attributes. */
    public Object clone() {
        try {
            SerialStruct copy = (SerialStruct) super.clone();
            copy.attribs = new Object[this.attribs.length];
            System.arraycopy(this.attribs, 0, copy.attribs, 0, this.attribs.length);
            return copy;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** Converts the attributes that do not survive the connection. */
    private static Object[] convert(Object[] raw) throws SerialException, SQLException {
        if (raw == null) {
            return new Object[0];
        }
        Object[] out = new Object[raw.length];
        int i = 0;
        while (i < raw.length) {
            Object a = raw[i];
            if (a instanceof java.sql.Blob) {
                out[i] = new SerialBlob((java.sql.Blob) a);
            } else if (a instanceof java.sql.Clob) {
                out[i] = new SerialClob((java.sql.Clob) a);
            } else if (a instanceof java.sql.Ref) {
                out[i] = new SerialRef((java.sql.Ref) a);
            } else if (a instanceof java.sql.Array) {
                out[i] = new SerialArray((java.sql.Array) a);
            } else {
                out[i] = a;
            }
            i = i + 1;
        }
        return out;
    }
}
