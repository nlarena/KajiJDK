package com.sun.jdi.request;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Ask to be told on leaving a method.
 *
 * @since 1.3
 */
public interface MethodExitRequest extends EventRequest {

    /**
     * It filters by thread; only with the request disabled.
     *
     * @param thread the ThreadReference
     */
    void addThreadFilter(ThreadReference thread);

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
     * It filters by instance; only with the request disabled.
     *
     * @param object the ObjectReference
     */
    void addInstanceFilter(ObjectReference object);
}
