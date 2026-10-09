package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

// A pool of worker threads draining a shared queue of tasks. This is the heart of the
// execution framework: threads are expensive to create, so they are created once and
// *reused*, while the queue absorbs bursts of work. Submitting becomes cheap and bounded;
// the pool size caps how much runs at once.
//
// The admission rule is the class, and it is not "queue everything". A submission takes the
// first of these that applies:
//
//   1. fewer than `corePoolSize` workers exist  -> create a worker for it;
//   2. the queue accepts it                     -> queue it;
//   3. fewer than `maximumPoolSize` workers     -> create a worker for it;
//   4. otherwise                                -> hand it to the rejection handler.
//
// Order matters: the pool grows past its core size only when the *queue* is full, which is
// why an unbounded queue makes `maximumPoolSize` unreachable and a direct hand-off queue
// (SynchronousQueue) makes it the only thing that matters.
//
// Each worker loops: take the next task, run it, repeat. It exits when the pool is shut down
// and the queue is drained, or when it has idled longer than the keep-alive and the pool has
// more workers than its core; the last one out marks the pool terminated (which is what
// {@link #awaitTermination} waits for).
//
// All pool state — the queue, the worker list, the counters, the shutdown flags — is guarded
// by one monitor, which is also the only thing workers and submitters signal through.
// Single-exit style throughout (finding #105).
public class ThreadPoolExecutor extends AbstractExecutorService {

    private final Object sync = new Object();
    private final BlockingQueue<Runnable> workQueue;
    private int corePoolSize;
    private int maximumPoolSize;
    // Idle time before a surplus worker retires. Kept in nanoseconds because that is what the
    // getter has to convert from, and converting away from the finest unit never loses.
    private long keepAliveNanos;
    private boolean coreThreadsTimeOut;
    private ThreadFactory threadFactory;
    private RejectedExecutionHandler rejectionHandler;

    // The worker threads themselves, not a count: shutdownNow has to interrupt them, and a
    // count cannot be interrupted. The size of this list *is* the pool size.
    private final ArrayList<Thread> workers = new ArrayList<Thread>();
    // Workers currently inside a task's run().
    private int activeWorkers;
    private int largestPoolSize;
    private long completedTasks;
    private boolean shutdown;
    // Set only by shutdownNow: tells a worker to stop even with work still queued.
    private boolean stopped;
    // Named `poolTerminated` and not `terminated` because {@link #terminated()} is a method of
    // this class; one name for two things reads as a typo every time it is met.
    private boolean poolTerminated;

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    // The one constructor that actually builds the pool; the other three fill in defaults.
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize
                || keepAliveTime < 0L) {
            throw new IllegalArgumentException("bad pool sizes");
        }
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveNanos = unit.toNanos(keepAliveTime);
        this.workQueue = workQueue;
        this.threadFactory = threadFactory;
        this.rejectionHandler = handler;
    }

    // Package-private convenience used by {@link Executors} and by the scheduled pool: same
    // pool, without naming a TimeUnit and with no keep-alive, so no worker ever retires.
    ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("bad pool sizes");
        }
        if (workQueue == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveNanos = 0L;
        this.workQueue = workQueue;
        this.threadFactory = Executors.defaultThreadFactory();
        this.rejectionHandler = new ThreadPoolExecutor.AbortPolicy();
    }

    // The same, with the thread factory and the rejection policy named. For the scheduled pool,
    // which cannot reach the public constructors without naming a TimeUnit constant.
    ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue,
                       ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, workQueue);
        if (threadFactory != null) {
            this.threadFactory = threadFactory;
        }
        if (handler != null) {
            this.rejectionHandler = handler;
        }
    }

    public void execute(Runnable command) {
        executeNow(command);
    }

    // The body of execute(), under a name a subclass can still reach. A subclass that
    // overrides execute() to mean something else — ScheduledThreadPoolExecutor overrides it
    // to mean "schedule with zero delay" — needs a way back to the plain pool submission,
    // and `super.execute(...)` does not compile here (finding #125: no invokespecial for
    // `super.method()`). Package-private, so it changes nothing observable.
    final void executeNow(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        Thread toStart = null;
        boolean rejected = false;
        synchronized (sync) {
            if (shutdown) {
                rejected = true;
            } else {
                boolean spawn = workers.size() < corePoolSize;
                if (!spawn) {
                    if (workQueue.offer(command)) {
                        sync.notifyAll();
                    } else if (workers.size() < maximumPoolSize) {
                        // The queue refused, so growing is the only way to take this task. This
                        // is the branch that makes maximumPoolSize mean anything: with an
                        // unbounded queue the offer always succeeds and it is never reached.
                        spawn = true;
                    } else {
                        rejected = true;
                    }
                }
                if (spawn) {
                    toStart = addWorker(command);
                    if (toStart == null) {
                        // The factory declined to make a thread. The task is still in nobody's
                        // hands, so it has to go somewhere: the queue, or the handler.
                        if (workQueue.offer(command)) {
                            sync.notifyAll();
                        } else {
                            rejected = true;
                        }
                    }
                }
            }
        }
        if (toStart != null) {
            // Started outside the monitor: Thread.start() is a scheduler operation, and the
            // new worker's first act is to take this very monitor.
            toStart.start();
        }
        if (rejected) {
            // Also outside the monitor, and that is not an accident: CallerRunsPolicy runs the
            // task right here, and running arbitrary work while holding the pool's lock would
            // block every other submitter for as long as the task lasts.
            rejectionHandler.rejectedExecution(command, this);
        }
    }

    // Builds a worker and registers it, returning the thread for the caller to start. Returns
    // null if the factory declined. Caller holds sync.
    private Thread addWorker(Runnable firstTask) {
        PoolWorker worker = new PoolWorker(this, firstTask);
        Thread t = threadFactory.newThread(worker);
        if (t != null) {
            worker.setThread(t);
            workers.add(t);
            if (workers.size() > largestPoolSize) {
                largestPoolSize = workers.size();
            }
        }
        return t;
    }

    // --- lifecycle ---

    public void shutdown() {
        shutdownInternal();
    }

    // Same seam as executeNow, for the same reason.
    final void shutdownInternal() {
        boolean nowTerminated = false;
        synchronized (sync) {
            shutdown = true;
            // With no worker left to drain it, an already-idle pool is done right away.
            if (workers.isEmpty()) {
                poolTerminated = true;
                nowTerminated = true;
            }
            sync.notifyAll();
        }
        if (nowTerminated) {
            terminated();
        }
    }

    /**
     * Stops accepting, abandons the queued tasks and returns them, and interrupts the workers.
     *
     * <p>The returned list is the queue's contents at this instant, drained: those tasks will not
     * run, and handing them back is the only way the caller can do anything about that.
     */
    public List<Runnable> shutdownNow() {
        ArrayList<Runnable> pending = new ArrayList<Runnable>();
        ArrayList<Thread> toInterrupt;
        boolean nowTerminated = false;
        synchronized (sync) {
            shutdown = true;
            stopped = true;
            Runnable task = workQueue.poll();
            while (task != null) {
                pending.add(task);
                task = workQueue.poll();
            }
            toInterrupt = new ArrayList<Thread>(workers);
            if (workers.isEmpty()) {
                poolTerminated = true;
                nowTerminated = true;
            }
            sync.notifyAll();
        }
        // Interrupting outside the monitor: a worker woken by the interrupt goes straight for
        // this lock, and holding it here would make every one of them queue up behind us.
        for (int i = 0; i < toInterrupt.size(); i++) {
            Thread t = toInterrupt.get(i);
            t.interrupt();
        }
        if (nowTerminated) {
            terminated();
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

    // Shut down but not finished: the window in which the pool is draining. Distinct from both
    // isShutdown (which stays true afterwards) and isTerminated (which is only the end).
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

    /**
     * Called once, when the last worker has left after a shutdown.
     *
     * <p>Empty here; it exists so a subclass can release what the pool was holding -- a
     * connection, a file, a registration -- at the one moment when no task can still be using it.
     * Invoked outside the monitor, so a hook that blocks cannot wedge the pool.
     */
    protected void terminated() {
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        boolean done;
        synchronized (sync) {
            long ms = unit.toMillis(timeout);
            if (!poolTerminated && ms > 0L) {
                // Wait in slices rather than one shot: every worker's exit does a
                // notifyAll, so a single wait would be woken by the *first* worker out and
                // report a premature false. Re-checking per slice waits for the last one.
                long slice = ms / 8L + 1L;
                for (int i = 0; i < 8; i++) {
                    if (!poolTerminated) {
                        sync.wait(slice);
                    }
                }
            }
            done = poolTerminated;
        }
        return done;
    }

    // --- sizing ---

    public int getPoolSize() {
        int n;
        synchronized (sync) {
            n = workers.size();
        }
        return n;
    }

    public int getActiveCount() {
        int n;
        synchronized (sync) {
            n = activeWorkers;
        }
        return n;
    }

    // The high-water mark, which is what tells you whether the maximum was ever reached --
    // getPoolSize only ever reports now, and a pool that spiked and shrank looks idle.
    public int getLargestPoolSize() {
        int n;
        synchronized (sync) {
            n = largestPoolSize;
        }
        return n;
    }

    public long getCompletedTaskCount() {
        long n;
        synchronized (sync) {
            n = completedTasks;
        }
        return n;
    }

    // Everything ever submitted and not rejected: finished, running, and still queued.
    // Approximate by nature -- the three parts are read together but move independently.
    public long getTaskCount() {
        long n;
        synchronized (sync) {
            n = completedTasks + (long) activeWorkers + (long) workQueue.size();
        }
        return n;
    }

    public int getCorePoolSize() {
        int n;
        synchronized (sync) {
            n = corePoolSize;
        }
        return n;
    }

    /**
     * Changes the core size, taking effect on the next submission.
     *
     * <p>Shrinking does not kill workers on the spot: a worker only notices its pool has more
     * threads than its core when it next goes idle, and stopping one mid-task to honour a resize
     * would lose the task.
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize < 0");
        }
        synchronized (sync) {
            if (corePoolSize > maximumPoolSize) {
                throw new IllegalArgumentException("corePoolSize > maximumPoolSize");
            }
            this.corePoolSize = corePoolSize;
            // Idle workers must re-evaluate: a shrink may have made some of them retirable.
            sync.notifyAll();
        }
    }

    public int getMaximumPoolSize() {
        int n;
        synchronized (sync) {
            n = maximumPoolSize;
        }
        return n;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0) {
            throw new IllegalArgumentException("maximumPoolSize <= 0");
        }
        synchronized (sync) {
            if (maximumPoolSize < corePoolSize) {
                throw new IllegalArgumentException("maximumPoolSize < corePoolSize");
            }
            this.maximumPoolSize = maximumPoolSize;
            sync.notifyAll();
        }
    }

    public long getKeepAliveTime(TimeUnit unit) {
        long n;
        synchronized (sync) {
            n = unit.convert(keepAliveNanos, TimeUnit.NANOSECONDS);
        }
        return n;
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0L) {
            throw new IllegalArgumentException("keepAliveTime < 0");
        }
        synchronized (sync) {
            if (time == 0L && coreThreadsTimeOut) {
                // Zero keep-alive plus core timeout would retire a worker the instant it went
                // idle, so the pool could never hold a thread. The JDK rejects the pair too.
                throw new IllegalArgumentException("keepAliveTime of 0 with coreThreadTimeOut");
            }
            this.keepAliveNanos = unit.toNanos(time);
            sync.notifyAll();
        }
    }

    public boolean allowsCoreThreadTimeOut() {
        boolean v;
        synchronized (sync) {
            v = coreThreadsTimeOut;
        }
        return v;
    }

    /**
     * Lets core workers retire on idle too, so a pool that is not being used costs no threads.
     *
     * @throws IllegalArgumentException if enabled with a keep-alive of zero -- a worker would
     *         retire the moment it went idle and the pool could never hold one
     */
    public void allowCoreThreadTimeOut(boolean value) {
        synchronized (sync) {
            if (value && keepAliveNanos <= 0L) {
                throw new IllegalArgumentException("core threads must have nonzero keep alive times");
            }
            this.coreThreadsTimeOut = value;
            sync.notifyAll();
        }
    }

    // Starts one core worker before any task arrives; reports whether one was started. What a
    // service that cannot afford the first request to pay for thread creation calls at start-up.
    public boolean prestartCoreThread() {
        Thread toStart = null;
        synchronized (sync) {
            if (workers.size() < corePoolSize) {
                toStart = addWorker(null);
            }
        }
        if (toStart != null) {
            toStart.start();
        }
        return toStart != null;
    }

    // The same for every core worker; returns how many were started.
    public int prestartAllCoreThreads() {
        int started = 0;
        boolean more = true;
        while (more) {
            more = prestartCoreThread();
            if (more) {
                started = started + 1;
            }
        }
        return started;
    }

    // --- the queue ---

    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    // Same seam again: a subclass that overrides getQueue() cannot call through to this one.
    final BlockingQueue<Runnable> queue() {
        return workQueue;
    }

    /**
     * Takes a task out of the queue if it is still there, reporting whether it was.
     *
     * <p>Only a task that has not started can be removed -- once a worker has taken it there is
     * nothing left in the queue to remove, and the boolean is how the caller finds that out.
     */
    public boolean remove(Runnable task) {
        boolean removed;
        synchronized (sync) {
            removed = workQueue.remove(task);
        }
        return removed;
    }

    /**
     * Drops the cancelled tasks still sitting in the queue.
     *
     * <p>Cancelling a Future does not unqueue it: cancel only marks the task, and the pool would
     * otherwise carry the corpse until a worker reached it and found there was nothing to do. In a
     * queue with many cancellations that is a slow memory leak, and this is the sweep for it.
     *
     * <p>The queue is rebuilt rather than filtered in place because a BlockingQueue offers no way
     * to remove from the middle without knowing the element.
     */
    public void purge() {
        synchronized (sync) {
            ArrayList<Runnable> keep = new ArrayList<Runnable>();
            Runnable task = workQueue.poll();
            while (task != null) {
                boolean dead = false;
                if (task instanceof Future) {
                    Future f = (Future) task;
                    dead = f.isCancelled();
                }
                if (!dead) {
                    keep.add(task);
                }
                task = workQueue.poll();
            }
            for (int i = 0; i < keep.size(); i++) {
                workQueue.offer(keep.get(i));
            }
            sync.notifyAll();
        }
    }

    // --- policy objects ---

    public ThreadFactory getThreadFactory() {
        ThreadFactory f;
        synchronized (sync) {
            f = threadFactory;
        }
        return f;
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            this.threadFactory = threadFactory;
        }
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        RejectedExecutionHandler h;
        synchronized (sync) {
            h = rejectionHandler;
        }
        return h;
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        synchronized (sync) {
            this.rejectionHandler = handler;
        }
    }

    // --- the hooks a subclass runs around each task ---

    /**
     * Called on the worker thread just before it runs {@code r}.
     *
     * <p>Empty here. It exists so a subclass can install per-task context -- a MDC entry, a
     * timer's start, a security or transaction scope -- on the thread that will actually run the
     * task, which the submitting thread cannot do.
     *
     * @param t the worker thread, which is {@code Thread.currentThread()}
     */
    protected void beforeExecute(Thread t, Runnable r) {
    }

    /**
     * Called on the worker thread just after {@code r} finishes.
     *
     * @param t what the task threw, or {@code null} if it returned normally. A task submitted
     *          through {@code submit} is wrapped in a FutureTask, which captures its own failure
     *          and never lets it out -- so for those this argument is always null, and the failure
     *          is read from the Future. That is the JDK's behaviour and it surprises people.
     */
    protected void afterExecute(Runnable r, Throwable t) {
    }

    // --- the seam workers run against (package-private) ---

    /**
     * Blocks until a task is available and hands it over, or returns null to tell the calling
     * worker to exit.
     *
     * <p>Three things make a worker exit: the pool was stopped by {@link #shutdownNow}; the pool
     * was shut down and the queue is empty; or the worker idled past the keep-alive while the pool
     * has more workers than its core (or core timeout is on). Everything else is a wait.
     */
    Runnable nextTask() {
        Runnable task = null;
        synchronized (sync) {
            boolean decided = false;
            while (!decided) {
                if (stopped) {
                    decided = true;
                } else if (!workQueue.isEmpty()) {
                    task = workQueue.poll();
                    decided = true;
                } else if (shutdown) {
                    decided = true;
                } else if (!retirable()) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        // An interrupt is how shutdownNow reaches a parked worker; the loop
                        // re-reads `stopped` and leaves. Re-marking the thread would be wrong:
                        // the flag belongs to the pool's shutdown, not to the next task's caller.
                    }
                } else {
                    // Timed wait, and the elapsed time is measured rather than assumed: a
                    // notifyAll from any submitter cuts the wait short, so returning on wake
                    // alone would retire a worker that had barely idled.
                    long before = System.nanoTime();
                    long ms = keepAliveNanos / 1000000L;
                    if (ms <= 0L) {
                        ms = 1L;
                    }
                    try {
                        sync.wait(ms);
                    } catch (InterruptedException e) {
                        // Same as above.
                    }
                    long idled = System.nanoTime() - before;
                    if (workQueue.isEmpty() && idled >= keepAliveNanos && retirable()) {
                        decided = true;
                    }
                }
            }
        }
        return task;
    }

    // Whether this pool currently has a worker it can afford to lose. Caller holds sync.
    private boolean retirable() {
        return coreThreadsTimeOut || workers.size() > corePoolSize;
    }

    /**
     * Runs one task with the two hooks around it, and books the result.
     *
     * <p>A task that throws must not kill the worker -- the pool would silently shrink -- so the
     * run is guarded and the throwable is handed to {@link #afterExecute} instead of propagating.
     * That is where this diverges from the JDK, which lets the throwable out and replaces the
     * worker; the observable difference is the pool's thread identities, not its behaviour.
     */
    void runTask(Runnable task, Thread me) {
        synchronized (sync) {
            activeWorkers++;
        }
        Throwable thrown = null;
        try {
            beforeExecute(me, task);
        } catch (RuntimeException e) {
            thrown = e;
        }
        if (thrown == null) {
            try {
                task.run();
            } catch (RuntimeException e) {
                thrown = e;
            }
        }
        try {
            afterExecute(task, thrown);
        } catch (RuntimeException e) {
            // A hook that fails is the subclass's problem; the pool still has work to do.
        }
        taskCompleted();
    }

    // Record a finished task and leave the active set.
    void taskCompleted() {
        synchronized (sync) {
            activeWorkers--;
            completedTasks = completedTasks + 1L;
            sync.notifyAll();
        }
    }

    // A worker is leaving; the last one out marks the pool terminated.
    void workerExited(Thread me) {
        boolean nowTerminated = false;
        synchronized (sync) {
            workers.remove(me);
            if (workers.isEmpty() && shutdown) {
                poolTerminated = true;
                nowTerminated = true;
            }
            sync.notifyAll();
        }
        if (nowTerminated) {
            terminated();
        }
    }

    // --- the four rejection policies ---
    //
    // Nested inside the pool, as in the JDK, because that is where they are named: a caller
    // writes `new ThreadPoolExecutor.CallerRunsPolicy()` and the reading of it is complete.
    // They hold no state, so one instance can serve any number of pools.

    /**
     * Rejection by exception: the submitter finds out, immediately and loudly.
     *
     * <p>The default, and the right default. The alternatives all lose the task, and a pool that
     * silently drops work is a bug that shows up somewhere else entirely, hours later.
     */
    public static class AbortPolicy implements RejectedExecutionHandler {

        public AbortPolicy() {
        }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString()
                    + " rejected from " + e.toString());
        }
    }

    /**
     * Rejection by running the task on the submitting thread.
     *
     * <p>The only policy that provides real backpressure: the submitter is busy running the task
     * it could not hand over, so for that whole time it submits nothing more, and the pool gets a
     * chance to catch up. Nothing is lost and nothing throws -- at the price of the caller's
     * thread, which is not free if that thread is one the application needs elsewhere.
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {

        public CallerRunsPolicy() {
        }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            // Not after shutdown: running it here would let a task start on a pool that has
            // declared it accepts no more work.
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /** Rejection by silence: the task is dropped and nobody is told. */
    public static class DiscardPolicy implements RejectedExecutionHandler {

        public DiscardPolicy() {
        }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * Rejection by evicting the oldest queued task to make room for the new one.
     *
     * <p>A "freshest wins" policy: for a queue of readings, quotes or frames, the task at the head
     * is the most stale and the least worth running. For anything where every task must run, this
     * is the worst of the four -- it drops work *and* says nothing.
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {

        public DiscardOldestPolicy() {
        }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}

// One pooled thread: run the task it was created for, if any, then take from the queue until
// the pool says to stop.
//
// A Runnable rather than a Thread subclass, because the thread has to come from the pool's
// {@link ThreadFactory} — that is the whole point of having a factory, and a class that *is* a
// Thread cannot also be one the factory made.
final class PoolWorker implements Runnable {

    private final ThreadPoolExecutor pool;
    // The task this worker was created to run, so a growing pool starts working immediately
    // instead of queueing the task and racing to take it back.
    private Runnable firstTask;
    // The thread the factory built for this worker. Set right after construction and before
    // start(), so it is visible by the time run() reads it.
    private Thread thread;

    PoolWorker(ThreadPoolExecutor pool, Runnable firstTask) {
        this.pool = pool;
        this.firstTask = firstTask;
    }

    void setThread(Thread thread) {
        this.thread = thread;
    }

    public void run() {
        Runnable task = firstTask;
        firstTask = null;
        boolean running = true;
        while (running) {
            if (task == null) {
                task = pool.nextTask();
            }
            if (task == null) {
                running = false;
            } else {
                pool.runTask(task, thread);
                task = null;
            }
        }
        pool.workerExited(thread);
    }
}
