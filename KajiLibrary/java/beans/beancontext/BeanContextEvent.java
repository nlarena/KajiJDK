package java.beans.beancontext;

import java.util.EventObject;

/**
 * The root of this API's events.
 *
 * <p>What it adds over {@link EventObject} is **propagation**: a nested context that forwards to
 * its children the events it receives from its parent is meant to mark where they came from.
 * Without that mark a listener could not tell an event of the context it listens to from one that
 * only passed through, and would forward it again — a cycle in a hierarchy with more than one path.
 *
 * <p>This note said `setPropagatedFrom` may be called once and only by the forwarding context, and
 * in the next sentence that nothing here restricts it. The second half is the true one: any caller
 * may set it, any number of times. Nothing in this tree calls it either —
 * {@link BeanContextServicesSupport} forwards events without marking them.
 */
public abstract class BeanContextEvent extends EventObject {

    /** The context this event was propagated from, or `null` if it is first-hand. */
    protected BeanContext propagatedFrom;

    /** The event originating in that context. */
    protected BeanContextEvent(BeanContext bc) {
        super(bc);
    }

    /** The context that originated the event. */
    public BeanContext getBeanContext() {
        return (BeanContext) this.getSource();
    }

    /** Marks where it was propagated from. `null` makes it first-hand again. */
    public synchronized void setPropagatedFrom(BeanContext bc) {
        this.propagatedFrom = bc;
    }

    /** Where it was propagated from, or `null`. */
    public synchronized BeanContext getPropagatedFrom() {
        return this.propagatedFrom;
    }

    /** Whether this event was forwarded from another context. */
    public synchronized boolean isPropagated() {
        return this.propagatedFrom != null;
    }
}
