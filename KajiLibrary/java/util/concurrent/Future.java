package java.util.concurrent;

// A handle on a result that does not exist yet: the task was handed to an executor, and
// this is the receipt. {@link #get} blocks until the answer is there, turning an
// asynchronous submission back into a synchronous read at the moment the caller needs it.
public interface Future<V> {

    // Where a task stands, for a caller that does not want to block to find out. The four
    // constants are the JDK's, and the order matters: a Future is RUNNING until it is done,
    // and exactly one of the three terminal states afterwards.
    enum State {
        // Not finished yet.
        RUNNING,
        // Finished and produced a value.
        SUCCESS,
        // Finished by throwing.
        FAILED,
        // Cancelled before it could produce a value.
        CANCELLED
    }

    // Attempt to cancel; reports whether it succeeded (a finished task cannot be).
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    // The result, waiting for it if the task has not finished. Raises the task's own
    // failure wrapped in an ExecutionException.
    V get() throws InterruptedException, ExecutionException;

    // The result, waiting at most the given time.
    V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Where the task stands, without blocking.
     *
     * <p>Written as a {@code default} rather than an abstract method for the same reason as in the
     * JDK: it is derivable from the three predicates every implementor already writes, so adding it
     * costs no implementor anything. The one call to {@link #get} it makes cannot block, because it
     * happens only after {@link #isDone} has said the answer is already there.
     *
     * <p>The JDK retries {@code get} in a loop around {@code InterruptedException}; here the catch
     * re-marks the thread and answers once. The loop exists there to survive an interrupt flag that
     * was already set on entry, and this library's {@code get} does not consult that flag when the
     * task is already done -- so a second attempt would take the same branch as the first.
     */
    default State state() {
        State s;
        if (!isDone()) {
            s = State.RUNNING;
        } else if (isCancelled()) {
            s = State.CANCELLED;
        } else {
            try {
                get();
                s = State.SUCCESS;
            } catch (InterruptedException e) {
                // `state()` declares no `throws`, and the JDK's does not either: it is a query, not
                // a wait. That `get()` can block here is only because the task already finished --it
                // was reached through `isDone()`-- so the interruption should never arrive. If it
                // does, the thread is re-marked and the only thing known is reported: that the task
                // ran.
                Thread.currentThread().interrupt();
                s = State.RUNNING;
            } catch (CancellationException e) {
                // A cancellation that landed between isDone() and get(); still CANCELLED.
                s = State.CANCELLED;
            } catch (ExecutionException e) {
                s = State.FAILED;
            }
        }
        return s;
    }

    /**
     * The result of a task already known to have succeeded.
     *
     * @throws IllegalStateException if the task is unfinished, failed or cancelled -- asking those
     *         for a value is a bug in the caller, and returning null would hide it
     */
    default V resultNow() {
        if (!isDone()) {
            throw new IllegalStateException("Task has not completed");
        }
        V value = null;
        String failure = null;
        try {
            value = get();
        } catch (InterruptedException e) {
            // The same reason as in state(): this was reached with the task already finished, so
            // get() does not wait and the interruption should not arrive. If it does, the thread is
            // re-marked -- swallowing it would be worse than the problem -- and it is reported as
            // "not completed", which is the only thing this method can say without inventing a
            // value.
            Thread.currentThread().interrupt();
            failure = "Task has not completed";
        } catch (CancellationException e) {
            failure = "Task was cancelled";
        } catch (ExecutionException e) {
            failure = "Task completed with exception";
        }
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
        return value;
    }

    /**
     * The exception a task already known to have failed threw -- the cause, not the
     * {@link ExecutionException} wrapper, which is an artefact of {@code get} and not of the task.
     *
     * @throws IllegalStateException if the task is unfinished, cancelled, or succeeded
     */
    default Throwable exceptionNow() {
        if (!isDone()) {
            throw new IllegalStateException("Task has not completed");
        }
        if (isCancelled()) {
            throw new IllegalStateException("Task was cancelled");
        }
        Throwable cause = null;
        String failure = "Task completed with a result";
        try {
            get();
        } catch (InterruptedException e) {
            // As in state() and resultNow(): the task already finished, get() does not wait.
            Thread.currentThread().interrupt();
            failure = "Task has not completed";
        } catch (ExecutionException e) {
            cause = e.getCause();
            failure = null;
        }
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
        return cause;
    }
}
