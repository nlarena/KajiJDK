package com.sun.jdi;

import java.util.List;

/**
 * A thread group of the debugged machine.
 *
 * @since 1.3
 */
public interface ThreadGroupReference extends ObjectReference {

    /**
     * The name.
     *
     * @return the result
     */
    String name();

    /**
     * The parent.
     *
     * @return the result
     */
    ThreadGroupReference parent();

    /**
     * The suspend.
     */
    void suspend();

    /**
     * The resume.
     */
    void resume();

    /**
     * The threads.
     *
     * @return the result
     */
    List<ThreadReference> threads();

    /**
     * The thread groups.
     *
     * @return the result
     */
    List<ThreadGroupReference> threadGroups();
}
