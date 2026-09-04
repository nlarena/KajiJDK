package javax.management;

import java.io.Serializable;

/**
 * Un par nombre-valor: lo que se lee o se escribe de un MBean de una vez.
 *
 * <p>Es inmutable y sin tipo declarado: el valor es un `Object` porque el mismo par tiene que poder
 * viajar por {@link MBeanServer#setAttribute} sin que el servidor sepa nada de la clase del MBean.
 * El tipo lo declara aparte {@link MBeanAttributeInfo}; aca solo va el dato.
 */
public class Attribute implements Serializable {

    private static final long serialVersionUID = 2484220110589082382L;

    /**
     * @serial el nombre del atributo
     */
    private String name;

    /**
     * @serial el valor
     */
    private Object value;

    /**
     * @throws RuntimeOperationsException envolviendo un `IllegalArgumentException` si `name` es
     *     `null`. Es no verificada a proposito: un nombre nulo es un error de programa, no una
     *     condicion que valga la pena atender.
     */
    public Attribute(String name, Object value) {
        if (name == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute name cannot be null"));
        }
        this.name = name;
        this.value = value;
    }

    /** El nombre del atributo. */
    public String getName() {
        return name;
    }

    /** El valor, que puede ser `null`. */
    public Object getValue() {
        return value;
    }

    /** Por nombre y valor; un valor `null` solo iguala a otro `null`. */
    public boolean equals(Object object) {
        if (!(object instanceof Attribute)) {
            return false;
        }
        Attribute otro = (Attribute) object;
        if (value == null) {
            return otro.getValue() == null && name.equals(otro.getName());
        }
        return name.equals(otro.getName()) && value.equals(otro.getValue());
    }

    public int hashCode() {
        return name.hashCode() ^ (value == null ? 0 : value.hashCode());
    }

    /** {@code nombre = valor}. */
    public String toString() {
        return getName() + " = " + getValue();
    }
}
