package javax.sql.rowset.serial;

import java.io.Serializable;
import java.sql.Ref;
import java.sql.SQLException;
import java.util.Map;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialRef -- a copy of an SQL reference.
 *
 * <p>A {@link Ref} points to an instance of a structured type that lives on the server. This class
 * copies the name of the type and the object it points to, so that the reference survives the
 * connection.
 *
 * <p>It is the weakest of the copies of this package and it has to be said: a reference <b>only
 * makes sense within its database</b>. What is copied is the value it pointed to at that moment,
 * not the ability to resolve it again. {@link #setObject} changes the copy, not the server.
 */
public class SerialRef implements Ref, Serializable, Cloneable {

    private static final long serialVersionUID = -4727123500609662274L;

    /** The name of the structured type. */
    private final String baseTypeName;

    /** The object it pointed to. */
    private Object object;

    /**
     * @param ref the reference to copy
     * @throws SQLException if it is null or has no type name
     */
    public SerialRef(Ref ref) throws SerialException, SQLException {
        if (ref == null) {
            throw new SQLException("Cannot instantiate a SerialRef object with a null Ref object");
        }
        if (ref.getBaseTypeName() == null) {
            throw new SQLException("Cannot instantiate a SerialRef object that returns a null base "
                + "type name");
        }
        this.baseTypeName = ref.getBaseTypeName();
        this.object = ref;
    }

    /** The name of the structured type. */
    public String getBaseTypeName() throws SerialException {
        return this.baseTypeName;
    }

    /**
     * The object, translating the type with that map.
     *
     * <p>The map is ignored in this copy: the driver does the translation when reading, and here it
     * has already been read. The note said that is what the JDK does; JDK 25 returns {@code null}
     * when given a map, even one keyed by the object.
     */
    public Object getObject(Map<String, Class<?>> map) throws SerialException {
        if (map == null) {
            throw new SerialException("Invalid Map object: no mapping between SQL type and Java "
                + "class");
        }
        return this.object;
    }

    /** The object it pointed to. */
    public Object getObject() throws SerialException {
        return this.object;
    }

    /** Changes the object of <b>this copy</b>. See the class note. */
    public void setObject(Object obj) throws SerialException {
        this.object = obj;
    }

    /** Equal if the type and the object match. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialRef)) {
            return false;
        }
        SerialRef that = (SerialRef) obj;
        if (!this.baseTypeName.equals(that.baseTypeName)) {
            return false;
        }
        return this.object == null ? that.object == null : this.object.equals(that.object);
    }

    /** Consistent with {@link #equals}. */
    public int hashCode() {
        return 31 * this.baseTypeName.hashCode()
            + (this.object == null ? 0 : this.object.hashCode());
    }

    /** A copy; it shares the object. */
    public Object clone() {
        try {
            SerialRef copy = new SerialRef((Ref) this.object);
            copy.object = this.object;
            return copy;
        } catch (SQLException e) {
            return null;
        }
    }
}
