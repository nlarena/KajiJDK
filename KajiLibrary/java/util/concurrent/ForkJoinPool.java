package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// The pool that runs {@link ForkJoinTask}s: a fixed set of worker threads draining a queue of
// tasks, where the tasks themselves are allowed to add more work while they run.
//
// WHAT THIS IS NOT. HotSpot's ForkJoinPool gives every worker its own double-ended queue: a
// worker pushes and pops its own forked tasks at one end (so the freshest, warmest subtask is
// the next one it runs) and *steals* from the other end of a random victim's deque when its own
// runs dry. That is where the throughput comes from, and none of it is here. This pool has one
// shared queue and every worker takes from it. The public surface cannot tell the difference —
// tasks run, joins return, results are the same — and what it costs is contention, not
// semantics.
//
// THE ONE PLACE THE DIFFERENCE WOULD BITE, AND WHAT IS DONE ABOUT IT. A worker that calls
// `join()` blocks. In HotSpot the joining worker instead *helps*: it runs other tasks while it
// waits, so a pool of N workers cannot be brought to a halt by N joins. Here a blocked worker is
// simply blocked, and N nested joins would deadlock a pool of N. The answer is compensation,
// which is what the JDK also falls back on when helping is impossible: a worker announces that
// it is about to block ({@link #beforeBlock}) and the pool replaces it, up to
// {@code maximumPoolSize}. So the count of *runnable* workers stays at the configured
// parallelism however deep the joins go, and progress is guaranteed as long as the depth stays
// under that bound -- which is the constructor's, not a hidden constant.
//
// Note what does NOT cause growth: queued work. A burst of a thousand submissions to a pool of
// two is two workers and a queue of nine hundred and ninety-eight, which is the whole point of
// having a pool. Only a thread that stops being available buys a replacement.
//
// Single-exit style throughout (finding #105).
public class ForkJoinPool extends AbstractExecutorService {

    /**
     * How a pool gets its worker threads.
     *
     * <p>Separate from {@link ThreadFactory} because a fork/join worker is not any thread: it must
     * be a {@link ForkJoinWorkerThread}, since that is what {@code ForkJoinTask.fork()} asks in
     * order to find the pool the current thread is working for.
     */
    public interface ForkJoinWorkerThreadFactory {
        ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }

    /**
     * A blocking operation the pool is told about, so it can keep its parallelism up while the
     * blocking thread is parked.
     *
     * <p>The two methods exist so the pool can spin the loop itself: it asks {@link #isReleasable}
     * first (cheap, no blocking) and only calls {@link #block} when it has arranged for a
     * replacement worker. Wrapping a blocking wait in one of these is the difference between a
     * pool that loses a thread to it and one that does not.
     */
    public interface ManagedBlocker {
        boolean block() throws InterruptedException;

        boolean isReleasable();
    }

    // The factory a pool uses when none is named. A shared instance: it holds no state.
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory =
            new DefaultForkJoinWorkerThreadFactory();

    // The common pool, built on first use. Lazily rather than in a static initializer so that a
    // program that never touches fork/join never starts a thread for it.
    private static ForkJoinPool common;
    // Guards `common`. A private object rather than the class literal, which is the shape the
    // rest of this library avoids relying on.
    private static final Object commonLock = new Object();

    private final Object sync = new Object();

    // Tasks handed in from outside the pool, and tasks forked by a task already running. Kept
    // apart because the two counts are separately observable (getQueuedSubmissionCount vs
    // getQueuedTaskCount) and because pollSubmission may only take from the first.
    // Raw lists on purpose: the element type would have to be `ForkJoinTask<?>`, and every
    // add of a `ForkJoinTask<T>` into one of those is a wildcard capture our javac does not
    // accept. Nothing is lost -- the queues are private and every element leaves through a cast
    // back to the type the caller already holds.
    private final ArrayList submissions = new ArrayList();
    private final ArrayList forked = new ArrayList();

    private final ArrayList<ForkJoinWorkerThread> workers = new ArrayList<ForkJoinWorkerThread>();
    private final ForkJoinWorkerThreadFactory factory;
    private final Thread.UncaughtExceptionHandler ueh;
    private final boolean asyncMode;
    private final Predicate<? super ForkJoinPool> saturate;
    private final long keepAliveMillis;

    private int parallelism;
    private final int maximumPoolSize;
    private final int minimumRunnable;
    private int workerIndex;
    // Workers parked waiting for a task. The compensation rule reads exactly this.
    private int idleWorkers;
    // Workers inside a task's exec().
    private int activeWorkers;
    // Workers parked on a join or a ManagedBlocker. Counted so getRunningThreadCount can leave
    // them out -- they hold a thread but they are not running anything.
    private int blockedWorkers;
    private long stealCount;
    private boolean shutdown;
    private boolean stopped;
    private boolean poolTerminated;

    // Scheduled tasks not yet handed to a worker, and whether shutdown should abandon them.
    private int delayedTasks;
    private boolean cancelDelayedOnShutdown;

    /** A pool with one worker per available processor. */
    public ForkJoinPool() {
        this(defaultParallelism(), defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    /**
     * The configured form.
     *
     * @param asyncMode FIFO instead of LIFO for forked tasks. LIFO is right for divide and
     *        conquer -- the most recently forked subtask is the one whose data is still warm and
     *        whose joiner is waiting. FIFO is right for event-style tasks that are never joined,
     *        where the oldest task is the one that has waited longest.
     */
    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory,
                        Thread.UncaughtExceptionHandler handler, boolean asyncMode) {
        this(parallelism, factory, handler, asyncMode, 0, 32767, 1, null, 60000L,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Every knob at once.
     *
     * <p>{@code corePoolSize} is accepted and ignored, as it is in the JDK: it was a hint about
     * how many workers to keep alive, and this pool creates workers on demand and keeps them.
     * The parameter stays in the signature because removing it would change the constructor.
     *
     * @param saturate consulted when the pool would have to exceed {@code maximumPoolSize} to make
     *        progress; returning true means "carry on anyway", and returning false (or leaving it
     *        null) means the attempt fails with a RejectedExecutionException
     */
    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory,
                        Thread.UncaughtExceptionHandler handler, boolean asyncMode,
                        int corePoolSize, int maximumPoolSize, int minimumRunnable,
                        Predicate<? super ForkJoinPool> saturate, long keepAliveTime,
                        TimeUnit unit) {
        if (parallelism <= 0 || maximumPoolSize < parallelism || keepAliveTime <= 0L) {
            throw new IllegalArgumentException("bad pool configuration");
        }
        if (factory == null) {
            throw new NullPointerException();
        }
        this.parallelism = parallelism;
        this.factory = factory;
        this.ueh = handler;
        this.asyncMode = asyncMode;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumRunnable = minimumRunnable;
        this.saturate = saturate;
        this.keepAliveMillis = unit.toMillis(keepAliveTime);
    }

    // One less than the processor count, floored at one: the submitting thread usually helps, so
    // the JDK sizes the common pool this way and there is no reason to differ.
    private static int defaultParallelism() {
        Runtime runtime = Runtime.getRuntime();
        int n = runtime.availableProcessors() - 1;
        if (n < 1) {
            n = 1;
        }
        return n;
    }

    /**
     * The pool that {@code fork()} uses when it is called from outside any pool.
     *
     * <p>Shared on purpose: fork/join work is CPU-bound, so one pool sized to the machine is the
     * right number of threads for the whole process, and every library that quietly creates its
     * own would oversubscribe it.
     */
    public static ForkJoinPool commonPool() {
        ForkJoinPool pool;
        synchronized (commonLock) {
            if (common == null) {
                common = new ForkJoinPool(defaultParallelism(),
                        defaultForkJoinWorkerThreadFactory, null, false);
            }
            pool = common;
        }
        return pool;
    }

    public static int getCommonPoolParallelism() {
        ForkJoinPool pool = commonPool();
        return pool.getParallelism();
    }

    // ---------------------------------------------------------------- submitting

    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        // The one-argument adapt, not `adapt(command, null)`: a null in a type-variable position
        // gives our javac nothing to infer T from, and it reports incompatible constraints.
        ForkJoinTask<?> wrapped = ForkJoinTask.adapt(command);
        submitExternal(wrapped);
    }

    // Runs a task that is already a ForkJoinTask, without wrapping it.
    public void execute(ForkJoinTask<?> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        submitExternal(task);
    }

    /**
     * Submits and waits for the result -- the blocking form.
     *
     * <p>The calling thread does not help: it parks on the task. Calling this from *inside* the
     * pool would therefore consume a worker for the whole computation, which is what {@code
     * ForkJoinTask.invoke()} exists to avoid.
     */
    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        submitExternal(task);
        return task.join();
    }

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        submitExternal(task);
        return task;
    }

    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<T> t = ForkJoinTask.adapt(task);
        submitExternal(t);
        return t;
    }

    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<T> t = this.<T>adapt(task, result);
        submitExternal(t);
        return t;
    }

    public ForkJoinTask<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<Object> t = this.<Object>adapt(task, null);
        submitExternal(t);
        return t;
    }

    /**
     * Submits from outside the pool even when called from inside it.
     *
     * <p>A plain {@code submit} made by a worker would be treated as forked work. This one is
     * always an external submission, which matters for a task that must not be picked up as part
     * of the current computation -- a follow-up job, not a subtask.
     */
    public <T> ForkJoinTask<T> externalSubmit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        submitExternal(task);
        return task;
    }

    /**
     * Queues the task without waking a worker for it.
     *
     * <p>For submitting a batch: waking a worker per task makes every one of them race for the
     * queue, whereas queueing them lazily and letting the last submission wake the pool costs one
     * round of signalling instead of N. Here it queues and does not signal, so the task starts
     * when a worker next looks -- which is what "lazy" promises and no more.
     */
    public <T> ForkJoinTask<T> lazySubmit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            if (shutdown) {
                throw new RejectedExecutionException("pool has been shut down");
            }
            submissions.add(task);
        }
        return task;
    }

    /**
     * Submits a task that is cancelled if it has not finished within the timeout.
     *
     * @param timeoutAction run with the task when the deadline passes, before the cancellation --
     *        the hook for logging or for recording a fallback; may be null
     */
    public <T> ForkJoinTask<T> submitWithTimeout(Callable<T> callable, long timeout, TimeUnit unit,
                                                 Consumer<? super ForkJoinTask<T>> timeoutAction) {
        if (callable == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<T> task = ForkJoinTask.adapt(callable);
        submitExternal(task);
        FjTimeout<T> watchdog = new FjTimeout<T>(task, unit.toMillis(timeout), timeoutAction);
        watchdog.setDaemon(true);
        watchdog.start();
        return task;
    }

    // The one entry point every submission goes through: queue it, then make sure somebody will
    // come for it.
    private void submitExternal(ForkJoinTask<?> task) {
        Thread starting = null;
        synchronized (sync) {
            if (shutdown) {
                throw new RejectedExecutionException("pool has been shut down");
            }
            submissions.add(task);
            starting = ensureTaker();
            sync.notifyAll();
        }
        if (starting != null) {
            starting.start();
        }
    }

    // The seam {@link ForkJoinTask#fork} uses: work produced by a task already running.
    void pushTask(ForkJoinTask task) {
        Thread starting = null;
        synchronized (sync) {
            if (shutdown && stopped) {
                throw new RejectedExecutionException("pool has been shut down");
            }
            forked.add(task);
            starting = ensureTaker();
            sync.notifyAll();
        }
        if (starting != null) {
            starting.start();
        }
    }

    /**
     * Starts a worker if the pool is below its target size.
     *
     * <p>Only up to {@code parallelism} (or {@code minimumRunnable}, whichever is larger). Queued
     * work does NOT by itself justify another thread -- that is what a pool is for, and growing on
     * every submission that finds the workers busy would turn a burst of a thousand tasks into a
     * thousand threads. Growth beyond the target happens in exactly one place, {@link #beforeBlock},
     * and for exactly one reason: a worker is about to stop being a worker.
     *
     * <p>Caller holds sync; returns the thread to start, outside the monitor.
     */
    private Thread ensureTaker() {
        Thread starting = null;
        int wanted = parallelism;
        if (wanted < minimumRunnable) {
            wanted = minimumRunnable;
        }
        if (workers.size() < wanted) {
            starting = addWorker();
        }
        return starting;
    }

    /**
     * Told by a worker that it is about to block -- on a join, or inside a {@link ManagedBlocker}.
     *
     * <p>This is the compensation rule, and it is what stands in for HotSpot's helping joiners.
     * There a joining worker runs other tasks while it waits, so a pool of N workers cannot be
     * halted by N joins. Here a blocked worker is simply blocked, so the pool replaces it: one
     * extra thread per blocked one, which restores the parallelism the pool was configured for.
     * Without this, a divide-and-conquer computation deeper than the pool is wide would deadlock --
     * every worker parked on a subtask that no worker is left to run.
     *
     * <p>The replacement is only started when nobody is idle; an idle worker is already the taker
     * this would have created. The extra threads are temporary: {@link #nextTask} retires any
     * worker that finds itself surplus with the queues empty.
     */
    void beforeBlock() {
        Thread starting = null;
        synchronized (sync) {
            blockedWorkers = blockedWorkers + 1;
            boolean nobodyFree = idleWorkers == 0;
            boolean queued = !forked.isEmpty() || !submissions.isEmpty();
            if (nobodyFree && queued) {
                if (workers.size() < maximumPoolSize) {
                    starting = addWorker();
                } else if (saturate != null && !saturate.test(this)) {
                    // At the ceiling with nothing free and a policy that says not to exceed it.
                    // Saying so is better than blocking on work nobody can reach.
                    throw new RejectedExecutionException("pool is saturated");
                }
            }
        }
        if (starting != null) {
            starting.start();
        }
    }

    // The worker is running again. Paired with beforeBlock by the caller's finally.
    void afterBlock() {
        synchronized (sync) {
            if (blockedWorkers > 0) {
                blockedWorkers = blockedWorkers - 1;
            }
        }
    }

    // Caller holds sync.
    private Thread addWorker() {
        ForkJoinWorkerThread w = factory.newThread(this);
        Thread started = null;
        if (w != null) {
            if (ueh != null) {
                w.setUncaughtExceptionHandler(ueh);
            }
            workers.add(w);
            started = w;
        }
        return started;
    }

    // Wraps a Runnable so it can live in a queue of ForkJoinTasks.
    private <T> ForkJoinTask<T> adapt(Runnable task, T result) {
        return ForkJoinTask.<T>adapt(task, result);
    }

    // ---------------------------------------------------------------- the worker loop

    int nextWorkerIndex() {
        int i;
        synchronized (sync) {
            i = workerIndex;
            workerIndex = workerIndex + 1;
        }
        return i;
    }

    /**
     * The body of a worker thread: take a task, run it, repeat until the pool says to stop.
     *
     * <p>Forked tasks are preferred over submissions, and in async mode taken from the front
     * rather than the back. Preferring forked work is what keeps a computation moving to its end
     * instead of starting a second one alongside it -- a pool that took submissions first would
     * hold every half-finished computation open at once.
     */
    void runWorker(ForkJoinWorkerThread w) {
        boolean running = true;
        while (running) {
            ForkJoinTask<?> task = nextTask();
            if (task == null) {
                running = false;
            } else {
                runTask(task);
            }
        }
        workerExited(w);
    }

    // Blocks until there is a task, or returns null to tell the worker to exit.
    private ForkJoinTask<?> nextTask() {
        ForkJoinTask<?> task = null;
        synchronized (sync) {
            boolean decided = false;
            while (!decided) {
                if (stopped) {
                    decided = true;
                } else if (!forked.isEmpty()) {
                    task = takeFrom(forked);
                    decided = true;
                } else if (!submissions.isEmpty()) {
                    task = (ForkJoinTask) submissions.remove(0);
                    decided = true;
                } else if (shutdown) {
                    decided = true;
                } else {
                    idleWorkers = idleWorkers + 1;
                    try {
                        // A bounded wait even with nothing to do: a worker beyond the pool's
                        // parallelism was created to compensate for a blocked one, and once the
                        // queue is empty it has no reason to stay.
                        sync.wait(keepAliveMillis);
                    } catch (InterruptedException e) {
                        // How shutdownNow reaches a parked worker; the loop re-reads `stopped`.
                    }
                    idleWorkers = idleWorkers - 1;
                    if (workers.size() > parallelism && forked.isEmpty() && submissions.isEmpty()) {
                        decided = true;
                    }
                }
            }
        }
        return task;
    }

    // LIFO by default, FIFO in async mode — see the constructor's note on why.
    private ForkJoinTask<?> takeFrom(ArrayList queue) {
        Object task;
        if (asyncMode) {
            task = queue.remove(0);
        } else {
            task = queue.remove(queue.size() - 1);
        }
        return (ForkJoinTask) task;
    }

    private void runTask(ForkJoinTask<?> task) {
        synchronized (sync) {
            activeWorkers = activeWorkers + 1;
        }
        task.doExec();
        synchronized (sync) {
            activeWorkers = activeWorkers - 1;
            stealCount = stealCount + 1L;
            sync.notifyAll();
        }
    }

    private void workerExited(ForkJoinWorkerThread w) {
        synchronized (sync) {
            workers.remove(w);
            if (workers.isEmpty() && shutdown) {
                poolTerminated = true;
            }
            sync.notifyAll();
        }
    }

    // ---------------------------------------------------------------- queries

    public int getParallelism() {
        int n;
        synchronized (sync) {
            n = parallelism;
        }
        return n;
    }

    // Changes the target parallelism and returns the previous value. Takes effect as workers are
    // next needed or next go idle; a worker in the middle of a task is never stopped for it.
    public int setParallelism(int size) {
        if (size <= 0 || size > maximumPoolSize) {
            throw new IllegalArgumentException("parallelism out of range");
        }
        int previous;
        synchronized (sync) {
            previous = parallelism;
            parallelism = size;
            sync.notifyAll();
        }
        return previous;
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return ueh;
    }

    public boolean getAsyncMode() {
        return asyncMode;
    }

    public int getPoolSize() {
        int n;
        synchronized (sync) {
            n = workers.size();
        }
        return n;
    }

    // Workers inside a task. "Active" and not "running": a worker parked in a join is neither.
    public int getActiveThreadCount() {
        int n;
        synchronized (sync) {
            n = activeWorkers;
        }
        return n;
    }

    // Workers not parked waiting for work -- an estimate, since a worker may be blocked in a join
    // rather than running. The gap between this and getActiveThreadCount is exactly the joins.
    public int getRunningThreadCount() {
        int n;
        synchronized (sync) {
            n = workers.size() - idleWorkers - blockedWorkers;
            if (n < 0) {
                n = 0;
            }
        }
        return n;
    }

    /**
     * An estimate of the tasks that were run by a thread other than the one that produced them.
     *
     * <p>With no per-worker deques there is nothing to steal *from*: every task a worker runs came
     * off the shared queue, so this counts the tasks the workers have executed. In HotSpot the two
     * numbers differ by however much of its own work each worker got to keep; here they coincide
     * by construction, and the JDK documents the value as an estimate for reasons of its own.
     */
    public long getStealCount() {
        long n;
        synchronized (sync) {
            n = stealCount;
        }
        return n;
    }

    public long getQueuedTaskCount() {
        long n;
        synchronized (sync) {
            n = (long) forked.size();
        }
        return n;
    }

    public int getQueuedSubmissionCount() {
        int n;
        synchronized (sync) {
            n = submissions.size();
        }
        return n;
    }

    public boolean hasQueuedSubmissions() {
        boolean any;
        synchronized (sync) {
            any = !submissions.isEmpty();
        }
        return any;
    }

    // Scheduled tasks that have not yet come due.
    public long getDelayedTaskCount() {
        long n;
        synchronized (sync) {
            n = (long) delayedTasks;
        }
        return n;
    }

    /**
     * Takes a submitted task back out of the queue, or null if there is none waiting.
     *
     * <p>{@code protected} because it is for a subclass that wants to reroute work -- extending
     * the pool to hand overflow to somewhere else, for instance. Only submissions, never forked
     * tasks: a forked task belongs to a computation that is waiting for it.
     */
    protected ForkJoinTask<?> pollSubmission() {
        ForkJoinTask<?> task;
        synchronized (sync) {
            if (submissions.isEmpty()) {
                task = null;
            } else {
                task = (ForkJoinTask) submissions.remove(0);
            }
        }
        return task;
    }

    // Empties both queues into `c` and reports how many moved. The draining counterpart of
    // pollSubmission, for a subclass shutting the pool down its own way.
    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        int moved = 0;
        synchronized (sync) {
            Collection sink = c;
            while (!forked.isEmpty()) {
                sink.add(forked.remove(forked.size() - 1));
                moved = moved + 1;
            }
            while (!submissions.isEmpty()) {
                sink.add(submissions.remove(0));
                moved = moved + 1;
            }
        }
        return moved;
    }

    // Nothing running and nothing queued. The predicate a caller polls when it wants to know that
    // a computation submitted earlier has entirely finished, including everything it forked.
    public boolean isQuiescent() {
        boolean quiet;
        synchronized (sync) {
            quiet = activeWorkers == 0 && forked.isEmpty() && submissions.isEmpty();
        }
        return quiet;
    }

    /**
     * Waits for the pool to go quiescent, up to the timeout; reports whether it did.
     *
     * <p>The JDK's version makes the calling thread help run tasks while it waits. This one only
     * waits: the caller is a thread from outside the pool, and giving it work would let a task
     * run on a thread the pool does not own -- which the two per-task hooks on
     * ForkJoinWorkerThread are entitled to assume never happens.
     */
    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        boolean quiet;
        synchronized (sync) {
            long remaining = deadline - System.currentTimeMillis();
            while (!isQuiescentLocked() && remaining > 0L) {
                try {
                    sync.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    remaining = 0L;
                }
                remaining = deadline - System.currentTimeMillis();
            }
            quiet = isQuiescentLocked();
        }
        return quiet;
    }

    // Caller holds sync.
    private boolean isQuiescentLocked() {
        return activeWorkers == 0 && forked.isEmpty() && submissions.isEmpty();
    }

    /**
     * Runs a blocking operation with the pool's parallelism kept up.
     *
     * <p>The contract is the loop: ask {@link ManagedBlocker#isReleasable}, and only if the answer
     * is no arrange a replacement and call {@link ManagedBlocker#block}. Calling it from outside
     * any pool is legal and simply runs the loop, which is why this is the safe way to write a
     * blocking wait that might or might not be on a fork/join thread.
     */
    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        if (blocker == null) {
            throw new NullPointerException();
        }
        ForkJoinPool pool = ForkJoinTask.getPool();
        if (pool != null) {
            pool.beforeBlock();
        }
        try {
            boolean done = blocker.isReleasable();
            while (!done) {
                done = blocker.block();
                if (!done) {
                    done = blocker.isReleasable();
                }
            }
        } finally {
            // In a finally because `block()` is the caller's code and may throw: leaving the
            // pool believing a thread is still blocked would keep a compensation thread that
            // nothing will ever retire.
            if (pool != null) {
                pool.afterBlock();
            }
        }
    }

    // ---------------------------------------------------------------- scheduling

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Callable<Object> callable = Executors.callable(command);
        return scheduleInternal(callable, unit.toMillis(delay), 0L);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return this.<V>scheduleInternal(callable, unit.toMillis(delay), 0L);
    }

    // Fixed rate: the period is measured from one start to the next.
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                  TimeUnit unit) {
        if (period <= 0L) {
            throw new IllegalArgumentException("period <= 0");
        }
        Callable<Object> callable = Executors.callable(command);
        return scheduleInternal(callable, unit.toMillis(initialDelay), unit.toMillis(period));
    }

    // Fixed delay: the gap is measured from one *finish* to the next start, encoded as a negative
    // period so one field carries both modes.
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit) {
        if (delay <= 0L) {
            throw new IllegalArgumentException("delay <= 0");
        }
        Callable<Object> callable = Executors.callable(command);
        return scheduleInternal(callable, unit.toMillis(initialDelay), -unit.toMillis(delay));
    }

    private <V> ScheduledFuture<V> scheduleInternal(Callable<V> callable, long delayMillis,
                                                    long periodMillis) {
        if (callable == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            if (shutdown) {
                throw new RejectedExecutionException("pool has been shut down");
            }
            delayedTasks = delayedTasks + 1;
        }
        FjScheduled<V> task = new FjScheduled<V>(this, callable, delayMillis, periodMillis);
        FjTimer timer = new FjTimer(this, task);
        timer.setDaemon(true);
        timer.start();
        return task;
    }

    /**
     * Makes shutdown abandon the scheduled tasks that have not come due.
     *
     * <p>Off by default, matching the JDK: a delayed task that has been accepted normally still
     * runs once its time arrives, and only a caller that knows its schedule is disposable asks for
     * the other behaviour.
     */
    public void cancelDelayedTasksOnShutdown() {
        synchronized (sync) {
            cancelDelayedOnShutdown = true;
        }
    }

    // Whether a scheduled task that has just come due may still be submitted. Package-private:
    // the timer threads ask.
    boolean acceptsDelayed() {
        boolean ok;
        synchronized (sync) {
            ok = !stopped && !(shutdown && cancelDelayedOnShutdown);
        }
        return ok;
    }

    void delayedFinished() {
        synchronized (sync) {
            if (delayedTasks > 0) {
                delayedTasks = delayedTasks - 1;
            }
            sync.notifyAll();
        }
    }

    // ---------------------------------------------------------------- lifecycle

    public void shutdown() {
        synchronized (sync) {
            shutdown = true;
            // An idle pool with no worker to notice the flag is finished right away; otherwise
            // the last worker out sets it.
            if (workers.isEmpty()) {
                poolTerminated = true;
            }
            sync.notifyAll();
        }
    }

    public List<Runnable> shutdownNow() {
        ArrayList<Runnable> pending = new ArrayList<Runnable>();
        ArrayList<ForkJoinWorkerThread> toInterrupt;
        synchronized (sync) {
            shutdown = true;
            stopped = true;
            cancelDelayedOnShutdown = true;
            while (!submissions.isEmpty()) {
                pending.add((Runnable) submissions.remove(0));
            }
            while (!forked.isEmpty()) {
                pending.add((Runnable) forked.remove(0));
            }
            toInterrupt = new ArrayList<ForkJoinWorkerThread>(workers);
            if (workers.isEmpty()) {
                poolTerminated = true;
            }
            sync.notifyAll();
        }
        for (int i = 0; i < toInterrupt.size(); i++) {
            ForkJoinWorkerThread w = toInterrupt.get(i);
            w.interrupt();
        }
        return pending;
    }

    public boolean isShutdown() {
        boolean s;
        synchronized (sync) {
            s = shutdown;
        }
        return s;
    }

    public boolean isTerminating() {
        boolean t;
        synchronized (sync) {
            t = shutdown && !poolTerminated;
        }
        return t;
    }

    public boolean isTerminated() {
        boolean t;
        synchronized (sync) {
            t = poolTerminated;
        }
        return t;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        boolean done;
        synchronized (sync) {
            long remaining = deadline - System.currentTimeMillis();
            while (!poolTerminated && remaining > 0L) {
                sync.wait(remaining);
                remaining = deadline - System.currentTimeMillis();
            }
            done = poolTerminated;
        }
        return done;
    }

    /**
     * {@link #invokeAll} without giving up on an interrupt.
     *
     * <p>Only for a caller that must not leave half its tasks running -- interruption here would
     * abandon them mid-computation. The interrupt is not swallowed: the thread is marked again on
     * the way out, so the caller's own loop still sees it.
     */
    public <T> List<Future<T>> invokeAllUninterruptibly(Collection<? extends Callable<T>> tasks) {
        boolean interrupted = false;
        List<Future<T>> result = null;
        while (result == null) {
            try {
                result = this.<T>invokeAll(tasks);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    public String toString() {
        String state;
        synchronized (sync) {
            state = "[parallelism = " + parallelism + ", size = " + workers.size()
                    + ", active = " + activeWorkers + ", queued = "
                    + (forked.size() + submissions.size()) + "]";
        }
        return "ForkJoinPool" + state;
    }
}

// The factory a pool uses when none is named: a plain ForkJoinWorkerThread bound to the pool.
final class DefaultForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    DefaultForkJoinWorkerThreadFactory() {
    }

    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread t = new ForkJoinWorkerThread(pool);
        // Daemon, as in the JDK: a pool that is never shut down must not keep the JVM alive.
        t.setDaemon(true);
        return t;
    }
}

// Cancels a submitted task once its deadline passes, unless it finished first.
//
// A thread per timed submission rather than a shared timer: `submitWithTimeout` is not a hot path,
// and one thread that sleeps once is far less machinery than a delay queue that would have to be
// kept ordered.
final class FjTimeout<T> extends Thread {

    private final ForkJoinTask<T> task;
    private final long millis;
    private final java.util.function.Consumer<? super ForkJoinTask<T>> action;

    FjTimeout(ForkJoinTask<T> task, long millis,
              java.util.function.Consumer<? super ForkJoinTask<T>> action) {
        this.task = task;
        this.millis = millis;
        this.action = action;
    }

    public void run() {
        if (millis > 0L) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!task.isDone()) {
            if (action != null) {
                action.accept(task);
            }
            task.cancel(true);
        }
    }
}

// A scheduled task on a ForkJoinPool: the Future the caller holds, and the Runnable the pool
// eventually runs.
//
// `periodMillis` encodes the repeat mode in its sign — zero for a one-shot, positive for fixed
// rate, negative for fixed delay — the same convention SchedTask uses, for the same reason: one
// field cannot fall out of step with itself.
final class FjScheduled<V> implements ScheduledFuture<V>, Runnable {

    private final Object lock = new Object();
    private final ForkJoinPool owner;
    private final Callable<V> callable;
    private final long periodMillis;
    private long dueMillis;
    private boolean done;
    private boolean cancelled;
    private V result;
    private Throwable failure;
    private int cycles;

    FjScheduled(ForkJoinPool owner, Callable<V> callable, long delayMillis, long periodMillis) {
        this.owner = owner;
        this.callable = callable;
        this.periodMillis = periodMillis;
        this.dueMillis = System.currentTimeMillis() + delayMillis;
    }

    public void run() {
        if (!isCancelled()) {
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
                    // A failure ends even a periodic task: repeating one that has already thrown
                    // would repeat the failure for ever.
                    failure = thrown;
                    done = true;
                } else if (periodMillis == 0L) {
                    result = value;
                    done = true;
                }
                lock.notifyAll();
            }
        }
    }

    boolean isPeriodic() {
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

    // Blocks until `count` runs have finished — how a fixed-delay timer knows the previous run is
    // over before it starts counting the gap.
    void awaitCycle(int count) {
        synchronized (lock) {
            while (cycles < count && !done && !cancelled) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    cancelled = true;
                }
            }
        }
    }

    public long getDelay(TimeUnit unit) {
        long remaining = dueMillis() - System.currentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    public int compareTo(Delayed other) {
        int sign;
        if (other == this) {
            sign = 0;
        } else {
            long mine = dueMillis() - System.currentTimeMillis();
            long theirs = other.getDelay(TimeUnit.MILLISECONDS);
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

    public V get() throws InterruptedException, ExecutionException {
        synchronized (lock) {
            while (!done) {
                lock.wait();
            }
        }
        return report();
    }

    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        synchronized (lock) {
            long remaining = millis;
            while (!done && remaining > 0L) {
                lock.wait(remaining);
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

// One thread per scheduled task: sleep until it is due, then hand it to the pool. Keeping the
// clock out of the workers is what lets the pool's own loop stay exactly as it is.
final class FjTimer extends Thread {

    private final ForkJoinPool owner;
    private final FjScheduled<?> task;

    FjTimer(ForkJoinPool owner, FjScheduled<?> task) {
        this.owner = owner;
        this.task = task;
    }

    public void run() {
        long next = task.dueMillis();
        int dispatched = 0;
        boolean running = true;
        while (running) {
            long wait = next - System.currentTimeMillis();
            if (wait > 0L) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
            if (running) {
                if (task.isCancelled() || !owner.acceptsDelayed()) {
                    running = false;
                } else {
                    owner.execute(task);
                    dispatched = dispatched + 1;
                    if (!task.isPeriodic()) {
                        running = false;
                    } else {
                        long period = task.periodMillis();
                        if (period > 0L) {
                            next = next + period;
                        } else {
                            task.awaitCycle(dispatched);
                            next = System.currentTimeMillis() - period;
                        }
                        task.setDue(next);
                    }
                }
            }
        }
        owner.delayedFinished();
    }
}
