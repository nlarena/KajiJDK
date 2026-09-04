package javax.sql.rowset.serial;

import java.io.Serializable;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialArray -- una copia en memoria de un ARRAY de SQL.
 *
 * <p>Copia los elementos y el tipo base. Como las otras copias del paquete, el dato sobrevive a la
 * conexion y se puede serializar.
 *
 * <h2>Los elementos se copian recursivamente</h2>
 *
 * <p>Un ARRAY de SQL puede contener BLOB, CLOB, referencias o estructuras, y ninguno de esos
 * sobrevive a la conexion por su cuenta. Por eso el constructor los convierte a su equivalente de
 * este paquete a medida que copia: un {@code Blob} adentro se vuelve un {@link SerialBlob}, y asi.
 * Sin eso, la copia seria un arreglo de punteros muertos.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Los cuatro {@code getResultSet} lanzan {@link SerialException}. Devolver un
 * {@link ResultSet} pide una implementacion de conjunto de resultados desconectado --que es
 * justamente lo que vive en {@code javax.sql.rowset}, un paquete que esta biblioteca no tiene-- y
 * fabricar uno a medias seria peor que decir que no. El JDK tampoco los soporta en esta clase: lanza
 * la misma excepcion.
 */
public class SerialArray implements Array, Serializable, Cloneable {

    private static final long serialVersionUID = -8466174297270688520L;

    /** Los elementos, ya convertidos. */
    private Object[] elements;

    /** El codigo de tipo de {@code java.sql.Types}. */
    private final int baseType;

    /** El nombre del tipo base. */
    private final String baseTypeName;

    /** Si ya se libero con {@link #free}. */
    private boolean freed = false;

    /**
     * Copia el arreglo, traduciendo los tipos definidos por el usuario con ese mapa.
     *
     * @throws SQLException si el arreglo es null o no se puede leer
     */
    public SerialArray(Array array, Map<String, Class<?>> map)
        throws SerialException, SQLException {
        if (array == null) {
            throw new SQLException("Cannot instantiate a SerialArray object with a null Array "
                + "object");
        }
        if (map == null) {
            throw new SQLException("Cannot instantiate a SerialArray object with a null map");
        }
        this.baseType = array.getBaseType();
        this.baseTypeName = array.getBaseTypeName();
        this.elements = copyElements(array);
    }

    /** Idem, sin mapa de tipos. */
    public SerialArray(Array array) throws SerialException, SQLException {
        if (array == null) {
            throw new SQLException("Cannot instantiate a SerialArray object with a null Array "
                + "object");
        }
        this.baseType = array.getBaseType();
        this.baseTypeName = array.getBaseTypeName();
        this.elements = copyElements(array);
    }

    /**
     * Suelta la copia.
     *
     * <p>Ademas libera los elementos que a su vez tengan recursos --los BLOB y CLOB copiados--, que
     * es lo que hace que liberar el arreglo libere de verdad la memoria.
     */
    public void free() throws SQLException {
        if (this.elements != null) {
            int i = 0;
            while (i < this.elements.length) {
                Object e = this.elements[i];
                if (e instanceof java.sql.Blob) {
                    ((java.sql.Blob) e).free();
                } else if (e instanceof java.sql.Clob) {
                    ((java.sql.Clob) e).free();
                }
                i = i + 1;
            }
        }
        this.elements = null;
        this.freed = true;
    }

    /** Los elementos. */
    public Object getArray() throws SerialException {
        check();
        Object[] copy = new Object[this.elements.length];
        System.arraycopy(this.elements, 0, copy, 0, this.elements.length);
        return copy;
    }

    /** Idem; el mapa se ignora, ver {@link SerialRef#getObject(Map)}. */
    public Object getArray(Map<String, Class<?>> map) throws SerialException {
        return getArray();
    }

    /**
     * Una porcion.
     *
     * @param index la primera posicion, empezando en 1
     */
    public Object getArray(long index, int count) throws SerialException {
        check();
        if (index < 1 || index > this.elements.length) {
            throw new SerialException("Invalid index");
        }
        if (count < 0 || (index - 1) + count > this.elements.length) {
            throw new SerialException("Invalid count");
        }
        Object[] copy = new Object[count];
        System.arraycopy(this.elements, (int) (index - 1), copy, 0, count);
        return copy;
    }

    /** Idem; el mapa se ignora. */
    public Object getArray(long index, int count, Map<String, Class<?>> map)
        throws SerialException {
        return getArray(index, count);
    }

    /** El codigo de tipo de {@code java.sql.Types}. */
    public int getBaseType() throws SerialException {
        check();
        return this.baseType;
    }

    /** El nombre del tipo base. */
    public String getBaseTypeName() throws SerialException {
        check();
        return this.baseTypeName;
    }

    /**
     * No hay conjunto de resultados.
     *
     * @throws SerialException siempre; ver la nota de la clase
     */
    public ResultSet getResultSet(long index, int count) throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Idem. */
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Idem. */
    public ResultSet getResultSet() throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Idem. */
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map)
        throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Iguales si coinciden el tipo base y los elementos. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialArray)) {
            return false;
        }
        SerialArray that = (SerialArray) obj;
        if (this.baseType != that.baseType) {
            return false;
        }
        if (!this.baseTypeName.equals(that.baseTypeName)) {
            return false;
        }
        if (this.elements == null || that.elements == null) {
            return this.elements == that.elements;
        }
        if (this.elements.length != that.elements.length) {
            return false;
        }
        int i = 0;
        while (i < this.elements.length) {
            Object a = this.elements[i];
            Object b = that.elements[i];
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        int hash = 31 * this.baseType + this.baseTypeName.hashCode();
        if (this.elements != null) {
            int i = 0;
            while (i < this.elements.length) {
                Object e = this.elements[i];
                hash = hash * 31 + (e == null ? 0 : e.hashCode());
                i = i + 1;
            }
        }
        return hash;
    }

    /** Una copia con su propio arreglo. */
    public Object clone() {
        try {
            SerialArray copy = (SerialArray) super.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** Copia los elementos, convirtiendo los que no sobreviven a la conexion. */
    private static Object[] copyElements(Array array) throws SerialException, SQLException {
        Object raw = array.getArray();
        if (raw == null) {
            return new Object[0];
        }
        int len = java.lang.reflect.Array.getLength(raw);
        Object[] out = new Object[len];
        int i = 0;
        while (i < len) {
            out[i] = copyElement(java.lang.reflect.Array.get(raw, i));
            i = i + 1;
        }
        return out;
    }

    /** Un elemento, convertido si hace falta. Ver la nota de la clase. */
    private static Object copyElement(Object e) throws SerialException, SQLException {
        if (e instanceof java.sql.Blob) {
            return new SerialBlob((java.sql.Blob) e);
        }
        if (e instanceof java.sql.Clob) {
            return new SerialClob((java.sql.Clob) e);
        }
        if (e instanceof java.sql.Ref) {
            return new SerialRef((java.sql.Ref) e);
        }
        return e;
    }

    /** Que no se haya liberado. */
    private void check() throws SerialException {
        if (this.freed || this.elements == null) {
            throw new SerialException("Error: You cannot call a method on a SerialArray instance "
                + "once free() has been called.");
        }
    }
}
