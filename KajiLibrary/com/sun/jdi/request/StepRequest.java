package com.sun.jdi.request;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Ask to be told when a thread advances what is indicated.
 *
 * <p>The size says in what unit -- instruction or line -- and the depth says whether to go into
 * the calls, skip them or leave the current one. They are the three things that in a debugger
 * are called "step into", "step over" and "step out".
 *
 * <p>There may be only one step request per thread: creating the second throws
 * {@link DuplicateRequestException}.
 *
 * @since 1.3
 */
public interface StepRequest extends EventRequest {

    /** Go into the calls. */
    int STEP_INTO = 1;

    /** Skip the calls. */
    int STEP_OVER = 2;

    /** Leave the current method. */
    int STEP_OUT = 3;

    /** Advance one bytecode instruction. */
    int STEP_MIN = -1;

    /** Advance one source line. */
    int STEP_LINE = -2;

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();

    /**
     * The size.
     *
     * @return the result
     */
    int size();

    /**
     * The depth.
     *
     * @return the result
     */
    int depth();

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
