package java.util.concurrent;

// An {@link Executor} with a lifecycle and results: tasks can be submitted for a
// {@link Future}, and the service can be shut down and waited on. This is the interface
// application code normally holds — a pool, hidden behind its contract.
//
// Subset: invokeAll / invokeAny (batch submission) and shutdownNow are omitted; the
// `throws InterruptedException` clauses are omitted package-wide (no interruption in
// KajiJDK, and a throws clause is not part of the descriptor).
public interface ExecutorService extends Executor, AutoCloseable {

    // Stop accepting tasks; those already submitted still run.
    void shutdown();

    boolean isShutdown();

    // True once shutdown completed and every task has finished.
    boolean isTerminated();

    // Wait for termination, up to the given time; reports whether it terminated.
    boolean awaitTermination(long timeout, TimeUnit unit);

    // Submit a value-returning task.
    <T> Future<T> submit(Callable<T> task);

    // Submit a Runnable; the Future yields `result` when it completes.
    <T> Future<T> submit(Runnable task, T result);

    // Submit a Runnable; the Future yields null when it completes.
    Future<?> submit(Runnable task);

    /**
     * Shuts the service down and waits for the tasks already submitted to finish -- which is what
     * lets an ExecutorService be a try-with-resources resource (#276).
     *
     * <p>It is a {@code default} and not an abstract method on purpose: it is one in the JDK too,
     * so every implementation gets the behaviour without writing it, and no existing implementor
     * of this interface breaks by the addition.
     *
     * <p>Waits in slices rather than forever so that a service that never terminates does not
     * wedge the caller with no way out. The JDK's version waits indefinitely and handles
     * interruption; there is no interruption to handle here, so the honest form is a bounded loop
     * that gives up rather than one that pretends to wait for ever.
     */
    @Override
    default void close() {
        this.shutdown();
        int rounds = 0;
        while (!this.isTerminated() && rounds < 100) {
            this.awaitTermination(100L, TimeUnit.MILLISECONDS);
            rounds = rounds + 1;
        }
    }
}
