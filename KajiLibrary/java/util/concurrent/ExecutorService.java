package java.util.concurrent;

// An {@link Executor} with a lifecycle and results: tasks can be submitted for a
// {@link Future}, and the service can be shut down and waited on. This is the interface
// application code normally holds — a pool, hidden behind its contract.
//
// Subset: invokeAll / invokeAny (batch submission) and shutdownNow are omitted; the
// `throws InterruptedException` clauses are omitted package-wide (no interruption in
// KajiJDK, and a throws clause is not part of the descriptor).
public interface ExecutorService extends Executor {

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
}
