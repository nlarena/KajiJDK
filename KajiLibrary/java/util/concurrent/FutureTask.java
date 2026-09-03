package java.util.concurrent;

// A cancellable task that carries its own result — the bridge between {@link Runnable}
// (what a worker thread knows how to run) and {@link Future} (what the submitter holds).
// An executor runs it; the submitter waits on it; the value passes between the two threads
// through this object.
//
// The JDK drives its state with a lock-free CAS ladder; here one monitor guards the state
// word, the result and the waiters, which on a runtime that interleaves threads between
// opcodes is observably the same.
//
// Single-exit style throughout (finding #105).
public class FutureTask<V> implements RunnableFuture<V> {

    // The task states. Deliberately **not** `final`: our javac neither folds a
    // compile-time constant into its use sites (JLS §13.1) nor emits a `<clinit>` for a
    // `static final` primitive — it stores the value only in the field's ConstantValue
    // attribute and reads the field with `getstatic`. If the VM does not apply
    // ConstantValue at class initialization, every constant reads back as 0, and
    // `state = COMPLETED` would leave the task looking unfinished forever (finding #112).
    // Dropping `final` forces a real `<clinit>`, which does run.
    private static int NEW = 0;
    // It finished normally and `result` holds the value.
    private static int COMPLETED = 1;
    // It threw, and `failure` holds what it threw.
    private static int FAILED = 2;
    // It was cancelled before running to completion.
    private static int CANCELLED = 3;

    private final Object sync = new Object();
    private final Callable<V> callable;
    private int state = NEW;
    private V result;
    private Throwable failure;

    public FutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
    }

    // Adapt a Runnable: it runs for its effect and the Future yields the given value.
    public FutureTask(Runnable runnable, V result) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        this.callable = new RunnableAdapter<V>(runnable, result);
    }

    // Run the task on the calling thread and publish its outcome to whoever is waiting.
    public void run() {
        boolean shouldRun;
        synchronized (sync) {
            shouldRun = state == NEW;
        }
        if (shouldRun) {
            V value = null;
            Throwable thrown = null;
            try {
                value = callable.call();
            } catch (Exception e) {
                thrown = e;
            }
            if (thrown == null) {
                set(value);
            } else {
                setException(thrown);
            }
        }
    }

    /**
     * Runs the task without recording its result, leaving it ready to run again; reports whether it
     * ran to completion and stayed unfinished.
     *
     * <p>This is what a periodic task is built on: {@code ScheduledThreadPoolExecutor} reruns the
     * same FutureTask on every tick, and a task that latched a result on the first tick could never
     * be rerun. A failure is *not* reset -- a periodic task that throws stops repeating, which is
     * the JDK's behaviour and the reason the return value exists.
     */
    protected boolean runAndReset() {
        boolean shouldRun;
        synchronized (sync) {
            shouldRun = state == NEW;
        }
        boolean ran = false;
        if (shouldRun) {
            Throwable thrown = null;
            try {
                callable.call();
                ran = true;
            } catch (Exception e) {
                thrown = e;
            }
            if (thrown != null) {
                setException(thrown);
            }
        }
        boolean stillNew;
        synchronized (sync) {
            stillNew = state == NEW;
        }
        return ran && stillNew;
    }

    /**
     * Records a normal result and wakes the waiters, unless the task already finished or was
     * cancelled.
     *
     * <p>{@code protected} rather than private because it is the seam a subclass uses to complete
     * the task from somewhere other than {@link #run} -- a task completed by a callback, an I/O
     * completion, another thread's answer.
     */
    protected void set(V v) {
        boolean transitioned;
        synchronized (sync) {
            // A cancel that landed while we ran wins: leave the outcome alone.
            if (state == NEW) {
                result = v;
                state = COMPLETED;
                transitioned = true;
            } else {
                transitioned = false;
            }
            sync.notifyAll();
        }
        if (transitioned) {
            done();
        }
    }

    // The failing counterpart of {@link #set}: the task's own throwable is what get() will wrap.
    protected void setException(Throwable t) {
        boolean transitioned;
        synchronized (sync) {
            if (state == NEW) {
                failure = t;
                state = FAILED;
                transitioned = true;
            } else {
                transitioned = false;
            }
            sync.notifyAll();
        }
        if (transitioned) {
            done();
        }
    }

    /**
     * Called exactly once, on the thread that settled the task, right after it became done -- by
     * completion, by failure or by cancellation.
     *
     * <p>Empty here on purpose: it exists so a subclass can react to completion without polling.
     * {@code ExecutorCompletionService} is the canonical user -- its tasks push themselves onto the
     * completion queue from this hook, which is what makes "take the next task that finished"
     * possible at all.
     *
     * <p>Invoked outside the monitor: a subclass's hook is arbitrary code, and running it under the
     * lock that {@code get} waits on would let a hook that blocks deadlock every waiter.
     */
    protected void done() {
    }

    // Cancel if the task has not finished. There is no interruption in KajiJDK, so a task
    // already running is not stopped — but its result is discarded and `get` reports the
    // cancellation, which is the contract callers actually observe.
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled;
        synchronized (sync) {
            if (state == NEW) {
                state = CANCELLED;
                sync.notifyAll();
                cancelled = true;
            } else {
                cancelled = false;
            }
        }
        if (cancelled) {
            done();
        }
        return cancelled;
    }

    public boolean isCancelled() {
        boolean c;
        synchronized (sync) {
            c = state == CANCELLED;
        }
        return c;
    }

    public boolean isDone() {
        boolean done;
        synchronized (sync) {
            done = state != NEW;
        }
        return done;
    }

    // No `throws` on these two overrides — see #104 on ExecutionException.
    public V get() throws InterruptedException {
        V value;
        synchronized (sync) {
            while (state == NEW) {
                sync.wait();
            }
            value = report();
        }
        return value;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException {
        V value;
        synchronized (sync) {
            if (state == NEW) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
            }
            if (state == NEW) {
                throw new TimeoutException();
            }
            value = report();
        }
        return value;
    }

    // Turn the finished state into a value or the matching exception. Caller holds sync
    // and has checked that the task is no longer NEW.
    private V report() {
        if (state == CANCELLED) {
            throw new CancellationException();
        }
        if (state == FAILED) {
            throw new ExecutionException(failure);
        }
        return result;
    }
}

// Wraps a Runnable as a Callable that returns a fixed value — what the (Runnable, V)
// constructor needs so both task shapes share one execution path.
final class RunnableAdapter<V> implements Callable<V> {

    private final Runnable task;
    private final V result;

    RunnableAdapter(Runnable task, V result) {
        this.task = task;
        this.result = result;
    }

    public V call() {
        task.run();
        return result;
    }
}
