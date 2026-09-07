package com.sun.jdi;

/**
 * Lo que tiene modificadores de acceso: una clase, un campo o un metodo.
 *
 * @since 1.3
 */
public interface Accessible {

    /**
     * El modifiers.
     *
     * @return el resultado
     */
    int modifiers();

    /**
     * Si private.
     *
     * @return el resultado
     */
    boolean isPrivate();

    /**
     * Si package private.
     *
     * @return el resultado
     */
    boolean isPackagePrivate();

    /**
     * Si protected.
     *
     * @return el resultado
     */
    boolean isProtected();

    /**
     * Si public.
     *
     * @return el resultado
     */
    boolean isPublic();
}
