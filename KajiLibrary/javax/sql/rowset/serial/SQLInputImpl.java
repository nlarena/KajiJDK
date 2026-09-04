package javax.sql.rowset.serial;

import java.sql.SQLException;
import java.sql.SQLInput;
import java.util.Map;

/**
 * KajiLibrary's javax.sql.rowset.serial.SQLInputImpl -- un {@link SQLInput} sobre un arreglo.
 *
 * <p>Lo que un {@code SQLData} usa para <b>leerse</b> a si mismo desde una copia en memoria: el
 * driver le entrega los atributos ya leidos y esta clase se los va sirviendo en orden.
 *
 * <h2>Un cursor, no un acceso por indice</h2>
 *
 * <p>Los veintiseis {@code read} no reciben posicion: cada uno consume <b>el siguiente</b> atributo y
 * avanza. Es lo que hace que un {@code readSQL} escrito a mano funcione, y tambien lo que lo hace
 * fragil: leer los atributos en distinto orden que el que se escribieron da valores cruzados, y
 * ningun {@code ClassCastException} lo avisa si los tipos coinciden por casualidad.
 *
 * <p>{@link #wasNull} contesta sobre <b>la ultima lectura</b>, no sobre la siguiente. Con los
 * primitivos es la unica forma de distinguir un cero de un null, porque {@code readInt} sobre un null
 * devuelve 0.
 */
public class SQLInputImpl implements SQLInput {

    /** Los atributos, en orden. */
    private final Object[] attrib;

    /** El mapa de tipos definidos por el usuario. */
    private final Map<String, Class<?>> map;

    /** Cual sigue. */
    private int idx = 0;

    /** Si la ultima lectura dio null. */
    private boolean lastWasNull = false;

    /**
     * @param attributes los atributos ya leidos, en orden
     * @param map la traduccion de tipos definidos por el usuario
     * @throws SQLException si alguno de los dos es null
     */
    public SQLInputImpl(Object[] attributes, Map<String, Class<?>> map) throws SQLException {
        if (attributes == null || map == null) {
            throw new SQLException("Cannot instantiate a SQLInputImpl object with null parameters");
        }
        this.attrib = attributes;
        this.map = map;
    }

    /**
     * El siguiente atributo, avanzando el cursor.
     *
     * @throws SQLException si ya no quedan
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

    /** Si la <b>ultima</b> lectura dio null. Ver la nota de la clase. */
    public boolean wasNull() throws SQLException {
        return this.lastWasNull;
    }

    /**
     * El siguiente atributo, sin castear.
     *
     * <p>Si es un {@code Struct} cuyo nombre de tipo esta en el mapa, se lo traduce: se instancia la
     * clase que le corresponde y se le pide que se lea a si misma. Es la recursion que permite que un
     * tipo estructurado contenga otro.
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

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public String readString() throws SQLException {
        Object v = nextAttribute();
        return (String) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public boolean readBoolean() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? false : ((Boolean) v).booleanValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public byte readByte() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0 : ((Byte) v).byteValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public short readShort() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0 : ((Short) v).shortValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public int readInt() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0 : ((Integer) v).intValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public long readLong() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0L : ((Long) v).longValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public float readFloat() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0f : ((Float) v).floatValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public double readDouble() throws SQLException {
        Object v = nextAttribute();
        return (v == null) ? 0d : ((Double) v).doubleValue();
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.math.BigDecimal readBigDecimal() throws SQLException {
        Object v = nextAttribute();
        return (java.math.BigDecimal) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public byte[] readBytes() throws SQLException {
        Object v = nextAttribute();
        return (byte[]) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Date readDate() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Date) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Time readTime() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Time) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Timestamp readTimestamp() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Timestamp) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.io.Reader readCharacterStream() throws SQLException {
        Object v = nextAttribute();
        return (java.io.Reader) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.io.InputStream readAsciiStream() throws SQLException {
        Object v = nextAttribute();
        return (java.io.InputStream) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.io.InputStream readBinaryStream() throws SQLException {
        Object v = nextAttribute();
        return (java.io.InputStream) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Ref readRef() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Ref) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Blob readBlob() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Blob) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Clob readClob() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Clob) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.Array readArray() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.Array) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.net.URL readURL() throws SQLException {
        Object v = nextAttribute();
        return (java.net.URL) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.NClob readNClob() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.NClob) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public String readNString() throws SQLException {
        Object v = nextAttribute();
        return (String) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.SQLXML readSQLXML() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.SQLXML) v;
    }

    /** El siguiente atributo. Consume y avanza; ver la nota de la clase. */
    public java.sql.RowId readRowId() throws SQLException {
        Object v = nextAttribute();
        return (java.sql.RowId) v;
    }
}
