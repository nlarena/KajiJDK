/** Do real threads work on our VM: start, run, join, and a result handed back. */
public class ThreadProbe {

    public static int corre() {
        Bump task = new Bump();
        Thread t = new Thread(task);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            return -1;
        }
        return task.value();
    }

    public static int varios() {
        Bump a = new Bump();
        Bump b = new Bump();
        Thread ta = new Thread(a);
        Thread tb = new Thread(b);
        ta.start();
        tb.start();
        try {
            ta.join();
            tb.join();
        } catch (InterruptedException e) {
            return -1;
        }
        return a.value() + b.value();
    }
}

final class Bump implements Runnable {
    private int n;

    Bump() {
        this.n = 0;
    }

    public void run() {
        this.n = 21;
    }

    int value() {
        return this.n;
    }
}
