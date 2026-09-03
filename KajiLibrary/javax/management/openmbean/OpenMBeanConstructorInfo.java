package javax.management.openmbean;

import javax.management.MBeanParameterInfo;

/**
 * La descripción de un constructor de un MBean abierto.
 *
 * <p>`getSignature` devuelve `MBeanParameterInfo[]` y no `OpenMBeanParameterInfo[]`, que es lo que
 * uno esperaría acá. No es un descuido del JDK: es el tipo que hereda de `MBeanConstructorInfo`, y
 * cambiarlo rompería la compatibilidad. Los elementos **son** `OpenMBeanParameterInfo`, así que el
 * cast es seguro; lo que no hay es forma de decirlo en la firma.
 */
public interface OpenMBeanConstructorInfo {

    /** La descripción, para una persona. */
    String getDescription();

    /** El nombre del constructor. */
    String getName();

    /** Los parámetros. Ver la nota de la clase sobre su tipo. */
    MBeanParameterInfo[] getSignature();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
