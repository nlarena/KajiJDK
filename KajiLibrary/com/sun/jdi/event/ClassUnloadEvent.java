package com.sun.jdi.event;

/**
 * A class was unloaded from the debugged machine.
 *
 * @since 1.3
 */
public interface ClassUnloadEvent extends Event {

    /**
     * The class name.
     *
     * @return the result
     */
    String className();

    /**
     * The class signature.
     *
     * @return the result
     */
    String classSignature();
}
