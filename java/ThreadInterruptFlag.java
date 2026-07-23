// Exercises the flag half of interruption: interrupt() sets it, isInterrupted() reads it
// without clearing, interrupted() (static) reads and clears. NOT the waking half yet.
public class ThreadInterruptFlag {
    static int run() {
        // A NEW thread can be interrupted; the flag is on the object, not a slot.
        Thread t = new Thread();
        if (t.isInterrupted()) return -1;      // starts clear
        t.interrupt();
        if (!t.isInterrupted()) return -2;      // now set
        if (!t.isInterrupted()) return -3;      // isInterrupted doesn't clear (read twice)

        // interrupted() (static) reads AND clears the *current* thread's flag.
        Thread me = Thread.currentThread();
        if (Thread.interrupted()) return -4;    // main starts clear
        me.interrupt();
        if (!Thread.interrupted()) return -5;   // set → true, and cleared
        if (Thread.interrupted()) return -6;    // now clear again
        if (me.isInterrupted()) return -7;      // and isInterrupted agrees

        return 42;
    }
}
