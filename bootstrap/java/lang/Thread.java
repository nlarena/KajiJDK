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

    public Thread() {
        this.tid = nextThreadNum();
        this.name = "Thread-" + tid;
    }

    // The common non-subclass form: the thread runs `target.run()`. The target is usually
    // a lambda — which works because a lambda is just an object implementing Runnable.
    public Thread(Runnable target) {
        this.tid = nextThreadNum();
        this.name = "Thread-" + tid;
        this.target = target;
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

    // A hint that the current thread will give up the CPU. VM-intercepted: our scheduler
    // already switches at every opcode, so this is essentially a no-op — but it's part of
    // the Thread API and a common busy-wait idiom.
    public static native void yield();

    // The thread's current lifecycle state. VM-intercepted: the authoritative state lives
    // in the scheduler (Rust), and the VM translates it into one of the constants below.
    // A Thread that was created but never started has no scheduler slot → NEW.
    public native State getState();

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
