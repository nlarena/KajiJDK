package javax.sql.rowset.serial;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialJavaObject -- a Java object kept in a column.
 *
 * <p>It wraps any object so that it can be put in a column of type {@code JAVA_OBJECT}.
 *
 * <h2>The constructor's validation</h2>
 *
 * <p>It requires the object to be serializable, and it also checks its public <b>fields</b>: if one
 * is not static and its type is not serializable, it is rejected. That second part looks
 * superfluous and is not -- an object marked {@code Serializable} with a field that is not fails
 * only when serializing, which is when the context it came from has already been lost.
 *
 * <p>JDK 25 does neither: it accepts a plain {@code Object}, and a class with public static fields,
 * without complaint.
 *
 * <p>{@link #getFields} returns the fields of the wrapped object. It is a reflection door onto
 * something that arrived from a database, and that is why it is worth looking at twice before using
 * it.
 */
public class SerialJavaObject implements Serializable, Cloneable {

    private static final long serialVersionUID = -1465795139032831023L;

    /** The wrapped object. */
    private final Object obj;

    /**
     * @param obj the object to keep
     * @throws SerialException if it is null, if it is not serializable, or if it has an instance
     *     field that is not
     */
    public SerialJavaObject(Object obj) throws SerialException {
        if (obj == null) {
            throw new SerialException("Cannot serialize a null object");
        }
        if (!(obj instanceof Serializable)) {
            throw new SerialException("Object is not serializable");
        }
        Field[] fields = obj.getClass().getFields();
        int i = 0;
        while (i < fields.length) {
            Field f = fields[i];
            if (!Modifier.isStatic(f.getModifiers())) {
                Class<?> t = f.getType();
                if (!t.isPrimitive() && !Serializable.class.isAssignableFrom(t)) {
                    throw new SerialException("Object is not serializable: field " + f.getName()
                        + " of type " + t.getName());
                }
            }
            i = i + 1;
        }
        this.obj = obj;
    }

    /** The wrapped object. */
    public Object getObject() throws SerialException {
        return this.obj;
    }

    /** The public fields of the wrapped object. See the class note. */
    public Field[] getFields() throws SerialException {
        if (this.obj == null) {
            throw new SerialException("SerialJavaObject does not contain an object");
        }
        return this.obj.getClass().getFields();
    }

    /** Equal if the wrapped objects are. */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SerialJavaObject)) {
            return false;
        }
        SerialJavaObject that = (SerialJavaObject) o;
        return this.obj == null ? that.obj == null : this.obj.equals(that.obj);
    }

    /** Consistent with {@link #equals}. */
    public int hashCode() {
        return 31 + (this.obj == null ? 0 : this.obj.hashCode());
    }

    /**
     * A copy.
     *
     * <p>It shares the wrapped object: copying it would need serializing it and reading it back,
     * which is expensive and can fail. It is what the JDK does.
     */
    public Object clone() {
        try {
            return new SerialJavaObject(this.obj);
        } catch (SerialException e) {
            return null;
        }
    }
}
