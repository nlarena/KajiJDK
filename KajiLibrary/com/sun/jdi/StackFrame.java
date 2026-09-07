package com.sun.jdi;

import java.util.List;
import java.util.Map;

/**
 * Un marco de la pila de un hilo suspendido.
 *
 * <p>Vale <strong>solo mientras el hilo siga suspendido en el mismo punto</strong>. Reanudarlo
 * invalida todos sus marcos, y usarlos despues tira {@link InvalidStackFrameException}.
 *
 * <p>Es el error clasico de quien escribe un depurador: guardarse un marco para mirarlo mas tarde.
 * Hay que volver a pedirlo despues de cada suspension.
 *
 * @since 1.3
 */
public interface StackFrame extends Mirror,Locatable {

    /**
     * El location.
     *
     * @return el resultado
     */
    Location location();

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();

    /**
     * El this object.
     *
     * @return el resultado
     */
    ObjectReference thisObject();

    /**
     * El visible variables.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<LocalVariable> visibleVariables()
            throws AbsentInformationException;

    /**
     * El visible variable by name.
     *
     * @param name el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    LocalVariable visibleVariableByName(String name)
            throws AbsentInformationException;

    /**
     * El value.
     *
     * @param variable el LocalVariable
     * @return el resultado
     */
    Value getValue(LocalVariable variable);

    /**
     * El values.
     *
     * @param values el List<? extends LocalVariable>
     * @return el resultado
     */
    Map<LocalVariable, Value> getValues(List<? extends LocalVariable> values);

    /**
     * Fija el value.
     *
     * @param variable el LocalVariable
     * @param value el Value
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     */
    void setValue(LocalVariable variable, Value value)
            throws InvalidTypeException, ClassNotLoadedException;

    /**
     * El argument values.
     *
     * @return el resultado
     */
    List<Value> getArgumentValues();
}
