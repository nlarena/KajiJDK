package javax.sql.rowset.serial;

import java.io.Serializable;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Map;
import java.util.Vector;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialStruct -- una copia de un tipo estructurado de SQL.
 *
 * <p>Un {@link Struct} es una instancia de un tipo definido por el usuario: un nombre de tipo y una
 * lista de atributos. Esta clase copia los dos.
 *
 * <h2>Los dos constructores son dos caminos distintos</h2>
 *
 * <p>El que recibe un {@link Struct} copia lo que ya viene del driver. El que recibe un
 * {@link SQLData} hace algo mas interesante: le pide al objeto que <b>se escriba</b> en un
 * {@link SQLOutputImpl}, y se queda con lo que salga. Es la via para guardar en una columna un
 * objeto Java que sabe mapearse a SQL.
 *
 * <p>Como en {@link SerialArray}, los atributos que no sobreviven a la conexion se convierten a su
 * equivalente de este paquete a medida que se copian.
 */
public class SerialStruct implements Struct, Serializable, Cloneable {

    private static final long serialVersionUID = -8322445504027483372L;

    /** El nombre del tipo. */
    private final String sqlTypeName;

    /** Los atributos, ya convertidos. */
    private Object[] attribs;

    /**
     * Copia un {@link Struct} del driver.
     *
     * @throws SerialException si es null o no se puede leer
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
     * Le pide al objeto que se escriba.
     *
     * <p>Ver la nota de la clase: este es el camino para guardar un objeto Java propio.
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

    /** El nombre del tipo. */
    public String getSQLTypeName() throws SerialException {
        return this.sqlTypeName;
    }

    /** Los atributos. Copia. */
    public Object[] getAttributes() throws SerialException {
        Object[] copy = new Object[this.attribs.length];
        System.arraycopy(this.attribs, 0, copy, 0, this.attribs.length);
        return copy;
    }

    /** Idem; el mapa se ignora, ver {@link SerialRef#getObject(Map)}. */
    public Object[] getAttributes(Map<String, Class<?>> map) throws SerialException {
        return getAttributes();
    }

    /** Iguales si coinciden el tipo y los atributos. */
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

    /** Coherente con {@link #equals}. */
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

    /** Una copia con su propio arreglo de atributos. */
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

    /** Convierte los atributos que no sobreviven a la conexion. */
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
