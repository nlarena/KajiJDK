package java.lang;

// KajiLibrary's java.lang.Thread. The scheduler operations are `native` — they create, park
// and wake threads on the VM's substrate, which since the concurrency work can be green
// threads on one carrier, OS threads behind a GIL, or truly parallel OS threads. Which one
// is a VM decision; this class is the same Java-side contract either way.
//
// The identity fields (`tid`, `name`) and the interrupt flag live here, in Java, because the
// VM reads them back by *name* (`field_offset`) — `interrupted` in particular is written by
// the scheduler when another thread interrupts this one.
public class Thread {

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

    public Thread() {
        this.tid = nextThreadNum();
        this.name = "Thread-" + tid;
    }

    public Thread(Runnable target) {
        this.tid = nextThreadNum();
        this.name = "Thread-" + tid;
        this.target = target;
    }

    // --- identity ---

    // The next id in the VM's sequence. Private: it's the ctor's helper, not API.
    private static native long nextThreadNum();

    public final long threadId() {
        return tid;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    // The Thread object of the currently running thread. VM-intercepted (it reads scheduler
    // state); the entry/main thread gets a bare Thread lazily on first call.
    public static native Thread currentThread();

    // Whether the current thread owns `o`'s monitor — the check `synchronized` relies on.
    public static native boolean holdsLock(Object o);

    // --- lifecycle ---

    // The VM spawns a new thread that runs this object's `run()` and returns immediately.
    // No bytecode: pure VM op.
    public native void start();

    // The body of the thread. A bare Thread runs its `target` if it was given one; a
    // subclass overrides this instead. Both forms end up here, which is why `start()`
    // only ever has to call `run()`.
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    // Block the current thread until this one terminates. Scheduler op (VM-intercepted).
    public final native void join();

    // Sleep the current thread for `ms`. Under the green scheduler that's ticks of the VM's
    // opcode clock; under the OS substrates it's real wall time. VM-intercepted.
    public static native void sleep(long ms);

    // Offer the scheduler a chance to run someone else. A hint, not a guarantee.
    public static native void yield();

    // Alive = started and not finished. Derived from the authoritative scheduler state
    // rather than a flag of our own, so it can't drift out of sync.
    public final boolean isAlive() {
        State s = getState();
        return s != State.NEW && s != State.TERMINATED;
    }

    // The scheduler's view of this thread. VM-intercepted: it has to initialize `State`
    // first, since the constant objects only exist once `State.<clinit>` has run.
    public native State getState();

    // --- interruption ---

    // Ask the VM to interrupt this thread: sets the flag and, if the thread is parked in
    // sleep/wait/join, wakes it there with an InterruptedException.
    public native void interrupt();

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
}
