package javax.management;

/**
 * Lo implementa todo lo que lleva un {@link Descriptor} colgado.
 *
 * <p>Existe para que el codigo que solo quiere leer metadatos no tenga que conocer cual de las seis
 * clases de `MBean*Info` tiene delante.
 */
public interface DescriptorRead {

    /**
     * El descriptor. Nunca `null`: si no hay campos, devuelve uno vacio.
     *
     * <p>Es una copia, porque el descriptor de un `MBeanInfo` es inmutable de hecho aunque su tipo
     * declarado no lo sea.
     */
    Descriptor getDescriptor();
}
