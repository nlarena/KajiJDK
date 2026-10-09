package javax.sql.rowset.serial;

import java.io.Serializable;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialArray -- an in-memory copy of an SQL ARRAY.
 *
 * <p>It copies the elements and the base type. Like the other copies of the package, the datum
 * survives the connection and can be serialized.
 *
 * <h2>The elements are copied recursively</h2>
 *
 * <p>An SQL ARRAY can contain BLOBs, CLOBs, references or structs, and none of those survives the
 * connection by itself. That is why the constructor converts them to their equivalent in this
 * package as it copies: a {@code Blob} inside becomes a {@link SerialBlob}, and so on. Without
 * that, the copy would be an array of dead pointers.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The four {@code getResultSet}s throw {@link SerialException}. Returning a {@link ResultSet}
 * needs an implementation of a disconnected result set, and making half of one would be worse than
 * saying no. The JDK does not support them in this class either: it throws the same exception. (The
 * note said {@code javax.sql.rowset} is a package this library does not have; its interfaces are
 * here, what is missing is an implementation of them.)
 */
public class SerialArray implements Array, Serializable, Cloneable {

    private static final long serialVersionUID = -8466174297270688520L;

    /** The elements, already converted. */
    private Object[] elements;

    /** The type code from {@code java.sql.Types}. */
    private final int baseType;

    /** The name of the base type. */
    private final String baseTypeName;

    /** Whether it was already freed with {@link #free}. */
    private boolean freed = false;

    /**
     * Copies the array, translating the user-defined types with that map.
     *
     * @throws SQLException if the array is null or cannot be read
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

    /** Likewise, without a type map. */
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
     * Lets go of the copy.
     *
     * <p>It also frees the elements that in turn hold resources --the copied BLOBs and CLOBs--,
     * which is what makes freeing the array really free the memory.
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

    /** The elements. */
    public Object getArray() throws SerialException {
        check();
        Object[] copy = new Object[this.elements.length];
        System.arraycopy(this.elements, 0, copy, 0, this.elements.length);
        return copy;
    }

    /** Likewise; the map is ignored, see {@link SerialRef#getObject(Map)}. */
    public Object getArray(Map<String, Class<?>> map) throws SerialException {
        return getArray();
    }

    /**
     * A slice.
     *
     * @param index the first position, starting at 1
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

    /** Likewise; the map is ignored. */
    public Object getArray(long index, int count, Map<String, Class<?>> map)
        throws SerialException {
        return getArray(index, count);
    }

    /** The type code from {@code java.sql.Types}. */
    public int getBaseType() throws SerialException {
        check();
        return this.baseType;
    }

    /** The name of the base type. */
    public String getBaseTypeName() throws SerialException {
        check();
        return this.baseTypeName;
    }

    /**
     * There is no result set.
     *
     * @throws SerialException always; see the class note
     */
    public ResultSet getResultSet(long index, int count) throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Likewise. */
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Likewise. */
    public ResultSet getResultSet() throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Likewise. */
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map)
        throws SerialException {
        throw new SerialException("Unsupported operation");
    }

    /** Equal if the base type and the elements match. */
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

    /** Consistent with {@link #equals}. */
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

    /** A copy with its own array. */
    public Object clone() {
        try {
            SerialArray copy = (SerialArray) super.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** Copies the elements, converting the ones that do not survive the connection. */
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

    /** One element, converted if needed. See the class note. */
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

    /** That it has not been freed. */
    private void check() throws SerialException {
        if (this.freed || this.elements == null) {
            throw new SerialException("Error: You cannot call a method on a SerialArray instance "
                + "once free() has been called.");
        }
    }
}
