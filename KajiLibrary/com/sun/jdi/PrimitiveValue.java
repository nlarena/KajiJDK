package com.sun.jdi;

/**
 * Un valor primitivo, con el dato adentro.
 *
 * <p>Los metodos de conversion hacen la ampliacion de Java: leer un {@code byte} como {@code int}
 * anda, al reves no.
 *
 * @since 1.3
 */
public interface PrimitiveValue extends Value {

    /**
     * El boolean value.
     *
     * @return el resultado
     */
    boolean booleanValue();

    /**
     * El byte value.
     *
     * @return el resultado
     */
    byte byteValue();

    /**
     * El char value.
     *
     * @return el resultado
     */
    char charValue();

    /**
     * El short value.
     *
     * @return el resultado
     */
    short shortValue();

    /**
     * El int value.
     *
     * @return el resultado
     */
    int intValue();

    /**
     * El long value.
     *
     * @return el resultado
     */
    long longValue();

    /**
     * El float value.
     *
     * @return el resultado
     */
    float floatValue();

    /**
     * El double value.
     *
     * @return el resultado
     */
    double doubleValue();
}
