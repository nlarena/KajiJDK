package com.sun.jdi;

/**
 * What has access modifiers: a class, a field or a method.
 *
 * @since 1.3
 */
public interface Accessible {

    /**
     * The modifiers.
     *
     * @return the result
     */
    int modifiers();

    /**
     * Whether private.
     *
     * @return the result
     */
    boolean isPrivate();

    /**
     * Whether package private.
     *
     * @return the result
     */
    boolean isPackagePrivate();

    /**
     * Whether protected.
     *
     * @return the result
     */
    boolean isProtected();

    /**
     * Whether public.
     *
     * @return the result
     */
    boolean isPublic();
}
