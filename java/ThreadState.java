// Exercises Thread.getState(): a thread's lifecycle state, read from the scheduler and
// mapped to the matching Thread.State constant. getState() returning the *same* object as
// the enum constant is the point — a switch/== against Thread.State.X has to match.
public class ThreadState {
    static int run() throws Exception {
        // A thread created but never started has no scheduler slot → NEW.
        Thread t = new Slow();
        if (t.getState() != Thread.State.NEW) return -1;

        t.start();
        // main waits for it; by the time join returns, the worker is done.
        t.join();
        if (t.getState() != Thread.State.TERMINATED) return -2;

        // A second unstarted thread is NEW too (not affected by the first).
        Thread u = new Slow();
        if (u.getState() != Thread.State.NEW) return -3;

        // The constant is a real object with enum behaviour.
        if (Thread.State.RUNNABLE.ordinal() != 1) return -4;
        if (!Thread.State.TERMINATED.name().equals("TERMINATED")) return -5;

        return 42;
    }
}

class Slow extends Thread {
    public void run() {
        int x = 0;
        for (int i = 0; i < 50; i++) x += i;
    }
}
