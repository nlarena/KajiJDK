package java.util.concurrent;

// A handle on a result that does not exist yet: the task was handed to an executor, and
// this is the receipt. {@link #get} blocks until the answer is there, turning an
// asynchronous submission back into a synchronous read at the moment the caller needs it.
//
// The Java 19+ defaults (resultNow / exceptionNow / state) are omitted; so is the nested
// State enum they need.
public interface Future<V> {

    // Attempt to cancel; reports whether it succeeded (a finished task cannot be).
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    // The result, waiting for it if the task has not finished. Raises the task's own
    // failure wrapped in an ExecutionException.
    V get() throws ExecutionException;

    // The result, waiting at most the given time.
    V get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException;
}
