package javax.sql.rowset.serial;

import java.io.Serializable;
import java.sql.Ref;
import java.sql.SQLException;
import java.util.Map;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialRef -- una copia de una referencia SQL.
 *
 * <p>Un {@link Ref} apunta a una instancia de un tipo estructurado que vive en el servidor. Esta
 * clase copia el nombre del tipo y el objeto al que apunta, para que la referencia sobreviva a la
 * conexion.
 *
 * <p>Es la mas debil de las copias de este paquete y hay que decirlo: una referencia <b>solo tiene
 * sentido dentro de su base</b>. Lo que se copia es el valor al que apuntaba en ese momento, no la
 * capacidad de volver a resolverla. {@link #setObject} cambia la copia, no el servidor.
 */
public class SerialRef implements Ref, Serializable, Cloneable {

    private static final long serialVersionUID = -4727123500609662274L;

    /** El nombre del tipo estructurado. */
    private final String baseTypeName;

    /** El objeto al que apuntaba. */
    private Object object;

    /**
     * @param ref la referencia a copiar
     * @throws SQLException si es null o no tiene nombre de tipo
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

    /** El nombre del tipo estructurado. */
    public String getBaseTypeName() throws SerialException {
        return this.baseTypeName;
    }

    /**
     * El objeto, traduciendo el tipo con ese mapa.
     *
     * <p>El mapa se ignora en esta copia: la traduccion la hace el driver al leer, y aca ya se leyo.
     * Es lo que hace el JDK.
     */
    public Object getObject(Map<String, Class<?>> map) throws SerialException {
        if (map == null) {
            throw new SerialException("Invalid Map object: no mapping between SQL type and Java "
                + "class");
        }
        return this.object;
    }

    /** El objeto al que apuntaba. */
    public Object getObject() throws SerialException {
        return this.object;
    }

    /** Cambia el objeto de <b>esta copia</b>. Ver la nota de la clase. */
    public void setObject(Object obj) throws SerialException {
        this.object = obj;
    }

    /** Iguales si coinciden el tipo y el objeto. */
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

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        return 31 * this.baseTypeName.hashCode()
            + (this.object == null ? 0 : this.object.hashCode());
    }

    /** Una copia; comparte el objeto. */
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
