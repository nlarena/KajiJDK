// Exercises the H1 wiring: Thread(Runnable) with a lambda target, isAlive() across the
// lifecycle, and start() twice throwing IllegalThreadStateException.
public class ThreadWiring {
    static int counter;

    static int run() throws Exception {
        // new Thread(runnable): the lambda is the target the thread runs.
        Thread t = new Thread(() -> { counter = 7; });

        if (t.isAlive()) return -1;            // NEW → not alive
        t.start();
        t.join();
        if (t.isAlive()) return -2;            // TERMINATED → not alive
        if (counter != 7) return -3;           // the lambda actually ran

        // Starting an already-started (now terminated) thread is illegal.
        boolean threw = false;
        try {
            t.start();
        } catch (IllegalThreadStateException e) {
            threw = true;
        }
        if (!threw) return -4;

        return 42;
    }
}
