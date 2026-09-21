package com.sun.jdi;

/**
 * A module of the debugged machine.
 *
 * @since 1.3
 */
public interface ModuleReference extends ObjectReference {

    /**
     * The name.
     *
     * @return the result
     */
    String name();

    /**
     * The class loader.
     *
     * @return the result
     */
    ClassLoaderReference classLoader();
}
