package javax.sql.rowset.serial;

import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.Map;
import java.util.Vector;

/**
 * KajiLibrary's javax.sql.rowset.serial.SQLOutputImpl -- an {@link SQLOutput} over a vector.
 *
 * <p>The counterpart of {@link SQLInputImpl}: what an {@code SQLData} uses to <b>write</b> itself.
 * Each {@code write} appends an attribute at the end of the vector, in the order they are called.
 *
 * <p>That order is the contract: whatever is written here has to be read in the same order on the
 * other side. It is the part of the API that validates nothing and breaks silently.
 *
 * <p>The {@code write}s of types that do not survive the connection --BLOB, CLOB, references,
 * arrays, structs-- keep this package's copy and not the driver's object. It is what makes the
 * resulting vector serializable.
 *
 * <p>{@link #writeObject} is the recursive case: it asks the object to write itself into a new
 * {@code SQLOutputImpl} and keeps the {@link SerialStruct} that comes out.
 */
public class SQLOutputImpl implements SQLOutput {

    /** Where the attributes are appended. */
    private final Vector<Object> attribs;

    /** The map of user-defined types. */
    private final Map<String, ?> map;

    /**
     * @param attributes the vector where they accumulate; the one passed is used, not a copy
     * @param map the translation of user-defined types
     * @throws SQLException if either of the two is null
     */
    @SuppressWarnings("unchecked")
    public SQLOutputImpl(Vector<?> attributes, Map<String, ?> map) throws SQLException {
        if (attributes == null || map == null) {
            throw new SQLException("Cannot instantiate a SQLOutputImpl object with null parameters");
        }
        this.attribs = (Vector<Object>) attributes;
        this.map = map;
    }

    /**
     * Writes an object that knows how to map itself to SQL.
     *
     * <p>It keeps a {@link SerialStruct}; see the class note.
     */
    public void writeObject(java.sql.SQLData x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialStruct(x, castMap()));
    }

    /** The map with the type {@link SerialStruct} asks for. */
    @SuppressWarnings("unchecked")
    private Map<String, Class<?>> castMap() {
        return (Map<String, Class<?>>) this.map;
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeString(String x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeBoolean(boolean x) throws SQLException {
        this.attribs.add(Boolean.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeByte(byte x) throws SQLException {
        this.attribs.add(Byte.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeShort(short x) throws SQLException {
        this.attribs.add(Short.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeInt(int x) throws SQLException {
        this.attribs.add(Integer.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeLong(long x) throws SQLException {
        this.attribs.add(Long.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeFloat(float x) throws SQLException {
        this.attribs.add(Float.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeDouble(double x) throws SQLException {
        this.attribs.add(Double.valueOf(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeBigDecimal(java.math.BigDecimal x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeBytes(byte[] x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeDate(java.sql.Date x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeTime(java.sql.Time x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeTimestamp(java.sql.Timestamp x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeCharacterStream(java.io.Reader x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeAsciiStream(java.io.InputStream x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeBinaryStream(java.io.InputStream x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeRef(java.sql.Ref x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialRef(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeBlob(java.sql.Blob x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialBlob(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeClob(java.sql.Clob x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialClob(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeStruct(java.sql.Struct x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialStruct(x, castMap()));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeArray(java.sql.Array x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialArray(x));
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeURL(java.net.URL x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeNString(String x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeNClob(java.sql.NClob x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeRowId(java.sql.RowId x) throws SQLException {
        this.attribs.add(x);
    }

    /** Appends the attribute at the end. See the class note on the order. */
    public void writeSQLXML(java.sql.SQLXML x) throws SQLException {
        this.attribs.add(x);
    }
}
