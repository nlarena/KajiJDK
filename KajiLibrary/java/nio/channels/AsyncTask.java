package java.nio.channels;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

// The bridge between a blocking operation and the two shapes this API hands it back in: a `Future`
// or a `CompletionHandler`.
//
// ===============================================================================================
// WHY ONE PLACE
// ===============================================================================================
//
// Because the two shapes are one operation seen differently, and writing them apart in every channel
// would be writing the same decision fourteen times: what to do when the operation throws, what to
// do when the group is already shut down, and who runs the handler. Here it is decided once.
//
// **The handler runs on the pool, never on the thread that asked for the operation.** That is what
// the API promises and what makes asking for a read not block: if the handler ran here, an
// operation that already had its answer -- a small file, say -- would run the handler before `read`
// returned, and the caller would watch their own handler run inside their own call.
final class AsyncTask {

    private AsyncTask() {
    }

    /**
     * Runs that on the pool and hands back a `Future` to wait on.
     *
     * @param <V> what the work produces
     * @param pool where to run it
     * @param work the operation
     * @return the future
     * @throws ShutdownChannelGroupException if the group takes no more work
     */
    static <V> Future<V> future(ExecutorService pool, Callable<V> work) {
        final FutureTask<V> t = new FutureTask<V>(work);
        try {
            pool.execute(t);
        } catch (RejectedExecutionException e) {
            throw new ShutdownChannelGroupException();
        }
        return t;
    }

    /**
     * Runs that on the pool and tells the handler when it is done.
     *
     * <p>The handler is called exactly once: `completed` when the work returned, `failed` when it
     * threw. Whatever the handler itself throws does not propagate -- nobody could catch it, it runs
     * on a pool thread -- but it does not take the thread down either.
     *
     * @param <V> what the work produces
     * @param <A> the type of the attachment
     * @param pool where to run it
     * @param work the operation
     * @param attachment handed back to the handler untouched
     * @param handler told when the work is done
     * @throws ShutdownChannelGroupException if the group takes no more work
     * @throws NullPointerException if the handler is null
     */
    static <V, A> void notifying(ExecutorService pool, Callable<V> work, A attachment,
            CompletionHandler<V, ? super A> handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        try {
            pool.execute(new Notify<V, A>(work, attachment, handler));
        } catch (RejectedExecutionException e) {
            throw new ShutdownChannelGroupException();
        }
    }

    /**
     * Waits on a future and translates its failure into what this API declares.
     *
     * @param <V> what the future carries
     * @param f the future
     * @param timeout how long to wait, or 0 for as long as it takes
     * @param unit the unit of the timeout
     * @return what the future carried
     * @throws java.io.IOException if the operation failed
     * @throws InterruptedException if the wait was interrupted
     */
    static <V> V await(Future<V> f, long timeout, TimeUnit unit)
            throws java.io.IOException, InterruptedException {
        try {
            return timeout <= 0 ? f.get() : f.get(timeout, unit);
        } catch (java.util.concurrent.TimeoutException e) {
            f.cancel(true);
            throw new InterruptedByTimeoutException();
        } catch (ExecutionException e) {
            final Throwable c = e.getCause();
            if (c instanceof java.io.IOException) {
                throw (java.io.IOException) c;
            }
            if (c instanceof RuntimeException) {
                throw (RuntimeException) c;
            }
            if (c instanceof Error) {
                throw (Error) c;
            }
            throw new java.io.IOException(c);
        }
    }

    /** The work plus the notification, so both run on the same pool thread. */
    private static final class Notify<V, A> implements Runnable {

        private final Callable<V> work;
        private final A attachment;
        private final CompletionHandler<V, ? super A> handler;

        Notify(Callable<V> work, A attachment, CompletionHandler<V, ? super A> handler) {
            this.work = work;
            this.attachment = attachment;
            this.handler = handler;
        }

        public void run() {
            V value;
            try {
                value = this.work.call();
            } catch (Throwable t) {
                try {
                    this.handler.failed(t, this.attachment);
                } catch (Throwable ignored) {
                    // A handler that throws must not take the pool thread with it: the next channel
                    // needs it.
                }
                return;
            }
            try {
                this.handler.completed(value, this.attachment);
            } catch (Throwable ignored) {
                // Same.
            }
        }
    }
}
