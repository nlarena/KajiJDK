package javax.sql.rowset.serial;

import java.sql.SQLException;
import java.sql.SQLInput;
import java.util.Map;

/**
 * KajiLibrary's javax.sql.rowset.serial.SQLInputImpl -- an {@link SQLInput} over an array.
 *
 * <p>What an {@code SQLData} uses to <b>read</b> itself from an in-memory copy: the driver hands it
 * the attributes already read and this class serves them one after another.
 *
 * <h2>A cursor, not access by index</h2>
 *
 * <p>The twenty-six {@code read}s take no position: each one consumes <b>the next</b> attribute and
 * advances. It is what makes a hand-written {@code readSQL} work, and also what makes it fragile:
 * reading the attributes in a different order from the one they were written in gives crossed
 * values, and no {@code ClassCastException} warns about it if the types happen to match.
 *
 * <p>{@link #wasNull} answers about <b>the last read</b>, not the next one. With primitives it is
 * the only way to tell a zero from a null, because {@code readInt} on a null returns 0.
 */
public class SQLInputImpl implements SQLInput {

    /** The attributes, in order. */
    private final Object[] attrib;

    /** The map of user-defined types. */
    private final Map<String, Class<?>> map;

    /** Which one is next. */
    private int idx = 0;

    /** Whether the last read gave null. */
    private boolean lastWasNull = false;

    /**
     * @param attributes the attributes already read, in order
     * @param map the translation of user-defined types
     * @throws SQLException if either of the two is null
     */
    public SQLInputImpl(Object[] attributes, Map<String, Class<?>> map) throws SQLException {
        if (attributes == null || map == null) {
            throw new SQLException("Cannot instantiate a SQLInputImpl object with null parameters");
        }
        this.attrib = attributes;
        this.map = map;
    }

    /**
     * The next attribute, advancing the cursor.
     *
     * @throws SQLException if there are none left
     */
    private Object nextAttribute() throws SQLException {
        if (this.idx >= this.attrib.length) {
            throw new SQLException("SQLInputImpl exception: Invalid read position");
        }
        Object v = this.attrib[this.idx];
        this.idx = this.idx + 1;
        this.lastWasNull = (v == null);
        return v;
    }

    /** Whether the <b>last</b> read gave null. See the class note. */
    public boolean wasNull() throws SQLException {
        return this.lastWasNull;
    }

    /**
     * The next attribute, without casting.
     *
     * <p>If it is a {@code Struct} whose type name is in the map, it is translated: the class that
     * corresponds to it is instantiated and asked to read itself. It is the recursion that allows a
     * structured type to contain another.
     */
    public Object readObject() throws SQLException {
        Object v = nextAttribute();
        if (!(v instanceof java.sql.Struct)) {
            return v;
        }
        java.sql.Struct s = (java.sql.Struct) v;
        Class<?> c = this.map.get(s.getSQLTypeName());
        if (c == null) {
            return v;
        }
        try {
            Object made = c.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            if (!(made instanceof java.sql.SQLData)) {
                return v;
            }
            java.sql.SQLData data = (java.sql.SQLData) made;
            SQLInputImpl inner = new SQLInputImpl(s.getAttributes(this.map), this.map);
            data.readSQL(inner, s.getSQLTypeName());
            return data;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Unable to instantiate " + c.getName() + ": " + e);
        }
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public String readString() throws SQLException {
        Object v = nextAttribute();
        return (String) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public boolean readBoolean() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? false : ((Boolean) v).booleanValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public byte readByte() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0 : ((Byte) v).byteValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public short readShort() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0 : ((Short) v).shortValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public int readInt() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0 : ((Integer) v).intValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public long readLong() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0L : ((Long) v).longValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public float readFloat() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0f : ((Float) v).floatValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public double readDouble() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0d : ((Double) v).doubleValue();
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.math.BigDecimal readBigDecimal() throws SQLException {
        Object v = nextAttribute();
        return (java.math.BigDecimal) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public byte[] readBytes() throws SQLException {
        Object v = nextAttribute();
        return (byte[]) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Date readDate() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Date) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Time readTime() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Time) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Timestamp readTimestamp() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Timestamp) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.io.Reader readCharacterStream() throws SQLException {
        Object v = nextAttribute();
        return (java.io.Reader) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.io.InputStream readAsciiStream() throws SQLException {
        Object v = nextAttribute();
        return (java.io.InputStream) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.io.InputStream readBinaryStream() throws SQLException {
        Object v = nextAttribute();
        return (java.io.InputStream) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Ref readRef() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Ref) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Blob readBlob() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Blob) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Clob readClob() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Clob) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.Array readArray() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Array) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.net.URL readURL() throws SQLException {
        Object v = nextAttribute();
        return (java.net.URL) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.NClob readNClob() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.NClob) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public String readNString() throws SQLException {
        Object v = nextAttribute();
        return (String) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.SQLXML readSQLXML() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.SQLXML) v;
    }

    /** The next attribute. It consumes and advances; see the class note. */
    public java.sql.RowId readRowId() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.RowId) v;
    }
}
