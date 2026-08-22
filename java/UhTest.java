// A7 #12: the remaining Thread peripherals. The headline is setUncaughtExceptionHandler — an
// exception that escapes run() must be delivered to the handler *in Java* (the VM's console report
// is only the fallback), on the dying thread, after its stack has unwound. The worker throws a
// UhBoom nobody catches; the handler records what it saw and main checks it afterwards, so a
// handler that never ran, ran twice, or got the wrong Thread/Throwable all score differently.
// Also exercises sleep(long, int) and onSpinWait(), which only have to not blow up.
// Deterministic → green = os-gil = os = 42.
class UhBoom extends RuntimeException {
    UhBoom(String message) {
        super(message);
    }
}

class UhCatcher implements Thread.UncaughtExceptionHandler {
    static int calls;
    static Thread seenThread;
    static Throwable seenThrowable;
    static String seenMessage;

    public void uncaughtException(Thread t, Throwable e) {
        calls = calls + 1;
        seenThread = t;
        seenThrowable = e;
        seenMessage = e.getMessage();
    }
}

class UhBomb implements Runnable {
    public void run() {
        throw new UhBoom("uh-oh"); // never caught: the handler is the only thing that sees it
    }
}

public class UhTest {
    static int run() {
        Thread t = new Thread(new UhBomb());
        t.setName("uh-worker");
        t.setUncaughtExceptionHandler(new UhCatcher());
        // Groups: a new thread joins its creator's group. Read *before* start(), because a real
        // JDK hands back null once the thread has died.
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        boolean grouped = group == t.getThreadGroup() && "main".equals(group.getName()) && group.activeCount() >= 1;
        t.start();
        try {
            t.join(); // the worker still terminates normally after the handler runs
        } catch (InterruptedException e) {
        }

        int score = 0;
        if (UhCatcher.calls == 1) {
            score += 10; // the handler ran, exactly once
        }
        if (UhCatcher.seenThread == t) {
            score += 8; // ...with the dying thread, not some other one
        }
        if (UhCatcher.seenThrowable instanceof UhBoom) {
            score += 8; // ...and the very throwable that escaped run()
        }
        if ("uh-oh".equals(UhCatcher.seenMessage)) {
            score += 6; // ...still carrying its detail message
        }
        if (t.getState() == Thread.State.TERMINATED) {
            score += 4; // handling it does not keep the thread alive
        }

        if (grouped) {
            score += 3; // same group as its creator, named "main", counting at least main itself
        }

        // The two no-drama additions: nanosecond sleep (rounded to our millisecond clock) and the
        // spin hint. They only have to return without faulting.
        try {
            Thread.sleep(0, 500000);
        } catch (InterruptedException e) {
        }
        Thread.onSpinWait();
        score += 3;
        return score; // 42
    }
}
