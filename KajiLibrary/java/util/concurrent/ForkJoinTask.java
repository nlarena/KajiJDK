package java.util.concurrent;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

// A unit of work that can split itself. The pattern it exists for is divide and conquer:
// a task that finds its problem too big *forks* subtasks, then *joins* them and combines
// their results. Recursion supplies the parallelism, so the caller never sizes a thread pool
// against the shape of the data.
//
// It is a {@link Future}, but a much lighter one than {@link FutureTask}: no interruption
// machinery, no per-task lock in the JDK's version, and a `join` that is expected to be
// called by a thread that is itself running tasks. That last point is the discipline the
// whole framework rests on — a forked task must be joined by the thread that forked it, and
// a task must not block on anything except its own subtasks.
//
// Failure is carried, not thrown at the point of failure: `exec` throwing records the
// throwable and completes the task abnormally, and it is `join` that raises it. That is why
// there is a "quiet" twin of every waiting method — `quietlyJoin` waits without raising, for
// callers that will inspect {@link #getException} themselves.
//
// The three abstract methods are the contract with subclasses: `exec` does the work and says
// whether the task is already complete, and the two raw-result accessors let this class move
// values around without knowing what a subclass stores. {@link RecursiveTask} and
// {@link RecursiveAction} implement all three so that a user only writes `compute`.
//
// What is deliberately simpler than HotSpot: there are no per-worker deques and no
// work-stealing. `fork` hands the task to its pool, which runs it on a thread of its own,
// and `join` waits on a monitor. Nothing of that is visible through the public surface,
// which is what has to be faithful; what it costs is throughput, not semantics.
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {

    private final Object lock = new Object();
    private boolean done;
    private boolean cancelled;
    private V result;
    private Throwable exception;
    private short tag;

    public ForkJoinTask() {
    }

    // ---------------------------------------------------------------- the subclass contract

    // The work. Returns true if the task is complete when it returns — RecursiveTask says
    // yes, a CountedCompleter says no, because it completes when its children do.
    protected abstract boolean exec();

    public abstract V getRawResult();

    protected abstract void setRawResult(V value);

    // Run `exec` and record what happened. The one place a task's outcome is decided.
    final void doExec() {
        boolean complete;
        Throwable thrown = null;
        try {
            complete = exec();
        } catch (Throwable t) {
            complete = true;
            thrown = t;
        }
        if (thrown != null) {
            completeExceptionally(thrown);
        } else if (complete) {
            quietlyComplete();
        }
    }

    // ---------------------------------------------------------------- forking and joining

    // Hand this task to a pool and return immediately. The pool is the one this thread is
    // already working for, if any, and the common pool otherwise — which is what makes
    // `fork` work identically inside and outside the framework.
    public final ForkJoinTask<V> fork() {
        ForkJoinPool pool = poolForFork();
        pool.pushTask(this);
        return this;
    }

    private static ForkJoinPool poolForFork() {
        ForkJoinPool pool = getPool();
        if (pool == null) {
            pool = ForkJoinPool.commonPool();
        }
        return pool;
    }

    // Wait for the result, raising whatever went wrong. Unlike get(), the failure is
    // unchecked — a compute() body is not allowed to declare checked exceptions, so a
    // checked join would be unusable from inside the framework.
    public final V join() {
        awaitDone();
        return reportJoin();
    }

    // Run here and now, then join. Cheaper than fork+join for the branch the current thread
    // is going to wait for anyway, and the reason divide-and-conquer code forks n-1 of its
    // n subtasks and invokes the last.
    public final V invoke() {
        if (!isDone()) {
            doExec();
        }
        awaitDone();
        return reportJoin();
    }

    // Fork one, run the other here, then wait: the two-task case is common enough that the
    // framework spells it out.
    public static void invokeAll(ForkJoinTask<?> t1, ForkJoinTask<?> t2) {
        t2.fork();
        t1.invoke();
        t2.join();
    }

    public static void invokeAll(ForkJoinTask<?>... tasks) {
        int i = 1;
        while (i < tasks.length) {
            tasks[i].fork();
            i = i + 1;
        }
        if (tasks.length > 0) {
            tasks[0].invoke();
        }
        i = 1;
        while (i < tasks.length) {
            tasks[i].join();
            i = i + 1;
        }
    }

    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        Iterator<T> forking = tasks.iterator();
        while (forking.hasNext()) {
            ForkJoinTask<?> task = forking.next();
            task.fork();
        }
        Iterator<T> joining = tasks.iterator();
        while (joining.hasNext()) {
            ForkJoinTask<?> task = joining.next();
            task.join();
        }
        return tasks;
    }

    // ---------------------------------------------------------------- waiting, quietly or not

    private void awaitDone() {
        synchronized (lock) {
            while (!done) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new CancellationException();
                }
            }
        }
    }

    private boolean awaitDone(long millis) {
        long deadline = System.currentTimeMillis() + millis;
        boolean finished;
        synchronized (lock) {
            long remaining = millis;
            while (!done && remaining > 0L) {
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    remaining = 0L;
                }
                remaining = deadline - System.currentTimeMillis();
            }
            finished = done;
        }
        return finished;
    }

    // The raising half of join(): cancellation and failure both come out unchecked.
    private V reportJoin() {
        V value;
        synchronized (lock) {
            if (cancelled) {
                throw new CancellationException();
            }
            if (exception != null) {
                throw new CompletionException(exception);
            }
            value = result;
        }
        return value;
    }

    // Waits without raising. The caller is expected to ask getException() afterwards.
    public final void quietlyJoin() {
        awaitDone();
    }

    public final void quietlyInvoke() {
        if (!isDone()) {
            doExec();
        }
        awaitDone();
    }

    public final boolean quietlyJoin(long timeout, TimeUnit unit) {
        return awaitDone(unit.toMillis(timeout));
    }

    public final boolean quietlyJoinUninterruptibly(long timeout, TimeUnit unit) {
        return awaitDone(unit.toMillis(timeout));
    }

    // No `throws` on either get: restating a compiled superinterface's clause is rejected
    // (finding #104), and the descriptor is the same without it.
    public final V get() {
        awaitDone();
        return reportGet();
    }

    public final V get(long timeout, TimeUnit unit) {
        if (!awaitDone(unit.toMillis(timeout))) {
            throw new TimeoutException();
        }
        return reportGet();
    }

    // Same wait as join(), but the Future spelling of failure: wrapped in ExecutionException.
    private V reportGet() {
        V value;
        synchronized (lock) {
            if (cancelled) {
                throw new CancellationException();
            }
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            value = result;
        }
        return value;
    }

    // ---------------------------------------------------------------- completion state

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

    public final boolean isDone() {
        boolean d;
        synchronized (lock) {
            d = done;
        }
        return d;
    }

    public final boolean isCancelled() {
        boolean c;
        synchronized (lock) {
            c = cancelled;
        }
        return c;
    }

    // Abnormal covers both cancellation and failure — the two ways a result never appeared.
    public final boolean isCompletedAbnormally() {
        boolean abnormal;
        synchronized (lock) {
            abnormal = cancelled || exception != null;
        }
        return abnormal;
    }

    public final boolean isCompletedNormally() {
        boolean normal;
        synchronized (lock) {
            normal = done && !cancelled && exception == null;
        }
        return normal;
    }

    // The throwable that ended this task, cancellation included, or null.
    public final Throwable getException() {
        Throwable t;
        synchronized (lock) {
            if (cancelled && exception == null) {
                t = new CancellationException();
            } else {
                t = exception;
            }
        }
        return t;
    }

    public V resultNow() {
        V value;
        synchronized (lock) {
            if (!done || cancelled || exception != null) {
                throw new IllegalStateException("task has no result");
            }
            value = result;
        }
        return value;
    }

    public Throwable exceptionNow() {
        Throwable t;
        synchronized (lock) {
            if (exception == null) {
                throw new IllegalStateException("task did not complete exceptionally");
            }
            t = exception;
        }
        return t;
    }

    public void completeExceptionally(Throwable ex) {
        synchronized (lock) {
            if (!done) {
                exception = ex;
                done = true;
                lock.notifyAll();
            }
        }
    }

    // Completes with a value the caller supplies rather than one exec() produced — how a
    // task short-circuits, and how CountedCompleter reports a root result.
    public void complete(V value) {
        boolean settled = false;
        synchronized (lock) {
            if (!done) {
                result = value;
                done = true;
                settled = true;
                lock.notifyAll();
            }
        }
        if (settled) {
            try {
                setRawResult(value);
            } catch (Throwable ignored) {
                // A setRawResult that throws must not turn a normal completion abnormal:
                // the value is already published and joiners have already been woken.
            }
        }
    }

    // Marks done and publishes whatever the subclass stored, without touching it.
    public final void quietlyComplete() {
        synchronized (lock) {
            if (!done) {
                result = getRawResult();
                done = true;
                lock.notifyAll();
            }
        }
    }

    // Back to unstarted, so the same task object can be forked again. Only sound once the
    // task has completed and nobody is joining it.
    public void reinitialize() {
        synchronized (lock) {
            done = false;
            cancelled = false;
            result = null;
            exception = null;
        }
    }

    // ---------------------------------------------------------------- ambient pool queries

    // The pool the *calling* thread is working for, or null outside the framework. Static
    // because it asks about the caller, not about any particular task.
    public static ForkJoinPool getPool() {
        ForkJoinPool pool = null;
        Thread current = Thread.currentThread();
        if (current instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread worker = (ForkJoinWorkerThread) current;
            pool = worker.getPool();
        }
        return pool;
    }

    public static boolean inForkJoinPool() {
        return getPool() != null;
    }

    // Un-fork a task that has not been picked up yet. Without per-worker deques there is
    // never anything to take back, so this always declines — a legal answer: the JDK's
    // returns false whenever the task is no longer the caller's most recent fork.
    public boolean tryUnfork() {
        return false;
    }

    // Estimates of the calling worker's backlog. With no local deques the honest estimate is
    // zero, which is also what these return when called from outside the framework.
    public static int getQueuedTaskCount() {
        return 0;
    }

    public static int getSurplusQueuedTaskCount() {
        return 0;
    }

    // Run tasks until the pool has nothing left to do. Nothing to help with here, since
    // forked tasks get threads of their own rather than sitting in a local queue.
    public static void helpQuiesce() {
    }

    // The four local-queue peeks, for the same reason: there is no local queue to look into.
    protected static ForkJoinTask<?> peekNextLocalTask() {
        return null;
    }

    protected static ForkJoinTask<?> pollNextLocalTask() {
        return null;
    }

    protected static ForkJoinTask<?> pollTask() {
        return null;
    }

    protected static ForkJoinTask<?> pollSubmission() {
        return null;
    }

    // ---------------------------------------------------------------- the tag

    // Sixteen bits of scratch space on every task, for algorithms that must mark tasks they
    // have already visited — graph traversals, mostly. The framework itself never reads it.
    public final short getForkJoinTaskTag() {
        short t;
        synchronized (lock) {
            t = tag;
        }
        return t;
    }

    public final short setForkJoinTaskTag(short newValue) {
        short previous;
        synchronized (lock) {
            previous = tag;
            tag = newValue;
        }
        return previous;
    }

    public final boolean compareAndSetForkJoinTaskTag(short expect, short update) {
        boolean swapped = false;
        synchronized (lock) {
            if (tag == expect) {
                tag = update;
                swapped = true;
            }
        }
        return swapped;
    }

    // ---------------------------------------------------------------- adapters

    // Turn ordinary work into a task. These are what let a ForkJoinPool accept a Runnable or
    // a Callable at all: everything the pool runs is a ForkJoinTask underneath.
    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnable<Object>(runnable, null);
    }

    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable<T>(runnable, result);
    }

    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable<T>(callable);
    }

    // The interruptible variants differ only in that the JDK's cancel() interrupts the
    // running thread. Cancellation here is cooperative — a task is marked and joiners are
    // released — so the two forms coincide.
    public static <T> ForkJoinTask<T> adaptInterruptible(Callable<? extends T> callable) {
        return new AdaptedCallable<T>(callable);
    }

    public static <T> ForkJoinTask<T> adaptInterruptible(Runnable runnable, T result) {
        return new AdaptedRunnable<T>(runnable, result);
    }

    public static ForkJoinTask<?> adaptInterruptible(Runnable runnable) {
        return new AdaptedRunnable<Object>(runnable, null);
    }
}

// A Runnable wearing a task's clothes: the result is fixed at construction, since a Runnable
// produces none.
final class AdaptedRunnable<T> extends ForkJoinTask<T> implements Runnable {

    private final Runnable body;
    private T value;

    AdaptedRunnable(Runnable body, T value) {
        if (body == null) {
            throw new NullPointerException();
        }
        this.body = body;
        this.value = value;
    }

    public T getRawResult() {
        return value;
    }

    protected void setRawResult(T v) {
        value = v;
    }

    protected boolean exec() {
        body.run();
        return true;
    }

    public void run() {
        invoke();
    }
}

// A Callable wearing a task's clothes. `call` may throw anything; doExec catches it and
// completes the task abnormally, which is why exec() need not declare a thing.
final class AdaptedCallable<T> extends ForkJoinTask<T> implements Runnable {

    private final Callable<? extends T> body;
    private T value;

    AdaptedCallable(Callable<? extends T> body) {
        if (body == null) {
            throw new NullPointerException();
        }
        this.body = body;
    }

    public T getRawResult() {
        return value;
    }

    protected void setRawResult(T v) {
        value = v;
    }

    protected boolean exec() {
        value = body.call();
        return true;
    }

    public void run() {
        invoke();
    }
}
