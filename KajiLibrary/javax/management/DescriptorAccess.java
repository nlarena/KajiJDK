package javax.management;

/**
 * Lectura y escritura del descriptor.
 *
 * <p>Solo lo implementan las piezas de `javax.management.modelmbean`, que son las que se configuran
 * en caliente. Los `MBean*Info` de este paquete son inmutables y por eso se quedan en
 * {@link DescriptorRead}.
 */
public interface DescriptorAccess extends DescriptorRead {

    /** Reemplaza el descriptor entero. */
    void setDescriptor(Descriptor inDescriptor);
}
