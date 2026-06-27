// join + sleep: each worker sleeps briefly (other threads run meanwhile), sets its
// value, and ends. `main` join()s both — no spin-wait — so a+b is guaranteed 30 once
// both have finished. (Classes prefixed Join to avoid .class clashes.)
class JoinWorker extends Thread {
    int id;

    JoinWorker(int id) {
        this.id = id;
    }

    public void run() {
        Thread.sleep(20);
        if (id == 1) {
            Joiner.a = 10;
        } else {
            Joiner.b = 20;
        }
    }
}

public class Joiner {
    static int a = 0;
    static int b = 0;

    static int run() {
        JoinWorker w1 = new JoinWorker(1);
        JoinWorker w2 = new JoinWorker(2);
        w1.start();
        w2.start();
        w1.join();
        w2.join();
        return a + b; // 30, guaranteed once both joined threads finished
    }
}
