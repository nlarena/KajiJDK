package java.util.concurrent;

// Minimal java.util.concurrent.Future — a handle to an async result: block for it (`get`) or ask if
// it is ready (`isDone`). (Simplified vs. the JDK: no cancel/timeout, and a task that throws is
// reported as a `null` result rather than through `ExecutionException`.)
public interface Future<V> {
    V get() throws InterruptedException;

    boolean isDone();
}
