package javax.management.openmbean;

import javax.management.MBeanParameterInfo;

/**
 * La descripción de una operación de un MBean abierto.
 *
 * <p>Hay dos formas de preguntar por lo que devuelve y no son redundantes: `getReturnType` da el
 * nombre de clase, que es lo que hereda de `MBeanOperationInfo`, y `getReturnOpenType` da el tipo
 * abierto, que es lo que este paquete agrega. El primero se puede deducir del segundo pero no al
 * revés.
 *
 * <p>Sobre el tipo de `getSignature`, vale la misma nota que en {@link OpenMBeanConstructorInfo}.
 */
public interface OpenMBeanOperationInfo {

    /** La descripción, para una persona. */
    String getDescription();

    /** El nombre de la operación. */
    String getName();

    /** Los parámetros. */
    MBeanParameterInfo[] getSignature();

    /** Si lee, escribe, hace las dos cosas, o no se sabe. Ver `MBeanOperationInfo`. */
    int getImpact();

    /** El nombre de clase de lo que devuelve. */
    String getReturnType();

    /** El tipo abierto de lo que devuelve. */
    OpenType<?> getReturnOpenType();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
