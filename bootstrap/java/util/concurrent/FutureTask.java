package java.util.concurrent;

// Minimal java.util.concurrent.FutureTask — a Runnable that IS a Future. `run()` executes the
// wrapped Callable (or Runnable + fixed result), stores the outcome and marks itself done, waking
// anyone blocked in `get()`. Completion is published on the task's own monitor (`wait`/`notifyAll`),
// which also gives `get()` its happens-before with `run()`. (Simplified: a task that throws leaves a
// null result — a full impl would rethrow it from `get()` via `ExecutionException`.)
public class FutureTask<V> implements Runnable, Future<V> {
    private final Callable<V> callable;
    private final Runnable runnable;
    private V result;
    private boolean done;

    public FutureTask(Callable<V> callable) {
        this.callable = callable;
        this.runnable = null;
    }

    public FutureTask(Runnable runnable, V result) {
        this.callable = null;
        this.runnable = runnable;
        this.result = result; // returned as-is once the runnable completes
    }

    public void run() {
        try {
            if (callable != null) {
                V v = callable.call();
                synchronized (this) {
                    result = v;
                }
            } else {
                runnable.run();
            }
        } catch (Exception e) {
            // Minimal: swallow — the result stays null and `done` is still set below, so `get()`
            // returns rather than hangs. A full impl would remember `e` and rethrow from `get()`.
        }
        synchronized (this) {
            done = true;
            notifyAll();
        }
    }

    public synchronized V get() throws InterruptedException {
        while (!done) {
            wait();
        }
        return result;
    }

    public synchronized boolean isDone() {
        return done;
    }
}
