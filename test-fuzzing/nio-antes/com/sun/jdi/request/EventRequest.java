package com.sun.jdi.request;

import com.sun.jdi.Mirror;

/**
 * A request to be told: the half of JDI that says <strong>what</strong> one wants to know.
 *
 * <h2>It is configured switched off and afterwards switched on</h2>
 *
 * <p>A request is born disabled. The filters -- by thread, by class, by instance -- may only be
 * set while it is switched off, and {@link #enable} activates it. Trying to filter a request
 * that is already enabled throws {@link InvalidRequestStateException}.
 *
 * <p>The order is not a whim: the filters are translated into configuration of the other VM,
 * and changing them with the request active would leave events in flight with the old filter.
 *
 * <h2>The suspension policy is the important decision</h2>
 *
 * <p>{@link #setSuspendPolicy} decides what is frozen when the event arrives:
 * {@link #SUSPEND_NONE} nothing, {@link #SUSPEND_EVENT_THREAD} the thread that generated it,
 * {@link #SUSPEND_ALL} the whole program.
 *
 * <p>{@code SUSPEND_ALL} is what a debugger wants for a breakpoint and the worst possible thing
 * for a frequent event: it freezes everything, thousands of times a second.
 *
 * <h2>Filtering is an optimization, not a convenience</h2>
 *
 * <p>With no filter, each occurrence crosses the connection. An unfiltered
 * {@code MethodEntryRequest} over a real program sends millions of events and makes it
 * unusable. The filter is applied <strong>on the debugged VM's side</strong>, which is what
 * avoids the trip.
 *
 * @since 1.3
 */
public interface EventRequest extends Mirror {

    /** Do not suspend anything when the event arrives. */
    int SUSPEND_NONE = 0;

    /** Suspend only the thread that generated the event. */
    int SUSPEND_EVENT_THREAD = 1;

    /**
     * Suspend every thread.
     *
     * <p>It is what a breakpoint wants and the worst possible thing for a frequent event.
     */
    int SUSPEND_ALL = 2;

    /**
     * Whether enabled.
     *
     * @return the result
     */
    boolean isEnabled();

    /**
     * It fixes the enabled.
     *
     * @param flag the boolean
     */
    void setEnabled(boolean flag);

    /**
     * The enable.
     */
    void enable();

    /**
     * The disable.
     */
    void disable();

    /**
     * It filters by count; only with the request disabled.
     *
     * @param index the int
     */
    void addCountFilter(int index);

    /**
     * It fixes the suspend policy.
     *
     * @param index the int
     */
    void setSuspendPolicy(int index);

    /**
     * The suspend policy.
     *
     * @return the result
     */
    int suspendPolicy();

    /**
     * The put property.
     *
     * @param obj the Object
     * @param obj2 the Object
     */
    void putProperty(Object obj, Object obj2);

    /**
     * The property.
     *
     * @param obj the Object
     * @return the result
     */
    Object getProperty(Object obj);
}
