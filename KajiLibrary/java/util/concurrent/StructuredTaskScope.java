package java.util.concurrent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * A scope in which concurrent subtasks are forked, and which does not close until every one of them
 * is done — STRUCTURED concurrency.
 *
 * <p>The idea it enforces is that a concurrent task should have the same lifetime discipline as a
 * block: a thread started inside the block ends inside the block. An executor gives you the
 * opposite — you submit and walk away, and a task can outlive whatever asked for it, so nobody owns
 * it, nobody is told when it fails, and a failure that nobody joined is a failure nobody sees.
 *
 * <p>Here, {@link #fork} starts a subtask and {@link #join} does not return until they have all
 * finished or the scope has been cancelled, so the tree of tasks matches the nesting of the code.
 * What "finished" means is decided by the {@link Joiner} the scope was opened with: wait for
 * everyone, stop at the first failure, stop at the first SUCCESS, or stop when a predicate says so.
 * That is the whole design — the scope handles lifetime, the joiner handles policy.
 *
 * <pre>{@code
 * try (StructuredTaskScope<String, Void> scope =
 *         StructuredTaskScope.open(StructuredTaskScope.Joiner.<String>awaitAllSuccessfulOrThrow())) {
 *     StructuredTaskScope.Subtask<String> left = scope.fork(leftHalf);
 *     StructuredTaskScope.Subtask<String> right = scope.fork(rightHalf);
 *     scope.join();
 *     use(left.get(), right.get());
 * }
 * }</pre>
 *
 * @param <T> what the subtasks produce
 * @param <R> what {@link #join} produces, which is the joiner's business and not the subtasks'
 *
 * @implNote A KajiLibrary subset. The JDK's scope runs its subtasks on VIRTUAL threads and confines
 *           them with a {@code ThreadFlock}; this one uses platform threads and keeps the flock as a
 *           plain list, because virtual threads are not something this VM has. The observable
 *           contract is the same: fork starts, join waits, close cannot leave a subtask running.
 *
 *           <p>Everything lives in ONE compilation unit, and that is not a style choice: a static
 *           field read across compilation units is emitted as {@code getfield} and crashes the VM
 *           (finding #110), and this class cannot avoid reading {@code Subtask.State} constants.
 *           Same file, same problem gone.
 */
public interface StructuredTaskScope<T, R> extends AutoCloseable {

    /**
     * Opens a scope with the given joiner and configuration.
     *
     * @param joiner decides when the scope has seen enough, and what {@link #join} returns
     * @param configFunction receives the default configuration and returns the one to use
     */
    static <T, R> StructuredTaskScope<T, R> open(Joiner<? super T, ? extends R> joiner,
            Function<Configuration, Configuration> configFunction) {
        if (joiner == null || configFunction == null) {
            throw new NullPointerException();
        }
        ScopeConfig start = new ScopeConfig(null, null, null);
        Configuration chosen = configFunction.apply(start);
        if (chosen == null) {
            throw new NullPointerException();
        }
        return new ScopeImpl<T, R>(joiner, (ScopeConfig) chosen);
    }

    /** Opens a scope with the given joiner and the default configuration. */
    static <T, R> StructuredTaskScope<T, R> open(Joiner<? super T, ? extends R> joiner) {
        if (joiner == null) {
            throw new NullPointerException();
        }
        return new ScopeImpl<T, R>(joiner, new ScopeConfig(null, null, null));
    }

    /** Opens a scope that waits for every subtask and reports nothing back. */
    static <T> StructuredTaskScope<T, Void> open() {
        Joiner<T, Void> waiting = Joiner.awaitAll();
        return new ScopeImpl<T, Void>(waiting, new ScopeConfig(null, null, null));
    }

    /**
     * Starts {@code task} in its own thread.
     *
     * @return a handle whose result is only readable once the scope has been joined
     */
    <U extends T> Subtask<U> fork(Callable<? extends U> task);

    /** Starts {@code task} in its own thread. Its subtask produces {@code null}. */
    <U extends T> Subtask<U> fork(Runnable task);

    /**
     * Waits until the joiner is satisfied — every subtask finished, or the policy stopped early —
     * and returns what the joiner made of it.
     *
     * @throws FailedException if the joiner ends on a failed subtask
     * @throws TimeoutException if the scope was configured with a timeout and it ran out
     */
    R join() throws InterruptedException;

    /** Whether the scope stopped early, so subtasks still running have been interrupted. */
    boolean isCancelled();

    /**
     * Cancels anything still running, waits for it, and closes the scope.
     *
     * <p>The waiting is the point: a scope cannot be left behind with a subtask still going, which
     * is the guarantee the whole class exists to give.
     */
    @Override
    void close();

    /**
     * The handle a fork hands back: a {@link Supplier} whose value only exists once the subtask has
     * succeeded, and which says so rather than returning null.
     */
    interface Subtask<T> extends Supplier<T> {

        /** Where a subtask stands. There is no RUNNING: from outside, unfinished is unavailable. */
        enum State {
            /** Still running, or the scope was cancelled before it finished. */
            UNAVAILABLE,
            /** Finished and produced a result. */
            SUCCESS,
            /** Finished by throwing. */
            FAILED
        }

        State state();

        /**
         * The result.
         *
         * @throws IllegalStateException if the subtask did not succeed — asking a failed or
         *         unfinished subtask for its value is a bug in the caller, not a null
         */
        @Override
        T get();

        /**
         * What it threw.
         *
         * @throws IllegalStateException if the subtask did not fail
         */
        Throwable exception();
    }

    /**
     * The policy: when has the scope seen enough, and what does {@link #join} return.
     *
     * <p>The two callbacks return "cancel now", which is what lets a joiner stop the scope the
     * moment its question is answered — the first success, or the first failure — instead of
     * waiting for subtasks whose results nobody will look at.
     */
    interface Joiner<T, R> {

        /** Called as each subtask is forked. */
        default boolean onFork(Subtask<? extends T> subtask) {
            return false;
        }

        /** Called as each subtask finishes. */
        default boolean onComplete(Subtask<? extends T> subtask) {
            return false;
        }

        /** What {@link StructuredTaskScope#join} returns, or throws. */
        R result() throws Throwable;

        /** Waits for all; fails on the first failure; yields every subtask. */
        static <T> Joiner<T, Stream<Subtask<T>>> allSuccessfulOrThrow() {
            return new AllSuccessJoiner<T>();
        }

        /** Stops at the first success and yields its result; fails if none succeeded. */
        static <T> Joiner<T, T> anySuccessfulResultOrThrow() {
            return new AnySuccessJoiner<T>();
        }

        /** Waits for all; fails on the first failure; yields nothing. */
        static <T> Joiner<T, Void> awaitAllSuccessfulOrThrow() {
            return new AwaitSuccessJoiner<T>();
        }

        /** Waits for all, whatever happened to them. Failures are the caller's to inspect. */
        static <T> Joiner<T, Void> awaitAll() {
            return new AwaitAllJoiner<T>();
        }

        /** Waits until {@code isDone} accepts a finished subtask, then stops; yields them all. */
        static <T> Joiner<T, Stream<Subtask<T>>> allUntil(Predicate<Subtask<? extends T>> isDone) {
            if (isDone == null) {
                throw new NullPointerException();
            }
            return new AllUntilJoiner<T>(isDone);
        }
    }

    /** How a scope is set up, built by chaining from the one {@link #open} hands the function. */
    interface Configuration {

        Configuration withThreadFactory(ThreadFactory threadFactory);

        Configuration withName(String name);

        Configuration withTimeout(Duration timeout);
    }

    /** Thrown by {@link #join} when the joiner ends on a subtask that threw. */
    final class FailedException extends RuntimeException {

        FailedException(Throwable cause) {
            super(cause);
        }
    }

    /** Thrown by {@link #join} when the scope's timeout ran out first. */
    final class TimeoutException extends RuntimeException {

        TimeoutException() {
            super("the scope timed out");
        }
    }
}


/** An immutable configuration: each {@code with} returns a new one. */
final class ScopeConfig implements StructuredTaskScope.Configuration {

    private final ThreadFactory factory;
    private final String name;
    private final Duration timeout;

    ScopeConfig(ThreadFactory factory, String name, Duration timeout) {
        this.factory = factory;
        this.name = name;
        this.timeout = timeout;
    }

    @Override
    public StructuredTaskScope.Configuration withThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        return new ScopeConfig(threadFactory, this.name, this.timeout);
    }

    @Override
    public StructuredTaskScope.Configuration withName(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        return new ScopeConfig(this.factory, name, this.timeout);
    }

    @Override
    public StructuredTaskScope.Configuration withTimeout(Duration timeout) {
        if (timeout == null) {
            throw new NullPointerException();
        }
        return new ScopeConfig(this.factory, this.name, timeout);
    }

    ThreadFactory factory() {
        return this.factory;
    }

    String name() {
        return this.name;
    }

    Duration timeout() {
        return this.timeout;
    }
}


/**
 * The scope itself: a list of forked subtasks, a count of the ones still running, and the joiner
 * that decides when to stop waiting.
 *
 * <p>The joiner is held RAW. Its declared type is {@code StructuredTaskScope.Joiner<? super T, ? extends R>}, and
 * calling through a wildcard-typed reference is rejected or, worse, silently dropped (finding
 * #253). Raw has no wildcard to lose, and the public signatures stay faithful.
 *
 * <p>A {@code sync} object with single-exit {@code synchronized} blocks, not synchronized methods:
 * the method modifier is dropped from the emitted flags (finding #255), so {@code wait} and
 * {@code notifyAll} inside one would run with no monitor.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class ScopeImpl<T, R> implements StructuredTaskScope<T, R> {

    private final Object sync = new Object();

    private final StructuredTaskScope.Joiner joiner;
    private final ThreadFactory factory;
    private final String name;
    private final Duration timeout;

    private final ArrayList<ForkedTask> forked;

    private int running;
    private boolean cancelled;
    private boolean joined;
    private boolean closed;

    ScopeImpl(StructuredTaskScope.Joiner joiner, ScopeConfig config) {
        this.joiner = joiner;
        this.factory = config.factory();
        this.name = config.name();
        this.timeout = config.timeout();
        this.forked = new ArrayList<ForkedTask>();
        this.running = 0;
        this.cancelled = false;
        this.joined = false;
        this.closed = false;
    }

    @Override
    public <U extends T> StructuredTaskScope.Subtask<U> fork(Callable<? extends U> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkedTask<U> forked = new ForkedTask<U>(this, task, null);
        this.start(forked);
        return forked;
    }

    @Override
    public <U extends T> StructuredTaskScope.Subtask<U> fork(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkedTask<U> forked = new ForkedTask<U>(this, null, task);
        this.start(forked);
        return forked;
    }

    // The half of fork that does not depend on which kind of task it was. It takes the task RAW and
    // returns nothing: inferring `<U extends T>` across this call fails (the #100 family), and the
    // caller already holds the typed handle it needs to hand back.
    private void start(ForkedTask task) {
        synchronized (this.sync) {
            if (this.closed) {
                throw new IllegalStateException("scope is closed");
            }
            if (this.joined) {
                throw new IllegalStateException("scope has already been joined");
            }
            this.forked.add(task);
            this.running = this.running + 1;
        }
        // Through a RAW local, which is the one form of this call the compiler accepts: passing a
        // typed handle to a parameter declared `Subtask<? extends T>` is finding #253, and a raw
        // supertype local is what erases the wildcard out of the way.
        StructuredTaskScope.Subtask handle = task;
        // Outside the monitor: the joiner is the caller's code.
        boolean stop = this.joiner.onFork(handle);
        Thread thread = this.newThread(task);
        task.attach(thread);
        thread.start();
        if (stop) {
            this.cancel();
        }
    }

    private Thread newThread(ForkedTask task) {
        Thread thread;
        if (this.factory != null) {
            thread = this.factory.newThread(task);
            if (thread == null) {
                throw new IllegalStateException("the thread factory declined to create a thread");
            }
        } else {
            thread = new Thread(task);
        }
        if (this.name != null) {
            thread.setName(this.name + "-" + this.forked.size());
        }
        return thread;
    }

    // Called by a subtask's thread as it finishes.
    void completed(ForkedTask task) {
        // The joiner runs with nothing held: it is the caller's code, and it may look at the
        // subtask, which takes the subtask's own monitor. Raw local, as in start(): #253.
        StructuredTaskScope.Subtask handle = task;
        boolean stop = this.joiner.onComplete(handle);
        synchronized (this.sync) {
            this.running = this.running - 1;
            this.sync.notifyAll();
        }
        if (stop) {
            this.cancel();
        }
    }

    // Marks the scope cancelled and interrupts whatever is still running. Waking join() matters as
    // much as the interrupts: a subtask that ignores interruption must not hold join() forever.
    private void cancel() {
        ArrayList<ForkedTask> live = new ArrayList<ForkedTask>();
        synchronized (this.sync) {
            if (!this.cancelled) {
                this.cancelled = true;
                int i = 0;
                while (i < this.forked.size()) {
                    live.add(this.forked.get(i));
                    i = i + 1;
                }
            }
            this.sync.notifyAll();
        }
        int i = 0;
        while (i < live.size()) {
            ForkedTask task = live.get(i);
            task.interrupt();
            i = i + 1;
        }
    }

    @Override
    public boolean isCancelled() {
        boolean stopped;
        synchronized (this.sync) {
            stopped = this.cancelled;
        }
        return stopped;
    }

    @Override
    public R join() throws InterruptedException {
        boolean timedOut = this.await();
        synchronized (this.sync) {
            this.joined = true;
        }
        if (timedOut) {
            this.cancel();
            throw new StructuredTaskScope.TimeoutException();
        }
        Object value;
        try {
            value = this.joiner.result();
        } catch (Error failure) {
            // An Error is not the subtask's failure being reported, it is the VM giving up. Wrapping
            // it would hide that.
            throw failure;
        } catch (Throwable failure) {
            // Everything else is wrapped, RuntimeException included: what a subtask threw reaches
            // the caller as the CAUSE of a FailedException, never as itself. A caller that catches
            // IllegalStateException around join() must not accidentally catch a subtask's.
            throw new StructuredTaskScope.FailedException(failure);
        }
        return (R) value;
    }

    // Waits for the subtasks. Reports whether the wait ran out of time rather than throwing from
    // inside the block, because a throw is an early exit and an early exit loses the monitor
    // (finding #105).
    private boolean await() throws InterruptedException {
        boolean expired = false;
        long deadline = 0L;
        boolean bounded = this.timeout != null;
        if (bounded) {
            deadline = System.currentTimeMillis() + this.timeout.toMillis();
        }
        synchronized (this.sync) {
            while (this.running > 0 && !this.cancelled && !expired) {
                if (bounded) {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0L) {
                        expired = true;
                    } else {
                        this.sync.wait(left);
                    }
                } else {
                    this.sync.wait();
                }
            }
        }
        return expired;
    }

    @Override
    public void close() {
        boolean unjoined;
        synchronized (this.sync) {
            unjoined = !this.joined && this.forked.size() > 0;
        }
        this.cancel();
        this.awaitThreads();
        synchronized (this.sync) {
            this.closed = true;
        }
        if (unjoined) {
            // The scope is clean by now -- nothing is left running -- but forking without joining
            // is a bug in the block, and staying quiet about it would defeat the point of the class.
            throw new IllegalStateException("the scope was closed without being joined");
        }
    }

    // Waits on the threads themselves, not on the counter: after a cancel a subtask may be ignoring
    // its interrupt, and close() promises that nothing outlives the scope.
    private void awaitThreads() {
        ArrayList<ForkedTask> live = new ArrayList<ForkedTask>();
        synchronized (this.sync) {
            int i = 0;
            while (i < this.forked.size()) {
                live.add(this.forked.get(i));
                i = i + 1;
            }
        }
        int i = 0;
        while (i < live.size()) {
            ForkedTask task = live.get(i);
            task.awaitEnd();
            i = i + 1;
        }
    }
}


/** One forked subtask: the thread body, and the handle the caller holds. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class ForkedTask<U> implements StructuredTaskScope.Subtask<U>, Runnable {

    private final Object sync = new Object();

    private final ScopeImpl scope;
    private final Callable callable;
    private final Runnable runnable;

    private Thread thread;
    private Object value;
    private Throwable failure;
    private boolean done;

    ForkedTask(ScopeImpl scope, Callable callable, Runnable runnable) {
        this.scope = scope;
        this.callable = callable;
        this.runnable = runnable;
        this.thread = null;
        this.value = null;
        this.failure = null;
        this.done = false;
    }

    void attach(Thread thread) {
        synchronized (this.sync) {
            this.thread = thread;
        }
    }

    void interrupt() {
        Thread running;
        synchronized (this.sync) {
            running = this.thread;
            if (this.done) {
                running = null;
            }
        }
        if (running != null) {
            running.interrupt();
        }
    }

    void awaitEnd() {
        Thread running;
        synchronized (this.sync) {
            running = this.thread;
        }
        if (running != null) {
            try {
                running.join();
            } catch (InterruptedException e) {
                // The thread closing the scope was interrupted while waiting. The interrupt is
                // re-raised rather than swallowed, and the wait ends: close() cannot promise more
                // than "I waited until I was told to stop".
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        Object produced = null;
        Throwable thrown = null;
        try {
            if (this.callable != null) {
                produced = this.callable.call();
            } else {
                this.runnable.run();
            }
        } catch (Throwable failing) {
            thrown = failing;
        }
        synchronized (this.sync) {
            this.value = produced;
            this.failure = thrown;
            this.done = true;
        }
        this.scope.completed(this);
    }

    @Override
    public StructuredTaskScope.Subtask.State state() {
        StructuredTaskScope.Subtask.State where;
        synchronized (this.sync) {
            if (!this.done) {
                where = StructuredTaskScope.Subtask.State.UNAVAILABLE;
            } else if (this.failure != null) {
                where = StructuredTaskScope.Subtask.State.FAILED;
            } else {
                where = StructuredTaskScope.Subtask.State.SUCCESS;
            }
        }
        return where;
    }

    @Override
    public U get() {
        Object produced;
        boolean ok;
        synchronized (this.sync) {
            ok = this.done && this.failure == null;
            produced = this.value;
        }
        if (!ok) {
            throw new IllegalStateException("the subtask did not complete with a result");
        }
        return (U) produced;
    }

    @Override
    public Throwable exception() {
        Throwable thrown;
        synchronized (this.sync) {
            thrown = this.failure;
            if (!this.done) {
                thrown = null;
            }
        }
        if (thrown == null) {
            throw new IllegalStateException("the subtask did not fail");
        }
        return thrown;
    }
}


/** Waits for every subtask and reports nothing: failures are the caller's to inspect. */
final class AwaitAllJoiner<T> implements StructuredTaskScope.Joiner<T, Void> {

    @Override
    public Void result() {
        return null;
    }
}


/** Waits for every subtask, and ends the scope at the first one that threw. */
final class AwaitSuccessJoiner<T> implements StructuredTaskScope.Joiner<T, Void> {

    private final Object sync = new Object();
    private Throwable first;

    AwaitSuccessJoiner() {
        this.first = null;
    }

    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        boolean stop = false;
        StructuredTaskScope.Subtask.State where = subtask.state();
        if (where == StructuredTaskScope.Subtask.State.FAILED) {
            Throwable thrown = subtask.exception();
            synchronized (this.sync) {
                if (this.first == null) {
                    this.first = thrown;
                }
            }
            stop = true;
        }
        return stop;
    }

    @Override
    public Void result() throws Throwable {
        Throwable thrown;
        synchronized (this.sync) {
            thrown = this.first;
        }
        if (thrown != null) {
            throw thrown;
        }
        return null;
    }
}


/** Like {@link AwaitSuccessJoiner}, but hands back every subtask. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class AllSuccessJoiner<T> implements StructuredTaskScope.Joiner<T, Stream<StructuredTaskScope.Subtask<T>>> {

    private final Object sync = new Object();
    private final ArrayList seen;
    private Throwable first;

    AllSuccessJoiner() {
        this.seen = new ArrayList();
        this.first = null;
    }

    @Override
    public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
        synchronized (this.sync) {
            this.seen.add(subtask);
        }
        return false;
    }

    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        boolean stop = false;
        StructuredTaskScope.Subtask.State where = subtask.state();
        if (where == StructuredTaskScope.Subtask.State.FAILED) {
            Throwable thrown = subtask.exception();
            synchronized (this.sync) {
                if (this.first == null) {
                    this.first = thrown;
                }
            }
            stop = true;
        }
        return stop;
    }

    @Override
    public Stream<StructuredTaskScope.Subtask<T>> result() throws Throwable {
        Throwable thrown;
        Object[] all;
        synchronized (this.sync) {
            thrown = this.first;
            all = new Object[this.seen.size()];
            int i = 0;
            while (i < this.seen.size()) {
                all[i] = this.seen.get(i);
                i = i + 1;
            }
        }
        if (thrown != null) {
            throw thrown;
        }
        Stream raw = Stream.of(all);
        return (Stream<StructuredTaskScope.Subtask<T>>) raw;
    }
}


/** Ends the scope at the FIRST success and yields its value; fails if nobody succeeded. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class AnySuccessJoiner<T> implements StructuredTaskScope.Joiner<T, T> {

    private final Object sync = new Object();
    private Object winner;
    private boolean won;
    private Throwable last;

    AnySuccessJoiner() {
        this.winner = null;
        this.won = false;
        this.last = null;
    }

    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        boolean stop = false;
        StructuredTaskScope.Subtask.State where = subtask.state();
        if (where == StructuredTaskScope.Subtask.State.SUCCESS) {
            Object produced = subtask.get();
            synchronized (this.sync) {
                if (!this.won) {
                    this.won = true;
                    this.winner = produced;
                }
            }
            stop = true;
        } else if (where == StructuredTaskScope.Subtask.State.FAILED) {
            Throwable thrown = subtask.exception();
            synchronized (this.sync) {
                this.last = thrown;
            }
        }
        return stop;
    }

    @Override
    public T result() throws Throwable {
        Object produced;
        boolean have;
        Throwable thrown;
        synchronized (this.sync) {
            have = this.won;
            produced = this.winner;
            thrown = this.last;
        }
        if (!have) {
            if (thrown != null) {
                throw thrown;
            }
            throw new java.util.NoSuchElementException("no subtask succeeded");
        }
        return (T) produced;
    }
}


/**
 * Ends the scope when the predicate accepts a finished subtask.
 *
 * <p>The predicate is held RAW, for the reason the scope holds its joiner raw: passing a
 * wildcard-typed subtask to a {@code Predicate<StructuredTaskScope.Subtask<? extends T>>} is finding #253.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class AllUntilJoiner<T> implements StructuredTaskScope.Joiner<T, Stream<StructuredTaskScope.Subtask<T>>> {

    private final Object sync = new Object();
    private final Predicate isDone;
    private final ArrayList seen;

    AllUntilJoiner(Predicate isDone) {
        this.isDone = isDone;
        this.seen = new ArrayList();
    }

    @Override
    public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
        synchronized (this.sync) {
            this.seen.add(subtask);
        }
        return false;
    }

    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        Object item = subtask;
        return this.isDone.test(item);
    }

    @Override
    public Stream<StructuredTaskScope.Subtask<T>> result() {
        Object[] all;
        synchronized (this.sync) {
            all = new Object[this.seen.size()];
            int i = 0;
            while (i < this.seen.size()) {
                all[i] = this.seen.get(i);
                i = i + 1;
            }
        }
        Stream raw = Stream.of(all);
        return (Stream<StructuredTaskScope.Subtask<T>>) raw;
    }
}
