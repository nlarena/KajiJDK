package com.sun.jdi;

/**
 * Un punto en el codigo: una clase, un metodo y un indice de bytecode.
 *
 * <p>{@code lineNumber} y {@code sourceName} pueden no estar: dependen de que la clase se haya
 * compilado con la informacion de depuracion, que es opcional. Cuando falta tiran
 * {@link AbsentInformationException} y lo unico que ubica el punto es {@code codeIndex}.
 *
 * @since 1.3
 */
public interface Location extends Mirror, Comparable<Location> {

    /**
     * El declaring type.
     *
     * @return el resultado
     */
    ReferenceType declaringType();

    /**
     * El method.
     *
     * @return el resultado
     */
    Method method();

    /**
     * El code index.
     *
     * @return el resultado
     */
    long codeIndex();

    /**
     * El source name.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    String sourceName()
            throws AbsentInformationException;

    /**
     * El source name.
     *
     * @param name el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    String sourceName(String name)
            throws AbsentInformationException;

    /**
     * El source path.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    String sourcePath()
            throws AbsentInformationException;

    /**
     * El source path.
     *
     * @param name el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    String sourcePath(String name)
            throws AbsentInformationException;

    /**
     * El line number.
     *
     * @return el resultado
     */
    int lineNumber();

    /**
     * El line number.
     *
     * @param name el String
     * @return el resultado
     */
    int lineNumber(String name);

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
