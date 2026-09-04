package javax.sql.rowset.serial;

import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.Map;
import java.util.Vector;

/**
 * KajiLibrary's javax.sql.rowset.serial.SQLOutputImpl -- un {@link SQLOutput} sobre un vector.
 *
 * <p>La contracara de {@link SQLInputImpl}: lo que un {@code SQLData} usa para <b>escribirse</b>.
 * Cada {@code write} agrega un atributo al final del vector, en el orden en que se llamen.
 *
 * <p>Ese orden es el contrato: lo que se escriba aca tiene que leerse en el mismo orden del otro
 * lado. Es la parte del API que no valida nada y que rompe en silencio.
 *
 * <p>Los {@code write} de tipos que no sobreviven a la conexion --BLOB, CLOB, referencias,
 * arreglos, estructuras-- guardan la copia de este paquete y no el objeto del driver. Es lo que hace
 * que el vector resultante se pueda serializar.
 *
 * <p>{@link #writeObject} es el caso recursivo: le pide al objeto que se escriba en un
 * {@code SQLOutputImpl} nuevo y guarda el {@link SerialStruct} que sale.
 */
public class SQLOutputImpl implements SQLOutput {

    /** Adonde se van agregando los atributos. */
    private final Vector<Object> attribs;

    /** El mapa de tipos definidos por el usuario. */
    private final Map<String, ?> map;

    /**
     * @param attributes el vector donde se acumulan; se usa el que se pasa, no una copia
     * @param map la traduccion de tipos definidos por el usuario
     * @throws SQLException si alguno de los dos es null
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
     * Escribe un objeto que sabe mapearse a SQL.
     *
     * <p>Guarda un {@link SerialStruct}; ver la nota de la clase.
     */
    public void writeObject(java.sql.SQLData x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialStruct(x, castMap()));
    }

    /** El mapa con el tipo que pide {@link SerialStruct}. */
    @SuppressWarnings("unchecked")
    private Map<String, Class<?>> castMap() {
        return (Map<String, Class<?>>) this.map;
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeString(String x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeBoolean(boolean x) throws SQLException {
        this.attribs.add(Boolean.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeByte(byte x) throws SQLException {
        this.attribs.add(Byte.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeShort(short x) throws SQLException {
        this.attribs.add(Short.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeInt(int x) throws SQLException {
        this.attribs.add(Integer.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeLong(long x) throws SQLException {
        this.attribs.add(Long.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeFloat(float x) throws SQLException {
        this.attribs.add(Float.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeDouble(double x) throws SQLException {
        this.attribs.add(Double.valueOf(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeBigDecimal(java.math.BigDecimal x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeBytes(byte[] x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeDate(java.sql.Date x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeTime(java.sql.Time x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeTimestamp(java.sql.Timestamp x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeCharacterStream(java.io.Reader x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeAsciiStream(java.io.InputStream x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeBinaryStream(java.io.InputStream x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeRef(java.sql.Ref x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialRef(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeBlob(java.sql.Blob x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialBlob(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeClob(java.sql.Clob x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialClob(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeStruct(java.sql.Struct x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialStruct(x, castMap()));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeArray(java.sql.Array x) throws SQLException {
        if (x == null) {
            this.attribs.add(null);
            return;
        }
        this.attribs.add(new SerialArray(x));
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeURL(java.net.URL x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeNString(String x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeNClob(java.sql.NClob x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeRowId(java.sql.RowId x) throws SQLException {
        this.attribs.add(x);
    }

    /** Agrega el atributo al final. Ver la nota de la clase sobre el orden. */
    public void writeSQLXML(java.sql.SQLXML x) throws SQLException {
        this.attribs.add(x);
    }
}
