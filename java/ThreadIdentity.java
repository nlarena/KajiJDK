// Exercises the identity wiring: currentThread() (including from main, which gets a
// fabricated Thread object), getName()/setName(), and threadId() assigned at construction.
public class ThreadIdentity {
    static int run() throws Exception {
        // currentThread() from main: a real Thread object named "main".
        Thread me = Thread.currentThread();
        if (me == null) return -1;
        if (!me.getName().equals("main")) return -2;

        // ...and it's stable: the same object each call (identity, via the GC root).
        if (Thread.currentThread() != me) return -3;

        // A spawned thread has a default name and a distinct id.
        Thread t = new Thread();
        if (!t.getName().startsWith("Thread-")) return -4;
        if (t.threadId() == me.threadId()) return -5; // different threads, different ids

        // setName sticks.
        t.setName("worker");
        if (!t.getName().equals("worker")) return -6;


        return 42;
    }
}
