// A worker sleeps; main interrupts it; the worker catches InterruptedException and records
// it. Also checks that the throw *clears* the interrupt flag (JLS).
public class InterruptSleep {
    static volatile int stage;

    static int run() throws Exception {
        Sleeper w = new Sleeper();
        w.start();

        // Let the worker reach its sleep, then interrupt it.
        while (stage < 1) { Thread.yield(); }
        w.interrupt();
        w.join();

        // stage 2 = the worker caught InterruptedException out of sleep().
        return stage == 2 ? 42 : -stage;
    }
}

class Sleeper extends Thread {
    public void run() {
        try {
            InterruptSleep.stage = 1;
            Thread.sleep(100000);       // long sleep; the interrupt cuts it short
            InterruptSleep.stage = -99; // must NOT reach here
        } catch (InterruptedException e) {
            // The throw cleared the flag: isInterrupted() is false in the handler.
            InterruptSleep.stage = isInterrupted() ? -50 : 2;
        }
    }
}
