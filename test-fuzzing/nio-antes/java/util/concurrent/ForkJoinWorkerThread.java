package java.util.concurrent;

// A thread that belongs to a {@link ForkJoinPool}. Its only real job is to *be identifiable*:
// `ForkJoinTask.fork()` asks the current thread which pool it is working for, and a task
// forked from inside the framework goes to that pool instead of the common one. Without a
// distinct thread type there would be nowhere to hang that answer.
//
// The two protected hooks are for subclasses that need per-worker setup — a thread-local
// buffer, a security or logging context — established before any task runs and torn down
// after the last one. `run` guarantees the pairing: `onTermination` is called exactly once
// however the worker ends, with the throwable that ended it or null.
//
public class ForkJoinWorkerThread extends Thread {

    private final ForkJoinPool pool;
    private final int poolIndex;

    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        if (pool == null) {
            throw new NullPointerException();
        }
        this.pool = pool;
        this.poolIndex = pool.nextWorkerIndex();
    }

    /**
     * The form that names a thread group, for a pool whose workers must be reachable through one.
     *
     * @param preserveThreadLocals whether this worker keeps its thread-locals between tasks.
     *        Accepted and ignored: the JDK clears them by resetting fields of {@code Thread} that
     *        only its own package can reach, and there is no such door here. Ignoring it is a
     *        divergence in what the flag *does*, and it is recorded rather than dressed up --
     *        passing {@code false} does not clear anything, so a task must not rely on starting
     *        with empty thread-locals.
     */
    protected ForkJoinWorkerThread(ThreadGroup group, ForkJoinPool pool,
                                   boolean preserveThreadLocals) {
        // The group can only be set through a Thread constructor, so this one must call super
        // before anything else — which is why the name cannot carry the pool index yet.
        super(group, null, "ForkJoinPool-worker", 0L);
        if (pool == null) {
            throw new NullPointerException();
        }
        this.pool = pool;
        this.poolIndex = pool.nextWorkerIndex();
    }

    // The pool this worker belongs to. This is the query the whole class exists to answer.
    public ForkJoinPool getPool() {
        return pool;
    }

    // A stable index within the pool, handed out at construction. Useful for indexing
    // per-worker side tables without a map lookup.
    public int getPoolIndex() {
        return poolIndex;
    }

    // How much work this worker has queued locally. Always zero here: forked tasks are
    // handed to the pool rather than pushed onto a per-worker deque.
    public int getQueuedTaskCount() {
        return 0;
    }

    // Called on this thread before it runs any task.
    protected void onStart() {
    }

    // Called on this thread after its last task, with whatever ended it, or null.
    protected void onTermination(Throwable exception) {
    }

    // The lifecycle, in one place: start hook, then work, then termination hook — and the
    // termination hook runs whether the work ended normally or not.
    public void run() {
        Throwable ending = null;
        try {
            onStart();
            pool.runWorker(this);
        } catch (Throwable t) {
            ending = t;
        }
        onTermination(ending);
    }
}
