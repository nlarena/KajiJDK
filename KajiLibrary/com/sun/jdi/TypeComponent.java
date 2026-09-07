package com.sun.jdi;

/**
 * Un miembro de un tipo: un campo o un metodo.
 *
 * @since 1.3
 */
public interface TypeComponent extends Mirror,Accessible {

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();

    /**
     * El signature.
     *
     * @return el resultado
     */
    String signature();

    /**
     * El generic signature.
     *
     * @return el resultado
     */
    String genericSignature();

    /**
     * El declaring type.
     *
     * @return el resultado
     */
    ReferenceType declaringType();

    /**
     * Si static.
     *
     * @return el resultado
     */
    boolean isStatic();

    /**
     * Si final.
     *
     * @return el resultado
     */
    boolean isFinal();

    /**
     * Si synthetic.
     *
     * @return el resultado
     */
    boolean isSynthetic();
}
