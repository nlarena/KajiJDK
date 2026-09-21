package com.sun.jdi;

/**
The type of an array.
 *
 * @since 1.3
 */
public interface ArrayType extends ReferenceType {

    /**
     * The new instance.
     *
     * @param index the int
     * @return the result
     */
    ArrayReference newInstance(int index);

    /**
     * The component signature.
     *
     * @return the result
     */
    String componentSignature();

    /**
     * The component type name.
     *
     * @return the result
     */
    String componentTypeName();

    /**
     * The component type.
     *
     * @return the result
     * @throws ClassNotLoadedException if it applies
     */
    Type componentType()
            throws ClassNotLoadedException;
}
