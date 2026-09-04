package javax.management;

import java.io.Serializable;

/**
 * Un MBean registrado: su nombre mas la clase que lo implementa.
 *
 * <p>Existe para ahorrar un viaje. `queryNames` devuelve nombres y el cliente que quiera saber de
 * que clase es cada uno tendria que preguntar uno por uno; `queryMBeans` devuelve estos y la
 * respuesta ya viene completa. Sobre una conexion remota la diferencia es de un viaje contra
 * cientos.
 *
 * <p>El nombre <b>no</b> puede ser un patron, y el constructor lo verifica: un patron designa un
 * conjunto y esto identifica a uno solo.
 */
public class ObjectInstance implements Serializable {

    private static final long serialVersionUID = -4099952623687795850L;

    /**
     * @serial el nombre del MBean
     */
    private ObjectName name;

    /**
     * @serial el nombre de su clase Java
     */
    private String className;

    /** Analiza `objectName` y delega. */
    public ObjectInstance(String objectName, String className)
            throws MalformedObjectNameException {
        this(new ObjectName(objectName), className);
    }

    /**
     * @throws RuntimeOperationsException envolviendo `IllegalArgumentException` si el nombre es un
     *     patron
     */
    public ObjectInstance(ObjectName objectName, String className) {
        if (objectName.isPattern()) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Invalid name->" + objectName.toString()));
        }
        this.name = objectName;
        this.className = className;
    }

    /** Por nombre y clase; la clase puede ser `null` de los dos lados. */
    public boolean equals(Object object) {
        if (!(object instanceof ObjectInstance)) {
            return false;
        }
        ObjectInstance otro = (ObjectInstance) object;
        if (!name.equals(otro.getObjectName())) {
            return false;
        }
        if (className == null) {
            return otro.getClassName() == null;
        }
        return className.equals(otro.getClassName());
    }

    public int hashCode() {
        return name.hashCode() ^ (className == null ? 0 : className.hashCode());
    }

    /** El nombre del MBean. */
    public ObjectName getObjectName() {
        return name;
    }

    /** El nombre de la clase Java que lo implementa. */
    public String getClassName() {
        return className;
    }

    /** {@code clase[nombre]}. */
    public String toString() {
        return getClassName() + "[" + getObjectName() + "]";
    }
}
