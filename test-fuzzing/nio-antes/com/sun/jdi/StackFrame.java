package com.sun.jdi;

import java.util.List;
import java.util.Map;

/**
 * A frame of a suspended thread's stack.
 *
 * <p>It holds <strong>only while the thread stays suspended at the same point</strong>.
 * Resuming it invalidates all its frames, and using them afterwards throws
 * {@link InvalidStackFrameException}.
 *
 * <p>It is the classic mistake of whoever writes a debugger: keeping a frame in order to look
 * at it later. It has to be asked for again after each suspension.
 *
 * @since 1.3
 */
public interface StackFrame extends Mirror,Locatable {

    /**
     * The location.
     *
     * @return the result
     */
    Location location();

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();

    /**
     * The this object.
     *
     * @return the result
     */
    ObjectReference thisObject();

    /**
     * The visible variables.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<LocalVariable> visibleVariables()
            throws AbsentInformationException;

    /**
     * The visible variable by name.
     *
     * @param name the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    LocalVariable visibleVariableByName(String name)
            throws AbsentInformationException;

    /**
     * The value.
     *
     * @param variable the LocalVariable
     * @return the result
     */
    Value getValue(LocalVariable variable);

    /**
     * The values.
     *
     * @param values the List<? extends LocalVariable>
     * @return the result
     */
    Map<LocalVariable, Value> getValues(List<? extends LocalVariable> values);

    /**
     * It fixes the value.
     *
     * @param variable the LocalVariable
     * @param value the Value
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     */
    void setValue(LocalVariable variable, Value value)
            throws InvalidTypeException, ClassNotLoadedException;

    /**
     * The argument values.
     *
     * @return the result
     */
    List<Value> getArgumentValues();
}
