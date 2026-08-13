package java.lang;

// Minimal java.lang.Thread for our green-thread scheduler. `start()` is native: the VM
// spawns a green thread that runs this object's `run()` (cooperatively scheduled onto
// the single OS thread). Subclasses override `run()`; the base `run()` does nothing.
public class Thread {
    // The task to run, for a thread built as `new Thread(runnable)`. Null when the thread
    // was made by subclassing and overriding `run()`.
    private Runnable target;
    // Identity, assigned at construction so even an unstarted thread has a stable id/name.
    // The id counter is the VM's (`nextThreadNum`); the default name mirrors the JDK's.
    private final long tid;
    private String name;
    // The interrupt status. It lives on the *object*, not the scheduler slot, because a
    // thread can be interrupted before it starts (a NEW thread has no slot yet, but
    // isInterrupted() must still report true). `volatile` for cross-thread visibility —
    // decorative under the GIL today, load-bearing once the GIL is gone.
    private volatile boolean interrupted;
    // Scheduling priority hint (1..10). Stored and reported per the API; our scheduler treats it
    // as advisory with no effect — priority is a hint even on real JVMs.
    private int priority = NORM_PRIORITY;
    // Daemon flag. The VM ends the program when `main` returns (it does not wait for background
    // threads), so this is a stored attribute that honors the lifecycle rule — it can't change
    // after the thread starts — rather than a driver of shutdown.
    private boolean daemon;
    // Head of this thread's ThreadLocal association list (see java.lang.ThreadLocal). Only this
    // thread touches its own list (always via currentThread()), so no synchronization is needed.
    ThreadLocal.Entry threadLocals;
    // Where an exception that escapes run() goes. The VM reads this field (by name) when it is
    // about to report an uncaught exception: a non-null handler is invoked instead of printing.
    private UncaughtExceptionHandler uncaughtExceptionHandler;
    // The process-wide fallback, used for any thread with no handler of its own. Also read by
    // the VM, straight out of Thread's mirror.
    private static UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    // The group this thread belongs to (see java.lang.ThreadGroup). Assigned at construction from
    // the creating thread's group, so groups form the same tree the JDK builds.
    private ThreadGroup group;

    public static final int MIN_PRIORITY = 1;
    public static final int NORM_PRIORITY = 5;
    public static final int MAX_PRIORITY = 10;

    public Thread() {
        this(null, null);
    }

    // The common non-subclass form: the thread runs `target.run()`. The target is usually
    // a lambda — which works because a lambda is just an object implementing Runnable.
    public Thread(Runnable target) {
        this(null, target);
    }

    // The full form: place the thread in `group` (null → the creating thread's own group, which
    // is what every other constructor asks for). Every construction funnels here, so the group
    // membership list is built in exactly one place.
    public Thread(ThreadGroup group, Runnable target) {
        this.tid = nextThreadNum();
        this.name = "Thread-" + tid;
        this.target = target;
        this.group = group != null ? group : currentThread().getThreadGroup();
        this.group.add(this);
    }

    // The Thread object of the caller's thread. VM-intercepted: it reads the scheduler's
    // "current" slot. The main thread gets its Thread object lazily on the first call.
    public static native Thread currentThread();

    // A fresh unique id, handed out by the VM. Native because the counter is the VM's.
    private static native long nextThreadNum();

    // Whether the current thread holds `o`'s intrinsic monitor. VM-intercepted (the monitor
    // ownership lives in the scheduler). Used to check the wait()-reacquire invariant.
    public static native boolean holdsLock(Object o);

    // This thread's id (Java 19+ name; the field is set once at construction).
    public final long threadId() {
        return tid;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final int getPriority() {
        return priority;
    }

    // Set the priority, clamped to the [MIN, MAX] range (an out-of-range value is rejected).
    public final void setPriority(int newPriority) {
        if (newPriority < MIN_PRIORITY || newPriority > MAX_PRIORITY) {
            throw new IllegalArgumentException();
        }
        this.priority = newPriority;
    }

    public final boolean isDaemon() {
        return daemon;
    }

    // Mark this thread daemon/non-daemon. Only legal before the thread starts (JVMS lifecycle
    // rule); after that it throws IllegalThreadStateException.
    public final void setDaemon(boolean on) {
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        this.daemon = on;
    }

    // Set this thread's interrupt status and, if it's parked in an interruptible wait
    // (sleep/join/wait), wake it so it can throw InterruptedException. Fully VM-handled:
    // the flag lives on this object but is written by the VM, which also does the waking.
    public native void interrupt();

    // Read this thread's interrupt status **without clearing** it.
    public boolean isInterrupted() {
        return this.interrupted;
    }

    // Read *and clear* the current thread's interrupt status — the destructive twin of
    // isInterrupted(). Static, so it always targets whoever is running.
    public static boolean interrupted() {
        Thread t = currentThread();
        boolean was = t.interrupted;
        t.interrupted = false;
        return was;
    }

    // The VM intercepts this: it creates a new green thread running `run()` and returns
    // immediately. (Declared native — it has no bytecode.) Starting an already-started
    // thread throws IllegalThreadStateException (also VM-checked).
    public native void start();

    // What the new thread executes. A subclass overrides this; the base version runs the
    // `Runnable` target if one was given (the `new Thread(runnable)` path), else does
    // nothing.
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    // Whether the thread has been started and has not yet finished. Pure Java on top of
    // getState(): everything between NEW and TERMINATED is alive.
    public final boolean isAlive() {
        State s = getState();
        return s != State.NEW && s != State.TERMINATED;
    }

    // Block until this thread terminates (no timeout). VM-intercepted (scheduler op).
    public final native void join();

    // Sleep the current thread for `ms` ticks of the VM's opcode clock. VM-intercepted.
    public static native void sleep(long ms);

    // Sleep with sub-millisecond precision — which our opcode clock does not have, so the nanos
    // are rounded to the nearest millisecond exactly the way the JDK has always done it: half a
    // millisecond or more rounds up, and any non-zero nanos round up when there are no whole
    // millis to sleep (so `sleep(0, 1)` still yields rather than returning instantly).
    public static void sleep(long millis, int nanos) {
        if (millis < 0) {
            throw new IllegalArgumentException();
        }
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException();
        }
        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }
        sleep(millis);
    }

    // A hint that the caller is spinning on a value another thread will publish. On HotSpot this
    // lowers to the PAUSE instruction; here it is a genuine no-op — our scheduler already
    // considers a context switch at every opcode, so a spinning thread cannot starve the others.
    public static void onSpinWait() {
    }

    // A hint that the current thread will give up the CPU. VM-intercepted: our scheduler
    // already switches at every opcode, so this is essentially a no-op — but it's part of
    // the Thread API and a common busy-wait idiom.
    public static native void yield();

    // The thread's current lifecycle state. VM-intercepted: the authoritative state lives
    // in the scheduler (Rust), and the VM translates it into one of the constants below.
    // A Thread that was created but never started has no scheduler slot → NEW.
    public native State getState();

    // This thread's group. The main thread's Thread object is fabricated by the VM without a
    // constructor, so it is the one thread that joins a group lazily, here.
    public final ThreadGroup getThreadGroup() {
        if (group == null) {
            group = ThreadGroup.root();
            group.add(this);
        }
        return group;
    }

    // Where an exception that escapes run() is delivered. Installing one replaces the VM's default
    // report (`Exception in thread "..."` + the trace) for THIS thread only.
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        this.uncaughtExceptionHandler = eh;
    }

    // This thread's handler, falling back to the process-wide default — the same order the VM
    // itself resolves in, so what you read here is what would actually run.
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        if (uncaughtExceptionHandler != null) {
            return uncaughtExceptionHandler;
        }
        return defaultUncaughtExceptionHandler;
    }

    // The fallback for every thread that has no handler of its own.
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        defaultUncaughtExceptionHandler = eh;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    // The callback for an exception that was never caught. The VM invokes it *on the dying
    // thread*, after the stack has unwound: the thread is finished either way, so this is a last
    // word (log it, record it, signal a supervisor), not a chance to resume. A handler that
    // throws is ignored and the VM prints its own report instead — a broken handler must not
    // silence the failure it was supposed to report.
    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread t, Throwable e);
    }

    // The six states a thread can be in (java.lang.Thread.State). This is plain data — six
    // named constants — so it's an ordinary enum; getState() hands back the matching one.
    // The names and order mirror the real JDK.
    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }
}
