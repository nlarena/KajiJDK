package com.sun.jdi.request;

/**
 * Ask to be told when a class is unloaded.
 *
 * @since 1.3
 */
public interface ClassUnloadRequest extends EventRequest {

    /**
     * It filters by class; only with the request disabled.
     *
     * @param name the String
     */
    void addClassFilter(String name);

    /**
     * It filters by class exclusion; only with the request disabled.
     *
     * @param name the String
     */
    void addClassExclusionFilter(String name);
}
