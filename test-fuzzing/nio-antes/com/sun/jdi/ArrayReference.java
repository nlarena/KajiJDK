package com.sun.jdi;

import java.util.List;

/**
 * An array of the debugged machine.
 *
 * @since 1.3
 */
public interface ArrayReference extends ObjectReference {

    /**
     * The length.
     *
     * @return the result
     */
    int length();

    /**
     * The value.
     *
     * @param index the int
     * @return the result
     */
    Value getValue(int index);

    /**
     * The values.
     *
     * @return the result
     */
    List<Value> getValues();

    /**
     * The values.
     *
     * @param index the int
     * @param index2 the int
     * @return the result
     */
    List<Value> getValues(int index, int index2);

    /**
     * It fixes the value.
     *
     * @param index the int
     * @param value the Value
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     */
    void setValue(int index, Value value)
            throws InvalidTypeException, ClassNotLoadedException;

    /**
     * It fixes the values.
     *
     * @param values the List<? extends Value>
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     */
    void setValues(List<? extends Value> values)
            throws InvalidTypeException, ClassNotLoadedException;

    /**
     * It fixes the values.
     *
     * @param index the int
     * @param values the List<? extends Value>
     * @param index2 the int
     * @param index3 the int
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     */
    void setValues(int index, List<? extends Value> values, int index2, int index3)
            throws InvalidTypeException, ClassNotLoadedException;
}
