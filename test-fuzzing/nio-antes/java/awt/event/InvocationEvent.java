package java.awt.event;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;

/**
 * Work to be run on the event thread.
 *
 * <p>It is the piece that settles AWT's strictest rule: **the interface is only touched from the
 * event thread**. A thread wanting to change something on screen does not do it directly; it queues
 * one of these with what has to be done, and the event thread takes it out in its turn and runs it.
 *
 * <p>It attends to itself, with no listeners: it implements {@link ActiveEvent}, so the queue calls
 * {@link #dispatch} on it and it runs what is its own.
 *
 * <p>Exceptions may be caught or let through. Catching and storing them serves the **synchronous**
 * calls: the waiting thread needs to hear that the task failed, and an exception propagating on the
 * event thread would never reach it.
 */
public class InvocationEvent extends AWTEvent implements ActiveEvent {

    private static final long serialVersionUID = 436056344909459450L;

    /** The usual identifier. */
    public static final int INVOCATION_DEFAULT = 1200;

    /** The family's first identifier. */
    public static final int INVOCATION_FIRST = 1200;

    /** The family's last identifier. */
    public static final int INVOCATION_LAST = 1200;

    /** What has to be done. */
    protected Runnable runnable;

    /** What to notify on once it has finished, or `null`. */
    protected volatile Object notifier;

    /** Whether the exceptions have to be caught instead of let through. */
    protected boolean catchExceptions;

    private Runnable listener;
    private volatile boolean dispatched;
    private Throwable throwable;
    private final long when;

    /**
     * With the task, letting the exceptions through.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public InvocationEvent(Object source, Runnable runnable) {
        this(source, INVOCATION_DEFAULT, runnable, null, false);
    }

    /**
     * With an object to notify on when it finishes.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public InvocationEvent(Object source, Runnable runnable, Object notifier,
            boolean catchThrowables) {
        this(source, INVOCATION_DEFAULT, runnable, notifier, catchThrowables);
    }

    /**
     * With a task that is run when it finishes.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public InvocationEvent(Object source, Runnable runnable, Runnable listener,
            boolean catchThrowables) {
        this(source, INVOCATION_DEFAULT, runnable, null, catchThrowables);
        this.listener = listener;
    }

    /**
     * The general constructor, for the subclasses.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    protected InvocationEvent(Object source, int id, Runnable runnable, Object notifier,
            boolean catchThrowables) {
        super(source, id);
        this.runnable = runnable;
        this.notifier = notifier;
        this.catchExceptions = catchThrowables;
        this.when = System.currentTimeMillis();
    }

    /**
     * Runs the task and reports that it finished.
     *
     * <p>The notice goes in a `finally`: if the task throws and the exceptions are not caught, the
     * waiting thread has to wake up all the same, or it hangs for ever.
     */
    public void dispatch() {
        try {
            if (this.catchExceptions) {
                try {
                    this.runnable.run();
                } catch (Throwable t) {
                    this.throwable = t;
                }
            } else {
                this.runnable.run();
            }
        } finally {
            this.finishedDispatching();
        }
    }

    /** Marks that it finished and wakes whoever was waiting. */
    private void finishedDispatching() {
        this.dispatched = true;
        Object n = this.notifier;
        if (n != null) {
            synchronized (n) {
                n.notifyAll();
            }
        }
        if (this.listener != null) {
            this.listener.run();
        }
    }

    /**
     * The exception the task threw, if it was an `Exception`.
     *
     * @return the exception, or `null` if there was none or if it was an `Error`
     */
    public Exception getException() {
        if (this.catchExceptions && this.throwable instanceof Exception) {
            return (Exception) this.throwable;
        }
        return null;
    }

    /**
     * Whatever the task threw.
     *
     * <p>Unlike {@link #getException}, it includes the `Error`s.
     */
    public Throwable getThrowable() {
        if (this.catchExceptions) {
            return this.throwable;
        }
        return null;
    }

    /** When it was queued. */
    public long getWhen() {
        return this.when;
    }

    /** Whether it has already run. */
    public boolean isDispatched() {
        return this.dispatched;
    }

    public String paramString() {
        String type = this.id == INVOCATION_DEFAULT ? "INVOCATION_DEFAULT" : "unknown type";
        return type + ",runnable=" + this.runnable + ",notifier=" + this.notifier
                + ",catchExceptions=" + this.catchExceptions + ",when=" + this.when;
    }
}
