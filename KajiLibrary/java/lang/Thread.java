package java.lang;

// KajiLibrary's java.lang.Thread for KajiJDK's green-thread scheduler. The scheduler
// operations are `native` (they create/park/wake green threads on the single carrier);
// `run()` is the overridable entry point a subclass fills in. This is the Java-21 model
// of a virtual thread — user-space scheduling — in miniature.
public class Thread {

    public Thread() {
    }

    // The VM spawns a new green thread that runs this object's `run()` (cooperatively
    // scheduled onto the carrier) and returns immediately. No bytecode: pure VM op.
    public native void start();

    // The body of the thread. A bare Thread does nothing; subclasses override this.
    public void run() {
    }

    // Block the current thread until this one terminates. Scheduler op (VM-intercepted).
    public final native void join();

    // Sleep the current thread for `ms` ticks of the VM's opcode clock. VM-intercepted.
    public static native void sleep(long ms);
}
