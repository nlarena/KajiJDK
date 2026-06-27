package java.lang;

// Minimal java.lang.Thread for our green-thread scheduler. `start()` is native: the VM
// spawns a green thread that runs this object's `run()` (cooperatively scheduled onto
// the single OS thread). Subclasses override `run()`; the base `run()` does nothing.
public class Thread {
    public Thread() {
    }

    // The VM intercepts this: it creates a new green thread running `run()` and returns
    // immediately. (Declared native — it has no bytecode.)
    public native void start();

    public void run() {
    }

    // Block until this thread terminates (no timeout). VM-intercepted (scheduler op).
    public final native void join();

    // Sleep the current thread for `ms` ticks of the VM's opcode clock. VM-intercepted.
    public static native void sleep(long ms);
}
