package java.util.concurrent;

// A pool of worker threads draining a shared queue of tasks. This is the heart of the
// execution framework: threads are expensive to create, so they are created once and
// *reused*, while the queue absorbs bursts of work. Submitting becomes cheap and bounded;
// the pool size caps how much runs at once.
//
// Workers are started lazily, one per task, up to the core size — after that a submission
// just queues. Each worker loops: take the next task, run it, repeat; it exits once the
// pool is shut down and the queue is drained, and the last one out marks the pool
// terminated (which is what {@link #awaitTermination} waits for).
//
// All pool state — the queue, the live-worker count, the shutdown and terminated flags —
// is guarded by one monitor, which is also the only thing workers and submitters signal
// through. Single-exit style throughout (finding #105).
public class ThreadPoolExecutor implements ExecutorService {

    private final Object sync = new Object();
    private final BlockingQueue<Runnable> workQueue;
    private final int corePoolSize;
    private final int maximumPoolSize;
    // Workers created and not yet exited.
    private int liveWorkers;
    // Workers currently inside a task's run().
    private int activeWorkers;
    private long completedTasks;
    private boolean shutdown;
    private boolean terminated;

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("bad pool sizes");
        }
        if (workQueue == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
    }

    // Package-private convenience used by {@link Executors}: same pool, without naming a
    // TimeUnit. Reading an enum constant of a *classpath* class (`TimeUnit.MILLISECONDS`)
    // compiles to `getfield` instead of `getstatic` and traps at run time — finding #110 —
    // and keepAliveTime is unused here anyway, since workers are never retired.
    ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("bad pool sizes");
        }
        if (workQueue == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
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
        boolean needWorker = false;
        synchronized (sync) {
            if (shutdown) {
                throw new RejectedExecutionException("executor has been shut down");
            }
            workQueue.offer(command);
            if (liveWorkers < corePoolSize) {
                liveWorkers++;
                needWorker = true;
            }
            sync.notifyAll();
        }
        if (needWorker) {
            // Started outside the monitor: Thread.start() is a scheduler operation, and
            // the new worker's first act is to take this very monitor.
            PoolWorker worker = new PoolWorker(this);
            worker.start();
        }
    }

    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> future = new FutureTask<T>(task);
        execute(future);
        return future;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        FutureTask<T> future = new FutureTask<T>(task, result);
        execute(future);
        return future;
    }

    public Future<?> submit(Runnable task) {
        FutureTask<Object> future = new FutureTask<Object>(task, null);
        execute(future);
        return future;
    }

    public void shutdown() {
        shutdownInternal();
    }

    // Same seam as executeNow, for the same reason.
    final void shutdownInternal() {
        synchronized (sync) {
            shutdown = true;
            // With no worker left to drain it, an already-idle pool is done right away.
            if (liveWorkers == 0) {
                terminated = true;
            }
            sync.notifyAll();
        }
    }

    public boolean isShutdown() {
        boolean s;
        synchronized (sync) {
            s = shutdown;
        }
        return s;
    }

    public boolean isTerminated() {
        boolean t;
        synchronized (sync) {
            t = terminated;
        }
        return t;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
        boolean done;
        synchronized (sync) {
            long ms = unit.toMillis(timeout);
            if (!terminated && ms > 0L) {
                // Wait in slices rather than one shot: every worker's exit does a
                // notifyAll, so a single wait would be woken by the *first* worker out and
                // report a premature false. Re-checking per slice waits for the last one.
                long slice = ms / 8L + 1L;
                for (int i = 0; i < 8; i++) {
                    if (!terminated) {
                        sync.wait(slice);
                    }
                }
            }
            done = terminated;
        }
        return done;
    }

    public int getPoolSize() {
        int n;
        synchronized (sync) {
            n = liveWorkers;
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

    public long getCompletedTaskCount() {
        long n;
        synchronized (sync) {
            // Explicit cast: implicit int→long widening drops the `i2l` (finding #103).
            n = completedTasks;
        }
        return n;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    // Same seam again: a subclass that overrides getQueue() cannot call through to this one.
    final BlockingQueue<Runnable> queue() {
        return workQueue;
    }

    // --- the seam workers run against (package-private) ---

    // Block until a task is available and hand it over, or return null once the pool is
    // shut down and drained — which tells the worker to exit.
    Runnable nextTask() {
        Runnable task;
        synchronized (sync) {
            while (workQueue.isEmpty() && !shutdown) {
                sync.wait();
            }
            if (workQueue.isEmpty()) {
                task = null;
            } else {
                task = workQueue.poll();
                activeWorkers++;
            }
        }
        return task;
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
    void workerExited() {
        synchronized (sync) {
            liveWorkers--;
            if (liveWorkers == 0 && shutdown) {
                terminated = true;
            }
            sync.notifyAll();
        }
    }
}

// One pooled thread: take a task, run it, repeat until the pool says to stop. A task that
// throws must not kill the worker — the pool would silently shrink — so the run is guarded.
final class PoolWorker extends Thread {

    private final ThreadPoolExecutor pool;

    PoolWorker(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    public void run() {
        boolean running = true;
        while (running) {
            Runnable task = pool.nextTask();
            if (task == null) {
                running = false;
            } else {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    // Swallowed on purpose: a failing task is the task's business, not the
                    // pool's. (A submitted FutureTask already captured its own failure.)
                }
                pool.taskCompleted();
            }
        }
        pool.workerExited();
    }
}
