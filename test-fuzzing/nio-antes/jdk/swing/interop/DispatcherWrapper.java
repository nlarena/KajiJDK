package jdk.swing.interop;

import java.awt.DispatcherAccess;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;

/**
 * Whoever takes over AWT's event dispatching when another toolkit is in charge.
 *
 * <h2>The problem</h2>
 *
 * <p>JavaFX and AWT each have their own event thread. Embedding Swing inside JavaFX -- which is what
 * this package exists for -- would have both threads touching the same components, and no amount of
 * synchronization fixes that: the order in which things happen would stop being defined.
 *
 * <p>The way out is that one of them gives in. AWT stops serving its queue and hands every dispatch
 * to this wrapper, which runs it on the other toolkit's thread. From there on there is a single
 * thread touching the interface.
 *
 * <h2>The three questions</h2>
 *
 * <p>{@link #isDispatchThread} because half of Swing asks whether it is on the right thread before
 * doing anything. {@link #scheduleDispatch} because it is the only way to reach that thread.
 * {@link #createSecondaryLoop} because a modal dialog has to block whoever opened it without
 * freezing the event thread, and the only one who knows how is whoever runs that thread.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>It works: {@link #setFwDispatcher} installs the wrapper into the real queue, and from then on
 * that queue's dispatches, its question about the thread, and its secondary loops all come through
 * here.
 *
 * @since 9
 */
public abstract class DispatcherWrapper {

    private final DispatcherAccess.Dispatcher fwd = new Bridge(this);

    /** One. */
    public DispatcherWrapper() {
    }

    /**
     * Whether the current thread is the one this dispatcher runs events on.
     *
     * @return true if it is
     */
    public abstract boolean isDispatchThread();

    /**
     * Runs that on the dispatch thread, when it can.
     *
     * <p>Returns right away: the caller is almost always on another thread and cannot wait.
     *
     * @param r what to run
     */
    public abstract void scheduleDispatch(Runnable r);

    /**
     * A secondary loop of its own.
     *
     * @return the loop
     */
    public abstract SecondaryLoop createSecondaryLoop();

    /**
     * Makes that queue give up dispatching to that wrapper.
     *
     * <p>Passing {@code null} as the dispatcher does not give the queue back its own thread: the JDK
     * dereferences the wrapper before it looks at the queue, so it throws. It is left that way
     * because the only real use is installing, and a call with {@code null} is a mistake by the
     * caller.
     *
     * @param eventQueue the queue giving up dispatching
     * @param dispatcher whoever takes over
     * @throws NullPointerException if either of the two is {@code null}
     */
    public static void setFwDispatcher(EventQueue eventQueue, DispatcherWrapper dispatcher) {
        DispatcherAccess.install(eventQueue, dispatcher.fwd);
    }

    /** What `java.awt` sees of this wrapper; the names on that side are different. */
    private static final class Bridge implements DispatcherAccess.Dispatcher {

        private final DispatcherWrapper d;

        Bridge(DispatcherWrapper d) {
            this.d = d;
        }

        public boolean isDispatchThread() {
            return this.d.isDispatchThread();
        }

        public void scheduleDispatch(Runnable task) {
            this.d.scheduleDispatch(task);
        }

        public SecondaryLoop secondaryLoop() {
            return this.d.createSecondaryLoop();
        }
    }
}
