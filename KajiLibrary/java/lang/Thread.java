package java.lang;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

// KajiLibrary's java.lang.Thread. The scheduler operations (`start`, `join`, `interrupt`,
// `sleep`, `yield`, `getState`, `currentThread`, `holdsLock`) are intercepted by the VM —
// they create, park and wake threads on its substrate, which since the concurrency work can
// be green threads on one carrier, OS threads behind a GIL, or truly parallel OS threads.
// Which one is a VM decision; this class is the same Java-side contract either way. Most of
// them are *not* `native`: the VM recognises them by name and runs its scheduler op instead
// of the (unreachable) body, exactly as the reference JDK's own non-native `start`/`join`
// route through `start0`/`join0`.
//
// The identity fields (`tid`, `name`) and the interrupt flag live here, in Java, because the
// VM reads them back by *name* (`field_offset`) — `interrupted` in particular is written by
// the scheduler when another thread interrupts this one.
//
// A KajiLibrary honesty note on **virtual threads**: KajiJDK has no separate virtual-thread
// carrier, so a thread built through {@link #ofVirtual()} or {@link #startVirtualThread} runs
// on the very same substrate as a platform thread. The {@code virtual} flag is remembered so
// {@link #isVirtual()} reports what was asked for, but the scheduling is identical.
public class Thread implements Runnable {

    /** The minimum priority a thread can have. */
    public static final int MIN_PRIORITY = 1;

    /** The default priority a thread is given. */
    public static final int NORM_PRIORITY = 5;

    /** The maximum priority a thread can have. */
    public static final int MAX_PRIORITY = 10;

    // The Runnable handed to the `Thread(Runnable)` ctor, or null when a subclass overrides
    // `run()` instead. `run()` below is what makes the two forms behave the same.
    private Runnable target;

    // A monotonic id, handed out by the VM. Final: identity never changes.
    private final long tid;

    // The thread's name. Defaults to `Thread-<tid>`; `setName` can replace it.
    private String name;

    // Set by the scheduler (from another thread) when this one is interrupted. `volatile`:
    // the interrupting thread and this one are genuinely concurrent under the OS substrates,
    // so the write has to be visible without a lock.
    private volatile boolean interrupted;

    // The group this thread belongs to. Every thread is in exactly one, and the group is what
    // lets several threads be interrupted or counted together.
    private ThreadGroup group;

    // Scheduling hint in [MIN_PRIORITY, MAX_PRIORITY]. A hint only: KajiJDK's schedulers are
    // fair round-robin and do not weight by priority. `0` means "never set" — see getPriority.
    private int priority;

    // Whether this is a daemon thread. Daemon threads don't keep the VM alive on their own.
    private boolean daemon;

    // True for a thread built by the virtual-thread builders. KajiJDK runs it as a platform
    // thread regardless; the flag exists so `isVirtual()` is truthful about the request.
    private boolean virtual;

    // The requested stack size in bytes, or 0 for "the VM decides". KajiJDK ignores it.
    private long stackSize;

    // The context class loader carried by this thread, or null to fall back to the system one.
    private ClassLoader contextClassLoader;

    // A per-thread handler for exceptions that escape `run()`, or null to defer to the group
    // (which is itself an UncaughtExceptionHandler) and then to the default handler.
    private UncaughtExceptionHandler uncaughtHandler;

    // The VM-wide fallback handler, consulted when neither the thread nor its group handles.
    private static volatile UncaughtExceptionHandler defaultUncaughtHandler;

    // --- constructors ---

    public Thread() {
        this(null, null, null, 0L);
    }

    public Thread(Runnable target) {
        this(null, target, null, 0L);
    }

    public Thread(String name) {
        this(null, null, name, 0L);
    }

    public Thread(Runnable target, String name) {
        this(null, target, name, 0L);
    }

    public Thread(ThreadGroup group, Runnable target) {
        this(group, target, null, 0L);
    }

    public Thread(ThreadGroup group, String name) {
        this(group, null, name, 0L);
    }

    public Thread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0L);
    }

    public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
        this(group, target, name, stackSize, true);
    }

    public Thread(ThreadGroup group, Runnable target, String name, long stackSize,
            boolean inheritInheritableThreadLocals) {
        this.tid = nextThreadNum();
        this.target = target;
        this.name = (name != null) ? name : "Thread-" + this.tid;
        // A thread inherits its creator's group unless one is named. The creating thread is the
        // one running this constructor -- `currentThread()`, not the thread being built.
        this.group = (group != null) ? group : currentThread().getThreadGroup();
        this.stackSize = stackSize;
        // Inherit the creator's priority, capped at the group's ceiling.
        int parent = currentThread().getPriority();
        int max = this.group.getMaxPriority();
        this.priority = parent < max ? parent : max;
        this.daemon = currentThread().isDaemon();
        // `inheritInheritableThreadLocals` is accepted for source compatibility; KajiJDK does
        // not model InheritableThreadLocal, so there is nothing to copy.
    }

    // --- identity ---

    // The next id in the VM's sequence. Private: it's the ctor's helper, not API.
    private static native long nextThreadNum();

    public final long threadId() {
        return tid;
    }

    /**
     * @deprecated use {@link #threadId()} — this returns the same value.
     */
    @Deprecated
    public long getId() {
        return tid;
    }

    public final String getName() {
        return name;
    }

    // The group this thread belongs to.
    //
    // Resolved lazily when it is null, which happens for the thread the VM starts the program
    // on: that Thread object is built by the VM without running a constructor, so nothing
    // attached it to a group.
    public final ThreadGroup getThreadGroup() {
        if (this.group == null) {
            this.group = ThreadGroup.root();
        }
        return this.group;
    }

    public final synchronized void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        this.name = name;
    }

    /** Whether this is a virtual thread. KajiJDK runs virtual threads on the platform substrate. */
    public final boolean isVirtual() {
        return this.virtual;
    }

    // The Thread object of the currently running thread. VM-intercepted (it reads scheduler
    // state); the entry/main thread gets a bare Thread lazily on first call.
    public static native Thread currentThread();

    // Whether the current thread owns `o`'s monitor — the check `synchronized` relies on.
    public static native boolean holdsLock(Object o);

    // --- priority ---

    // The scheduling priority. A thread built without a constructor (the VM's `main`) never had
    // this set, so a `0` reads as the default -- the same lazy-default trick as getThreadGroup.
    public final int getPriority() {
        if (this.priority == 0) {
            this.priority = NORM_PRIORITY;
        }
        return this.priority;
    }

    public final void setPriority(int newPriority) {
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        ThreadGroup g = getThreadGroup();
        int max = g.getMaxPriority();
        this.priority = newPriority < max ? newPriority : max;
    }

    // --- daemon ---

    public final boolean isDaemon() {
        return this.daemon;
    }

    public final void setDaemon(boolean on) {
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        this.daemon = on;
    }

    /**
     * A no-op access check. KajiJDK has no {@code SecurityManager} (removed from the platform),
     * so nothing is ever denied — the method is kept for source compatibility.
     */
    public final void checkAccess() {
    }

    // --- lifecycle ---

    // The VM spawns a new thread that runs this object's `run()` and returns immediately.
    // Not native: the VM intercepts this call and does the spawn, so the body is never reached.
    public void start() {
        // Intercepted by the VM (it must touch the thread list and spawn a scheduler slot).
        throw new UnsupportedOperationException("Thread.start is intercepted by the VM");
    }

    // The body of the thread. A bare Thread runs its `target` if it was given one; a
    // subclass overrides this instead. Both forms end up here, which is why `start()`
    // only ever has to call `run()`.
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    // Block the current thread until this one terminates. VM-intercepted scheduler op; the
    // body is never reached.
    public final void join() throws InterruptedException {
        throw new UnsupportedOperationException("Thread.join is intercepted by the VM");
    }

    // Block the current thread until this one terminates, or `millis` elapse. VM-intercepted
    // (it arms both a deadline and a termination wake); the body is never reached.
    public final void join(long millis) throws InterruptedException {
        throw new UnsupportedOperationException("Thread.join(long) is intercepted by the VM");
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
        // Round a sub-millisecond remainder up to a whole millisecond -- KajiJDK's clock is
        // millisecond-grained, exactly as the reference does before its own nanosecond path.
        if (nanos > 0 && millis < Long.MAX_VALUE) {
            millis = millis + 1;
        }
        join(millis);
    }

    public final boolean join(Duration duration) throws InterruptedException {
        if (duration == null) {
            throw new NullPointerException("duration cannot be null");
        }
        long millis = duration.toMillis();
        if (millis <= 0) {
            // A non-positive timeout does not wait; report the thread's current liveness.
            return !isAlive();
        }
        join(millis);
        return !isAlive();
    }

    // Sleep the current thread for `ms`. Under the green scheduler that's ticks of the VM's
    // opcode clock; under the OS substrates it's real wall time. VM-intercepted.
    public static void sleep(long millis) throws InterruptedException {
        throw new UnsupportedOperationException("Thread.sleep is intercepted by the VM");
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
        if (nanos > 0 && millis < Long.MAX_VALUE) {
            millis = millis + 1;
        }
        sleep(millis);
    }

    public static void sleep(Duration duration) throws InterruptedException {
        if (duration == null) {
            throw new NullPointerException("duration cannot be null");
        }
        long millis = duration.toMillis();
        if (millis <= 0) {
            return;
        }
        sleep(millis);
    }

    // Offer the scheduler a chance to run someone else. A hint, not a guarantee.
    // VM-intercepted (a no-op beyond a yield point under the cooperative scheduler).
    public static void yield() {
        throw new UnsupportedOperationException("Thread.yield is intercepted by the VM");
    }

    /**
     * A hint that the caller is spin-waiting. KajiJDK has nothing to relax, so this is a no-op.
     */
    public static void onSpinWait() {
    }

    // Alive = started and not finished. Derived from the authoritative scheduler state
    // rather than a flag of our own, so it can't drift out of sync.
    public final boolean isAlive() {
        State s = getState();
        return s != State.NEW && s != State.TERMINATED;
    }

    // The scheduler's view of this thread. VM-intercepted: it has to initialize `State`
    // first, since the constant objects only exist once `State.<clinit>` has run.
    public State getState() {
        throw new UnsupportedOperationException("Thread.getState is intercepted by the VM");
    }

    /**
     * {@return an always-empty stack trace}. KajiJDK does not expose another thread's Java
     * stack through reflection, so there is no frame information to report.
     */
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    /**
     * {@return a map of every live thread to its (empty) stack trace}. KajiJDK cannot walk the
     * stacks of other threads, so each maps to an empty array; the reference reports real frames.
     */
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        return new HashMap<Thread, StackTraceElement[]>();
    }

    /**
     * Prints the current thread's stack trace. KajiJDK builds no stack snapshot for this, so
     * it prints an empty trace rather than fabricating frames.
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    /**
     * The number of active threads in the current thread's group (and its subgroups). An
     * estimate, as the reference warns: the count can change while it is being taken.
     */
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    /**
     * Copies into {@code tarray} the active threads of the current thread's group, returning how
     * many were stored. Delegates to the group, like the reference.
     */
    public static int enumerate(Thread[] tarray) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }

    /**
     * Throws {@link UnsupportedOperationException} — {@code Thread.stop} is degraded to a
     * permanent failure, exactly as in the reference (it was always unsafe and is now inert).
     */
    public final void stop() {
        throw new UnsupportedOperationException();
    }

    // --- interruption ---

    // Ask the VM to interrupt this thread: sets the flag and, if the thread is parked in
    // sleep/wait/join, wakes it there with an InterruptedException. VM-intercepted.
    public void interrupt() {
        throw new UnsupportedOperationException("Thread.interrupt is intercepted by the VM");
    }

    // This thread's flag, read without clearing it.
    public boolean isInterrupted() {
        return interrupted;
    }

    // The *current* thread's flag, read **and cleared** — the JDK's asymmetry between the
    // instance method above and this static one, kept deliberately.
    public static boolean interrupted() {
        Thread t = currentThread();
        boolean was = t.interrupted;
        t.interrupted = false;
        return was;
    }

    // --- context class loader ---

    public ClassLoader getContextClassLoader() {
        if (this.contextClassLoader == null) {
            return ClassLoader.getSystemClassLoader();
        }
        return this.contextClassLoader;
    }

    public void setContextClassLoader(ClassLoader cl) {
        this.contextClassLoader = cl;
    }

    // --- uncaught exception handlers ---

    /** The handler for exceptions escaping this thread: its own if set, else its group. */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        if (this.uncaughtHandler != null) {
            return this.uncaughtHandler;
        }
        // A ThreadGroup is itself an UncaughtExceptionHandler; the cast is explicit because the
        // frozen javac does not chase the subtype through the nested-interface name on its own.
        return (UncaughtExceptionHandler) getThreadGroup();
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        this.uncaughtHandler = eh;
    }

    /** The VM-wide default handler, or null if none is installed. */
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtHandler;
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        defaultUncaughtHandler = eh;
    }

    // --- string form ---

    public String toString() {
        ThreadGroup g = getThreadGroup();
        String groupName = (g != null) ? g.getName() : "";
        return "Thread[#" + tid + "," + name + "," + getPriority() + "," + groupName + "]";
    }

    // --- virtual / builder API ---

    /**
     * Starts a virtual thread running {@code task} and returns it. KajiJDK has no virtual-thread
     * carrier, so the thread runs on the platform substrate; {@link #isVirtual()} still reports
     * {@code true}.
     */
    public static Thread startVirtualThread(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task cannot be null");
        }
        Thread t = new Thread(task);
        t.virtual = true;
        t.daemon = true;
        t.start();
        return t;
    }

    /** {@return a builder for a platform thread}. */
    public static Builder.OfPlatform ofPlatform() {
        return new PlatformBuilder();
    }

    /** {@return a builder for a virtual thread} (run on the platform substrate in KajiJDK). */
    public static Builder.OfVirtual ofVirtual() {
        return new VirtualBuilder();
    }

    // The lifecycle states the scheduler distinguishes. NEW is "constructed but never
    // started"; TERMINATED is "run() returned". The three in between are why a thread
    // isn't running right now: waiting on a monitor (BLOCKED), waiting to be notified
    // (WAITING), or the same with a deadline (TIMED_WAITING).
    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }

    // What to do with a throwable that got out of a thread run() -- installed per thread or per
    // group. It exists because by the time it is called the thread is already finishing: there
    // is nothing to catch, only something to report.
    public interface UncaughtExceptionHandler {

        void uncaughtException(Thread t, Throwable e);
    }

    /**
     * A builder for {@link Thread}s (JEP 444). Accumulates configuration, then {@link #unstarted}
     * or {@link #start}s a thread, or hands back a {@link ThreadFactory} that repeats the recipe.
     */
    public interface Builder {

        Builder name(String name);

        Builder name(String prefix, long start);

        Builder inheritInheritableThreadLocals(boolean inherit);

        Builder uncaughtExceptionHandler(UncaughtExceptionHandler ueh);

        Thread unstarted(Runnable task);

        Thread start(Runnable task);

        ThreadFactory factory();

        /** A {@link Builder} that builds platform threads. */
        interface OfPlatform extends Builder {

            OfPlatform name(String name);

            OfPlatform name(String prefix, long start);

            OfPlatform inheritInheritableThreadLocals(boolean inherit);

            OfPlatform uncaughtExceptionHandler(UncaughtExceptionHandler ueh);

            OfPlatform group(ThreadGroup group);

            OfPlatform daemon(boolean on);

            default OfPlatform daemon() {
                return daemon(true);
            }

            OfPlatform priority(int priority);

            OfPlatform stackSize(long stackSize);
        }

        /** A {@link Builder} that builds virtual threads. */
        interface OfVirtual extends Builder {

            OfVirtual name(String name);

            OfVirtual name(String prefix, long start);

            OfVirtual inheritInheritableThreadLocals(boolean inherit);

            OfVirtual uncaughtExceptionHandler(UncaughtExceptionHandler ueh);
        }
    }

    // Shared state and thread-minting for the two builder flavours. Nested in Thread, so it
    // sets the new thread's private fields directly.
    private abstract static class BaseBuilder {

        String fixedName;         // a name set by name(String), or null
        String namePrefix;        // a counting prefix set by name(String, long), or null
        long nameCounter;         // the next number for a counting prefix
        boolean inheritThreadLocals = true;
        UncaughtExceptionHandler ueh;

        // Compute the name for the next thread this builder makes.
        final String nextName() {
            if (this.namePrefix != null) {
                String n = this.namePrefix + this.nameCounter;
                this.nameCounter = this.nameCounter + 1;
                return n;
            }
            return this.fixedName;
        }

        // Build a fully-configured but unstarted Thread. Subclasses stamp the flavour-specific
        // fields via `decorate`.
        final Thread make(Runnable task) {
            Thread t = new Thread(null, task, nextName(), 0L, this.inheritThreadLocals);
            if (this.ueh != null) {
                t.uncaughtHandler = this.ueh;
            }
            decorate(t);
            return t;
        }

        abstract void decorate(Thread t);
    }

    private static final class PlatformBuilder extends BaseBuilder implements Builder.OfPlatform {

        private ThreadGroup group;
        private boolean daemon;
        private boolean daemonSet;
        private int priority;
        private long stackSize;

        public Builder.OfPlatform name(String name) {
            if (name == null) {
                throw new NullPointerException("name cannot be null");
            }
            this.fixedName = name;
            this.namePrefix = null;
            return this;
        }

        public Builder.OfPlatform name(String prefix, long start) {
            if (prefix == null) {
                throw new NullPointerException("prefix cannot be null");
            }
            if (start < 0) {
                throw new IllegalArgumentException("start must be non-negative");
            }
            this.namePrefix = prefix;
            this.nameCounter = start;
            this.fixedName = null;
            return this;
        }

        public Builder.OfPlatform inheritInheritableThreadLocals(boolean inherit) {
            this.inheritThreadLocals = inherit;
            return this;
        }

        public Builder.OfPlatform uncaughtExceptionHandler(UncaughtExceptionHandler ueh) {
            this.ueh = ueh;
            return this;
        }

        public Builder.OfPlatform group(ThreadGroup group) {
            if (group == null) {
                throw new NullPointerException("group cannot be null");
            }
            this.group = group;
            return this;
        }

        public Builder.OfPlatform daemon(boolean on) {
            this.daemon = on;
            this.daemonSet = true;
            return this;
        }

        public Builder.OfPlatform priority(int priority) {
            if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
                throw new IllegalArgumentException();
            }
            this.priority = priority;
            return this;
        }

        public Builder.OfPlatform stackSize(long stackSize) {
            if (stackSize < 0) {
                throw new IllegalArgumentException("stackSize must be non-negative");
            }
            this.stackSize = stackSize;
            return this;
        }

        void decorate(Thread t) {
            if (this.group != null) {
                t.group = this.group;
            }
            if (this.daemonSet) {
                t.daemon = this.daemon;
            }
            if (this.priority != 0) {
                t.priority = this.priority;
            }
            t.stackSize = this.stackSize;
            t.virtual = false;
        }

        public Thread unstarted(Runnable task) {
            if (task == null) {
                throw new NullPointerException("task cannot be null");
            }
            return make(task);
        }

        public Thread start(Runnable task) {
            Thread t = unstarted(task);
            t.start();
            return t;
        }

        public ThreadFactory factory() {
            return new BuilderFactory(this);
        }
    }

    private static final class VirtualBuilder extends BaseBuilder implements Builder.OfVirtual {

        public Builder.OfVirtual name(String name) {
            if (name == null) {
                throw new NullPointerException("name cannot be null");
            }
            this.fixedName = name;
            this.namePrefix = null;
            return this;
        }

        public Builder.OfVirtual name(String prefix, long start) {
            if (prefix == null) {
                throw new NullPointerException("prefix cannot be null");
            }
            if (start < 0) {
                throw new IllegalArgumentException("start must be non-negative");
            }
            this.namePrefix = prefix;
            this.nameCounter = start;
            this.fixedName = null;
            return this;
        }

        public Builder.OfVirtual inheritInheritableThreadLocals(boolean inherit) {
            this.inheritThreadLocals = inherit;
            return this;
        }

        public Builder.OfVirtual uncaughtExceptionHandler(UncaughtExceptionHandler ueh) {
            this.ueh = ueh;
            return this;
        }

        void decorate(Thread t) {
            // A virtual thread on KajiJDK still runs on the platform substrate, but reports as
            // virtual and defaults to daemon (virtual threads never keep the VM alive).
            t.virtual = true;
            t.daemon = true;
        }

        public Thread unstarted(Runnable task) {
            if (task == null) {
                throw new NullPointerException("task cannot be null");
            }
            return make(task);
        }

        public Thread start(Runnable task) {
            Thread t = unstarted(task);
            t.start();
            return t;
        }

        public ThreadFactory factory() {
            return new BuilderFactory(this);
        }
    }

    // The ThreadFactory a builder hands back: each newThread repeats the builder's recipe
    // (including its counting name), exactly as if `unstarted` had been called again.
    private static final class BuilderFactory implements ThreadFactory {

        private final BaseBuilder builder;

        BuilderFactory(BaseBuilder builder) {
            this.builder = builder;
        }

        public Thread newThread(Runnable task) {
            if (task == null) {
                throw new NullPointerException("task cannot be null");
            }
            return this.builder.make(task);
        }
    }
}
