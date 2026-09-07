package com.sun.jdi;

/**
 * Una variable local de un metodo.
 *
 * @since 1.3
 */
public interface LocalVariable extends Mirror, Comparable<LocalVariable> {

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();

    /**
     * El type name.
     *
     * @return el resultado
     */
    String typeName();

    /**
     * El tipo.
     *
     * @return el resultado
     * @throws ClassNotLoadedException si corresponde
     */
    Type type()
            throws ClassNotLoadedException;

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
     * Si visible.
     *
     * @param frame el StackFrame
     * @return el resultado
     */
    boolean isVisible(StackFrame frame);

    /**
     * Si argument.
     *
     * @return el resultado
     */
    boolean isArgument();

    /**
     * Dos reflejos son iguales si nombran a lo mismo en la misma VM.
     *
     * @param obj el Object
     * @return el resultado
     */
    boolean equals(Object obj);

    /**
     * Coherente con {@link #equals}.
     *
     * @return el resultado
     */
    int hashCode();
}
