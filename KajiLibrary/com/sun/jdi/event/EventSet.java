package com.sun.jdi.event;

import com.sun.jdi.Mirror;
import java.util.Set;

/**
 * The events that happened <strong>at the same point</strong>, delivered together.
 *
 * <h2>Why they come as a set and not one at a time</h2>
 *
 * <p>Because at one and the same instant more than one thing may have happened: a breakpoint and
 * the end of a step, on the same line. Delivering them separately would force the debugger to
 * guess whether the second is of the same moment as the first.
 *
 * <h2>The set decides the suspension</h2>
 *
 * <p>And this is what has to be understood before writing a debugger: when a set arrives, the
 * threads its requests indicated are already <strong>suspended</strong>. They stay suspended
 * until somebody calls {@link #resume}.
 *
 * <p>Forgetting to resume leaves the debugged program frozen with no error in sight. It is the
 * commonest mistake with this API.
 *
 * @since 1.3
 */
public interface EventSet extends Mirror, Set<Event> {

    /**
     * The suspend policy.
     *
     * @return the result
     */
    int suspendPolicy();

    /**
     * The event iterator.
     *
     * @return the result
     */
    EventIterator eventIterator();

    /**
     * The resume.
     */
    void resume();
}
