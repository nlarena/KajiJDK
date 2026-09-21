package com.sun.jdi.request;

import com.sun.jdi.ReferenceType;

/**
 * Ask to be told when a class is prepared.
 *
 * @since 1.3
 */
public interface ClassPrepareRequest extends EventRequest {

    /**
     * It filters by class; only with the request disabled.
     *
     * @param type the ReferenceType
     */
    void addClassFilter(ReferenceType type);

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

    /**
     * It filters by source name; only with the request disabled.
     *
     * @param name the String
     */
    void addSourceNameFilter(String name);
}
