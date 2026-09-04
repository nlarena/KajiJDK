package javax.sql.rowset.serial;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialJavaObject -- un objeto Java guardado en una columna.
 *
 * <p>Envuelve un objeto cualquiera para poder ponerlo en una columna de tipo {@code JAVA_OBJECT}.
 *
 * <h2>La validacion del constructor</h2>
 *
 * <p>Exige que el objeto sea serializable, y ademas revisa sus <b>campos</b>: si alguno no es
 * estatico y su tipo no es serializable, se rechaza. Esa segunda parte parece de mas y no lo es --
 * un objeto marcado {@code Serializable} con un campo que no lo es falla recien al serializar, que
 * es cuando ya se perdio el contexto de donde vino.
 *
 * <p>{@link #getFields} devuelve los campos del objeto envuelto. Es una puerta de reflexion sobre
 * algo que llego de una base de datos, y por eso conviene mirarla dos veces antes de usarla.
 */
public class SerialJavaObject implements Serializable, Cloneable {

    private static final long serialVersionUID = -1465795139032831023L;

    /** El objeto envuelto. */
    private final Object obj;

    /**
     * @param obj el objeto a guardar
     * @throws SerialException si es null, si no es serializable, o si tiene un campo de instancia
     *     que no lo es
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

    /** El objeto envuelto. */
    public Object getObject() throws SerialException {
        return this.obj;
    }

    /** Los campos publicos del objeto envuelto. Ver la nota de la clase. */
    public Field[] getFields() throws SerialException {
        if (this.obj == null) {
            throw new SerialException("SerialJavaObject does not contain an object");
        }
        return this.obj.getClass().getFields();
    }

    /** Iguales si los objetos envueltos lo son. */
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

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        return 31 + (this.obj == null ? 0 : this.obj.hashCode());
    }

    /**
     * Una copia.
     *
     * <p>Comparte el objeto envuelto: copiarlo pediria serializarlo y volver a leerlo, que es caro y
     * puede fallar. Es lo que hace el JDK.
     */
    public Object clone() {
        try {
            return new SerialJavaObject(this.obj);
        } catch (SerialException e) {
            return null;
        }
    }
}
