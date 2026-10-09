package com.sun.jdi.request;

import com.sun.jdi.Locatable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * Ask to be told when a point of the code is reached.
 *
 * <p>The location is fixed on creating it and cannot be changed: in order to move it, it has to
 * be deleted and another created.
 *
 * @since 1.3
 */
public interface BreakpointRequest extends EventRequest,Locatable {

    /**
     * The location.
     *
     * @return the result
     */
    Location location();

    /**
     * It filters by thread; only with the request disabled.
     *
     * @param thread the ThreadReference
     */
    void addThreadFilter(ThreadReference thread);

    /**
     * It filters by instance; only with the request disabled.
     *
     * @param object the ObjectReference
     */
    void addInstanceFilter(ObjectReference object);
}
