package com.sun.jdi;

/**
 * A member of a type: a field or a method.
 *
 * @since 1.3
 */
public interface TypeComponent extends Mirror,Accessible {

    /**
     * The name.
     *
     * @return the result
     */
    String name();

    /**
     * The signature.
     *
     * @return the result
     */
    String signature();

    /**
     * The generic signature.
     *
     * @return the result
     */
    String genericSignature();

    /**
     * The declaring type.
     *
     * @return the result
     */
    ReferenceType declaringType();

    /**
     * Whether static.
     *
     * @return the result
     */
    boolean isStatic();

    /**
     * Whether final.
     *
     * @return the result
     */
    boolean isFinal();

    /**
     * Whether synthetic.
     *
     * @return the result
     */
    boolean isSynthetic();
}
