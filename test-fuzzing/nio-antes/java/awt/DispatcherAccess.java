package java.awt;

/**
 * Bridge for installing a foreign dispatcher into an {@link EventQueue}.
 *
 * <p>Not a JDK class: it is scaffolding of ours, the same kind as
 * {@code java.nio.channels.MapModes}. It exists because the {@link EventQueue} method that stores
 * the dispatcher is private -- in the JDK it is reached through {@code AWTAccessor}, the service
 * door the platform modules open for each other -- and {@code jdk.swing.interop} lives in another
 * package.
 *
 * <h2>What a foreign dispatcher is</h2>
 *
 * <p>When AWT shares a process with another toolkit -- JavaFX embedding Swing, which is what
 * {@code jdk.swing.interop} exists for -- there cannot be two event threads fighting over the same
 * components. The way out is that one of them gives in: AWT stops serving its own queue and hands
 * every dispatch to the other one, which runs them on its thread. That is a foreign dispatcher.
 *
 * <p>Installing one changes three things about the queue: who decides whether the current thread is
 * the dispatch thread, who runs each event, and where secondary loops come from. All three are in
 * {@link Dispatcher}.
 */
public final class DispatcherAccess {

    /** Whoever takes over the events of a queue that gave up dispatching. */
    public interface Dispatcher {

        /**
         * Whether the current thread is the one this dispatcher runs events on.
         *
         * @return true if it is
         */
        boolean isDispatchThread();

        /**
         * Runs that on the dispatch thread, when it can.
         *
         * @param task what to run
         */
        void scheduleDispatch(Runnable task);

        /**
         * A secondary loop of its own, so a modal dialog can block without freezing the interface.
         *
         * @return the loop
         */
        SecondaryLoop secondaryLoop();
    }

    private DispatcherAccess() {
    }

    /**
     * Installs that dispatcher into that queue, or takes it out when it is {@code null}.
     *
     * <p>If the queue is stacked under others, the dispatcher goes to the topmost one, which is the
     * one serving: installing it into a covered queue would have no effect.
     *
     * @param queue the queue giving up dispatching
     * @param dispatcher whoever takes over, or {@code null} to undo
     * @throws NullPointerException if the queue is {@code null}
     */
    public static void install(EventQueue queue, Dispatcher dispatcher) {
        if (queue == null) {
            throw new NullPointerException("queue");
        }
        queue.installDispatcher(dispatcher);
    }
}
