package com.sun.jdi;

import java.util.List;

/**
 * Un arreglo de la maquina depurada.
 *
 * @since 1.3
 */
public interface ArrayReference extends ObjectReference {

    /**
     * El length.
     *
     * @return el resultado
     */
    int length();

    /**
     * El value.
     *
     * @param index el int
     * @return el resultado
     */
    Value getValue(int index);

    /**
     * El values.
     *
     * @return el resultado
     */
    List<Value> getValues();

    /**
     * El values.
     *
     * @param index el int
     * @param index2 el int
     * @return el resultado
     */
    List<Value> getValues(int index, int index2);

    /**
     * Fija el value.
     *
     * @param index el int
     * @param value el Value
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     */
    void setValue(int index, Value value)
            throws InvalidTypeException, ClassNotLoadedException;

    /**
     * Fija el values.
     *
     * @param values el List<? extends Value>
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     */
    void setValues(List<? extends Value> values)
            throws InvalidTypeException, ClassNotLoadedException;

    /**
     * Fija el values.
     *
     * @param index el int
     * @param values el List<? extends Value>
     * @param index2 el int
     * @param index3 el int
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     */
    void setValues(int index, List<? extends Value> values, int index2, int index3)
            throws InvalidTypeException, ClassNotLoadedException;
}
