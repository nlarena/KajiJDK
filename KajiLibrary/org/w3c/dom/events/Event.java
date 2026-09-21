package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.Event -- something that happened in the document.
 *
 * <h2>The walk in three phases</h2>
 *
 * <p>An event is not delivered only at the node where it happened: it walks the tree twice.
 *
 * <ol>
 *   <li>{@link #CAPTURING_PHASE} -- it goes down from the root to the target. Only the listeners
 *       registered with {@code useCapture = true} see it.
 *   <li>{@link #AT_TARGET} -- it arrives at the node where it happened.
 *   <li>{@link #BUBBLING_PHASE} -- it goes back up to the root, if the event bubbles.
 * </ol>
 *
 * <p>From there comes the difference between {@link #getTarget()} --where it <b>happened</b>,
 * always the same-- and {@link #getCurrentTarget()} --where it <b>is passing</b>, different in each
 * listener--. Reading the second believing it is the first is the classic mistake of this API.
 *
 * <h2>Stopping is not cancelling</h2>
 *
 * <p>The two control methods do different things and are independent:
 *
 * <ul>
 *   <li>{@link #stopPropagation()} cuts the <b>walk</b>: the nodes that are left do not find out.
 *       The default action still happens.
 *   <li>{@link #preventDefault()} cancels the <b>action</b> --following a link, submitting a form--
 *       and the walk goes on. It only serves if the event is cancelable.
 * </ul>
 */
public interface Event {

    /** Going down from the root towards the target. */
    short CAPTURING_PHASE = 1;

    /** At the node where it happened. */
    short AT_TARGET = 2;

    /** Going up from the target towards the root. */
    short BUBBLING_PHASE = 3;

    /**
     * The name of the event: {@code "click"}, {@code "DOMNodeInserted"}. With no {@code "on"}
     * prefix.
     */
    String getType();

    /** Where it <b>happened</b>. It does not change during the walk. */
    EventTarget getTarget();

    /** Where it <b>is passing</b>. It changes in each listener; see the note of the class. */
    EventTarget getCurrentTarget();

    /** Which of the three phases it is in. */
    short getEventPhase();

    /**
     * Whether it goes up through the bubbling phase. An event that does not bubble only reaches the
     * target.
     */
    boolean getBubbles();

    /** Whether {@link #preventDefault()} has any effect on it. */
    boolean getCancelable();

    /**
     * When it happened, in milliseconds since the epoch.
     *
     * <p>It may be 0: the standard admits that an implementation may not have a clock with enough
     * resolution, and returning 0 is how it says so.
     */
    long getTimeStamp();

    /** It cuts the walk. It does not cancel the action; see the note of the class. */
    void stopPropagation();

    /** It cancels the default action. It does not cut the walk. */
    void preventDefault();

    /**
     * It initialises an event newly created by {@code DocumentEvent.createEvent}.
     *
     * <p>It is needed because the event is created empty: the factory takes no arguments. The
     * specification says it may only be called before the event is dispatched (several times, the
     * last one winning); the note said that calling it on an event already being dispatched does
     * nothing, which the specification does not say -- it leaves that case undefined.
     */
    void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg);
}
