package com.sun.jdi.request;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Ask to be told when an exception is thrown.
 *
 * <p>The constructor's two booleans decide whether the caught ones, the uncaught ones or both
 * are of interest. Filtering by "uncaught" is what turns this into a usable tool: a normal
 * program throws and catches exceptions all the time.
 *
 * @since 1.3
 */
public interface ExceptionRequest extends EventRequest {

    /**
     * The exception.
     *
     * @return the result
     */
    ReferenceType exception();

    /**
     * The notify caught.
     *
     * @return the result
     */
    boolean notifyCaught();

    /**
     * The notify uncaught.
     *
     * @return the result
     */
    boolean notifyUncaught();

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
