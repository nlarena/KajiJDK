package javax.management.openmbean;

/**
 * La descripción de un atributo de un MBean abierto: un parámetro más cómo se accede.
 *
 * <p>`isIs` distingue un getter llamado `isFoo()` de uno llamado `getFoo()`. Sólo puede ser `true`
 * si el tipo es `SimpleType.BOOLEAN`, que es la única forma en que Java permite el prefijo `is`.
 */
public interface OpenMBeanAttributeInfo extends OpenMBeanParameterInfo {

    /** Si se puede leer. */
    boolean isReadable();

    /** Si se puede escribir. */
    boolean isWritable();

    /** Si su getter se llama `isXxx` en vez de `getXxx`. */
    boolean isIs();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
