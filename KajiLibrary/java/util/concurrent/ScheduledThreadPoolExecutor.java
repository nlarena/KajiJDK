package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

// A thread pool that also knows *when*. On top of {@link ThreadPoolExecutor} it adds the
// four scheduling entry points — run once after a delay, or repeat on a period or after a
// delay between runs — and hands back a {@link ScheduledFuture}, which is an ordinary
// cancellable future that can additionally say how long remains before it is due.
//
// The timing and the running are kept apart. Each submission gets a {@link SchedTimer}
// thread that does nothing but sleep until the task is due and then hand it to the inherited
// pool; the pool's own workers do the running, so the pool size still bounds concurrency and
// the timers never execute user code. The JDK instead keeps one delay-ordered queue that
// the workers themselves block on. The public surface cannot tell the difference, and this
// arrangement needs no priority queue of Delayed nodes underneath.
//
// Two ideas are worth separating. A *fixed rate* task is due at start + n·period regardless
// of how long a run takes, so its schedule never drifts but runs can bunch up; a *fixed
// delay* task waits for the previous run to finish and then counts the delay, so runs never
// overlap but the schedule drifts by the run time. That is the whole of the sign convention
// in {@link SchedTask}: a positive period means rate, a negative one means delay.
//
// Three policies decide what survives a shutdown. By default delayed one-shots that were
// already submitted still run and periodic tasks stop; `removeOnCancel` decides whether a
// cancelled task is dropped eagerly rather than left to be skipped when it comes due.
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {

    private final Object policyLock = new Object();
    private boolean continueExistingPeriodicTasksAfterShutdown;
    private boolean executeExistingDelayedTasksAfterShutdown = true;
    private boolean removeOnCancel;

    // The maximum is the core size: a scheduled pool never grows past it, because work
    // arrives on a clock rather than in bursts. The queue-only super constructor is the
    // package-private one, which avoids naming a TimeUnit constant — reading an enum
    // constant of a classpath class compiles to `getfield` and traps (finding #110).
    //
    // The factory and the rejection policy are handed to the SUPERCLASS and not kept here. They
    // used to be stored in fields of this class and read by nobody, which made the inherited
    // getThreadFactory() answer about a factory this pool was not using — a getter that reports
    // something other than what is in force is exactly the kind of member this project refuses to
    // write. Now there is one copy, in the class that builds the threads.
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, maxOf(corePoolSize), new LinkedBlockingQueue<Runnable>());
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, maxOf(corePoolSize), new LinkedBlockingQueue<Runnable>(),
                requireFactory(threadFactory), null);
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, maxOf(corePoolSize), new LinkedBlockingQueue<Runnable>(),
                null, requireHandler(handler));
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, maxOf(corePoolSize), new LinkedBlockingQueue<Runnable>(),
                requireFactory(threadFactory), requireHandler(handler));
    }

    // The null checks have to happen inside the super call's argument list, because a `super(...)`
    // must be the first statement and there is nowhere earlier to put an `if`.
    private static ThreadFactory requireFactory(ThreadFactory f) {
        if (f == null) {
            throw new NullPointerException();
        }
        return f;
    }

    private static RejectedExecutionHandler requireHandler(RejectedExecutionHandler h) {
        if (h == null) {
            throw new NullPointerException();
        }
        return h;
    }

    // A core size of zero is legal for a scheduled pool but the super constructor insists on
    // a positive maximum, so the floor is one.
    private static int maxOf(int corePoolSize) {
        int max = corePoolSize;
        if (max < 1) {
            max = 1;
        }
        return max;
    }

    // ------------------------------------------------------------ scheduling

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (command == null || unit == null) {
            throw new NullPointerException();
        }
        Callable<Object> callable = Executors.callable(command);
        SchedTask<Object> task = new SchedTask<Object>(this, callable, unit.toMillis(delay), 0L);
        RunnableScheduledFuture<Object> decorated = this.<Object>decorateTask(command, task);
        arm(decorated);
        return decorated;
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (callable == null || unit == null) {
            throw new NullPointerException();
        }
        SchedTask<V> task = new SchedTask<V>(this, callable, unit.toMillis(delay), 0L);
        RunnableScheduledFuture<V> decorated = this.<V>decorateTask(callable, task);
        arm(decorated);
        return decorated;
    }

    // Positive period: due at initialDelay + n·period, whatever the runs cost.
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                  long period, TimeUnit unit) {
        if (command == null || unit == null) {
            throw new NullPointerException();
        }
        if (period <= 0L) {
            throw new IllegalArgumentException("period must be positive");
        }
        Callable<Object> callable = Executors.callable(command);
        SchedTask<Object> task = new SchedTask<Object>(this, callable, unit.toMillis(initialDelay),
                                                       unit.toMillis(period));
        RunnableScheduledFuture<Object> decorated = this.<Object>decorateTask(command, task);
        arm(decorated);
        return decorated;
    }

    // Negative period: the gap is measured from the *end* of the previous run, so two runs
    // of the same task never overlap.
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit) {
        if (command == null || unit == null) {
            throw new NullPointerException();
        }
        if (delay <= 0L) {
            throw new IllegalArgumentException("delay must be positive");
        }
        Callable<Object> callable = Executors.callable(command);
        long negated = 0L - unit.toMillis(delay);
        SchedTask<Object> task = new SchedTask<Object>(this, callable,
                                                       unit.toMillis(initialDelay), negated);
        RunnableScheduledFuture<Object> decorated = this.<Object>decorateTask(command, task);
        arm(decorated);
        return decorated;
    }

    // The hook a subclass overrides to wrap every task the pool creates — for tracing, or to
    // attach a context. The default wraps nothing.
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable,
                                                          RunnableScheduledFuture<V> task) {
        return task;
    }

    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable,
                                                          RunnableScheduledFuture<V> task) {
        return task;
    }

    private void arm(RunnableScheduledFuture<?> task) {
        if (isShutdown()) {
            throw new RejectedExecutionException("executor has been shut down");
        }
        SchedTimer timer = new SchedTimer(this, task);
        timer.start();
    }

    // ------------------------------------------------------------ Executor view

    // Submitting without a delay is submitting with a delay of zero — the one definition
    // that keeps `execute` and `schedule` from being two different code paths.
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        Callable<Object> callable = Executors.callable(command);
        SchedTask<Object> task = new SchedTask<Object>(this, callable, 0L, 0L);
        arm(task);
    }

    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        Callable<Object> callable = Executors.callable(task);
        SchedTask<Object> scheduled = new SchedTask<Object>(this, callable, 0L, 0L);
        arm(scheduled);
        return scheduled;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        Callable<T> callable = Executors.<T>callable(task, result);
        SchedTask<T> scheduled = new SchedTask<T>(this, callable, 0L, 0L);
        arm(scheduled);
        return scheduled;
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        SchedTask<T> scheduled = new SchedTask<T>(this, task, 0L, 0L);
        arm(scheduled);
        return scheduled;
    }

    // The bridge a timer uses to hand a due task to the inherited pool. It goes through
    // ThreadPoolExecutor's package-private seam rather than `super.execute(...)`, which does
    // not compile here (finding #125: no invokespecial for `super.method()`).
    final void dispatch(Runnable task) {
        executeNow(task);
    }

    // Whether a task that has just come due may still run. Before shutdown, always; after
    // it, the two "existing tasks after shutdown" policies decide.
    final boolean acceptsRun(RunnableScheduledFuture<?> task) {
        boolean ok = true;
        if (isShutdown()) {
            if (task.isPeriodic()) {
                ok = getContinueExistingPeriodicTasksAfterShutdownPolicy();
            } else {
                ok = getExecuteExistingDelayedTasksAfterShutdownPolicy();
            }
        }
        return ok;
    }

    // ------------------------------------------------------------ policies

    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {
        synchronized (policyLock) {
            continueExistingPeriodicTasksAfterShutdown = value;
        }
    }

    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        boolean value;
        synchronized (policyLock) {
            value = continueExistingPeriodicTasksAfterShutdown;
        }
        return value;
    }

    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        synchronized (policyLock) {
            executeExistingDelayedTasksAfterShutdown = value;
        }
    }

    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        boolean value;
        synchronized (policyLock) {
            value = executeExistingDelayedTasksAfterShutdown;
        }
        return value;
    }

    public void setRemoveOnCancelPolicy(boolean value) {
        synchronized (policyLock) {
            removeOnCancel = value;
        }
    }

    public boolean getRemoveOnCancelPolicy() {
        boolean value;
        synchronized (policyLock) {
            value = removeOnCancel;
        }
        return value;
    }

    // ------------------------------------------------------------ lifecycle

    public void shutdown() {
        shutdownInternal();
    }

    // Stops accepting, and hands back what never got to run. Tasks still sleeping in their
    // timers are not in the queue yet; they are stopped by `acceptsRun` when they wake.
    public List<Runnable> shutdownNow() {
        shutdownInternal();
        List<Runnable> pending = new ArrayList<Runnable>();
        BlockingQueue<Runnable> queue = queue();
        Runnable task = queue.poll();
        while (task != null) {
            pending.add(task);
            task = queue.poll();
        }
        return pending;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue();
    }
}

// A scheduled submission: a task that carries its own due time and, if it repeats, its
// period. The three roles of RunnableScheduledFuture meet here — the pool runs it, the
// caller waits on it, the clock orders it.
//
// `periodMillis` encodes the repeat mode in its sign: zero for a one-shot, positive for
// fixed rate, negative for fixed delay. One field, no second flag to keep consistent.
final class SchedTask<V> implements RunnableScheduledFuture<V> {

    private final Object lock = new Object();
    private final ScheduledThreadPoolExecutor owner;
    private final Callable<V> callable;
    private final long periodMillis;
    private long dueMillis;
    private boolean done;
    private boolean cancelled;
    private V result;
    private Throwable failure;
    // Counts finished runs, so a fixed-delay timer can wait for the previous one.
    private int cycles;

    SchedTask(ScheduledThreadPoolExecutor owner, Callable<V> callable, long delayMillis,
              long periodMillis) {
        this.owner = owner;
        this.callable = callable;
        this.periodMillis = periodMillis;
        this.dueMillis = System.currentTimeMillis() + delayMillis;
    }

    // ------------------------------------------------------------ the run

    public void run() {
        if (isCancelled()) {
            return;
        }
        V value = null;
        Throwable thrown = null;
        try {
            value = callable.call();
        } catch (Throwable t) {
            thrown = t;
        }
        synchronized (lock) {
            cycles = cycles + 1;
            if (thrown != null) {
                // A failure ends even a periodic task, as in the JDK: repeating a task that
                // has already thrown would repeat the failure forever.
                failure = thrown;
                done = true;
            } else if (periodMillis == 0L) {
                result = value;
                done = true;
            }
            lock.notifyAll();
        }
    }

    // ------------------------------------------------------------ timing

    public boolean isPeriodic() {
        return periodMillis != 0L;
    }

    long periodMillis() {
        return periodMillis;
    }

    long dueMillis() {
        long due;
        synchronized (lock) {
            due = dueMillis;
        }
        return due;
    }

    void setDue(long millis) {
        synchronized (lock) {
            dueMillis = millis;
            lock.notifyAll();
        }
    }

    // Blocks until `count` runs have finished — how a fixed-delay timer knows the previous
    // run is over before it starts counting the gap.
    void awaitCycle(int count) {
        synchronized (lock) {
            while (cycles < count && !done && !cancelled) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    // Remaining time before this task is due, in whatever unit the caller asks for. The
    // MILLISECONDS constant is obtained through `valueOf` rather than read as a field:
    // reading an enum constant of a classpath class compiles to `getfield` (finding #110).
    public long getDelay(TimeUnit unit) {
        long remaining = dueMillis() - System.currentTimeMillis();
        TimeUnit millis = TimeUnit.valueOf("MILLISECONDS");
        return unit.convert(remaining, millis);
    }

    // Orders by remaining delay, which is what a delay-ordered queue needs. The comparison
    // is on millisecond due times, so it agrees with getDelay by construction.
    public int compareTo(Delayed other) {
        int sign;
        if (other == this) {
            sign = 0;
        } else {
            TimeUnit millis = TimeUnit.valueOf("MILLISECONDS");
            long mine = dueMillis() - System.currentTimeMillis();
            long theirs = other.getDelay(millis);
            if (mine < theirs) {
                sign = -1;
            } else if (mine > theirs) {
                sign = 1;
            } else {
                sign = 0;
            }
        }
        return sign;
    }

    // ------------------------------------------------------------ Future

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean won = false;
        synchronized (lock) {
            if (!done) {
                cancelled = true;
                done = true;
                won = true;
                lock.notifyAll();
            }
        }
        return won;
    }

    public boolean isCancelled() {
        boolean c;
        synchronized (lock) {
            c = cancelled;
        }
        return c;
    }

    public boolean isDone() {
        boolean d;
        synchronized (lock) {
            d = done;
        }
        return d;
    }

    // No `throws` on either get: restating a compiled superinterface's clause is rejected
    // (finding #104), and the descriptor is unchanged without it.
    public V get() {
        synchronized (lock) {
            while (!done) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new CancellationException();
                }
            }
        }
        return report();
    }

    public V get(long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        synchronized (lock) {
            long remaining = millis;
            while (!done && remaining > 0L) {
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    throw new CancellationException();
                }
                remaining = deadline - System.currentTimeMillis();
            }
            if (!done) {
                throw new TimeoutException();
            }
        }
        return report();
    }

    private V report() {
        V value;
        synchronized (lock) {
            if (cancelled) {
                throw new CancellationException();
            }
            if (failure != null) {
                throw new ExecutionException(failure);
            }
            value = result;
        }
        return value;
    }
}

// One thread per scheduled task, and all it does is sleep until the task is due and hand it
// to the pool. Keeping the clock out of the workers is what lets the inherited
// ThreadPoolExecutor stay exactly as it is.
final class SchedTimer extends Thread {

    private final ScheduledThreadPoolExecutor owner;
    private final RunnableScheduledFuture<?> task;

    SchedTimer(ScheduledThreadPoolExecutor owner, RunnableScheduledFuture<?> task) {
        this.owner = owner;
        this.task = task;
    }

    public void run() {
        SchedTask inner = null;
        if (task instanceof SchedTask) {
            inner = (SchedTask) task;
        }
        long next = dueOf(inner);
        int dispatched = 0;
        boolean running = true;
        while (running) {
            long wait = next - System.currentTimeMillis();
            if (wait > 0L) {
                // Ver la nota de `CfDelayedTask.run`: una interrupcion es cancelacion. Se restaura
                // la marca y se corta el bucle en vez de seguir esperando, que es lo que hace un
                // `ScheduledThreadPoolExecutor` real cuando le apagan el hilo.
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (task.isCancelled() || !owner.acceptsRun(task)) {
                running = false;
            } else {
                owner.dispatch(task);
                dispatched = dispatched + 1;
                if (!task.isPeriodic() || inner == null) {
                    running = false;
                } else {
                    long period = inner.periodMillis();
                    if (period > 0L) {
                        // Fixed rate: the next due time is a fixed step from the last one.
                        next = next + period;
                    } else {
                        // Fixed delay: wait for the run to end, then count from there.
                        inner.awaitCycle(dispatched);
                        next = System.currentTimeMillis() - period;
                    }
                    inner.setDue(next);
                }
            }
        }
    }

    private long dueOf(SchedTask inner) {
        long due;
        if (inner == null) {
            due = System.currentTimeMillis();
        } else {
            due = inner.dueMillis();
        }
        return due;
    }
}
