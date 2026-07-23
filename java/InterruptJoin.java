// A worker joins on a long-running target; main interrupts the joiner, which catches
// InterruptedException out of join().
public class InterruptJoin {
    static volatile int stage;

    static int run() throws Exception {
        IjJoiner j = new IjJoiner();
        j.start();
        while (stage < 1) { Thread.yield(); }   // wait until j is inside join()
        j.interrupt();
        j.join();
        return stage == 2 ? 42 : -stage;
    }
}

class Spinner extends Thread {
    public void run() {
        // long-lived so the joiner is genuinely blocked when interrupted
        int x = 0;
        for (int i = 0; i < 2000000; i++) x += i;
    }
}

class IjJoiner extends Thread {
    public void run() {
        Spinner s = new Spinner();
        s.start();
        try {
            InterruptJoin.stage = 1;
            s.join();                 // blocks; the interrupt cuts it short
            InterruptJoin.stage = -99;
        } catch (InterruptedException e) {
            InterruptJoin.stage = 2;
        }
    }
}
