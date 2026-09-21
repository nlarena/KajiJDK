package java.util.concurrent.locks;

import java.io.Serializable;

// A synchronizer that can be **owned by a thread**. It does nothing by itself: it only stores who
// holds it in exclusive mode, so that diagnostic tools --and subclasses-- can ask. It is the root of
// the hierarchy: both `AbstractQueuedSynchronizer` and `AbstractQueuedLongSynchronizer` inherit from
// here.
//
// The class has neither a public constructor nor public methods on purpose: the two accessors are
// `protected final`, which is to say only the subclass's code touches the field, and no subclass can
// change how it is stored. The field is `transient` --as in the JDK--: a lock's ownership is
// execution state, not something it makes sense to serialise.
public abstract class AbstractOwnableSynchronizer implements Serializable {

    // The thread that holds the synchronizer in exclusive mode, or `null` if it is free.
    private transient Thread exclusiveOwnerThread;

    /** For subclasses' use only. */
    protected AbstractOwnableSynchronizer() {
    }

    /**
     * It records that `thread` is the exclusive owner. `null` to say there is none.
     *
     * <p>Nothing is checked: the JDK does not check either. The caller is the subclass, and it is
     * the one that knows whether it has just won the race for the state.
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        this.exclusiveOwnerThread = thread;
    }

    /** The last thread noted with {@link #setExclusiveOwnerThread}, or `null`. */
    protected final Thread getExclusiveOwnerThread() {
        return this.exclusiveOwnerThread;
    }
}
