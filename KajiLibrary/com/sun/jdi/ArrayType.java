package com.sun.jdi;

/**
 * El tipo de un arreglo.
 *
 * @since 1.3
 */
public interface ArrayType extends ReferenceType {

    /**
     * El new instance.
     *
     * @param index el int
     * @return el resultado
     */
    ArrayReference newInstance(int index);

    /**
     * El component signature.
     *
     * @return el resultado
     */
    String componentSignature();

    /**
     * El component type name.
     *
     * @return el resultado
     */
    String componentTypeName();

    /**
     * El component type.
     *
     * @return el resultado
     * @throws ClassNotLoadedException si corresponde
     */
    Type componentType()
            throws ClassNotLoadedException;
}
