package com.sun.jdi.request;

import com.sun.jdi.ThreadReference;

/**
 * Ask to be told when a thread starts.
 *
 * @since 1.3
 */
public interface ThreadStartRequest extends EventRequest {

    /**
     * It filters by thread; only with the request disabled.
     *
     * @param thread the ThreadReference
     */
    void addThreadFilter(ThreadReference thread);

    /**
     * It filters by platform threads only; only with the request disabled.
     */
    void addPlatformThreadsOnlyFilter();
}
