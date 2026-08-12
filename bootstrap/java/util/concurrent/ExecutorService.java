package java.util.concurrent;

// Minimal java.util.concurrent.ExecutorService — an Executor you can hand tasks to and shut down.
// (Simplified vs. the JDK: `awaitTermination` takes no timeout — it blocks until every worker has
// exited — and `submit(Callable)`, `invokeAll`, `shutdownNow`, etc. are omitted.)
public interface ExecutorService extends Executor {
    Future<?> submit(Runnable task);

    void shutdown();

    void awaitTermination() throws InterruptedException;
}
