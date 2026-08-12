package java.util.concurrent;

// Minimal fixed-size java.util.concurrent.ThreadPoolExecutor: `nThreads` worker threads pull
// Runnables from a bounded `ArrayBlockingQueue` and run them, so N tasks run on a fixed set of
// threads instead of one-thread-per-task. `execute` enqueues a task (blocking if the queue is full);
// `submit` wraps it in a `FutureTask` and hands back the `Future`; `shutdown` enqueues one poison
// pill per worker — FIFO, so every real task is taken before any pill — and each worker exits on
// taking one; `awaitTermination` joins the workers. (Simplified vs. the JDK: fixed size only, no
// core/max/keep-alive, no `RejectedExecutionHandler`, no `shutdownNow`.)
public class ThreadPoolExecutor implements ExecutorService {
    // A unique sentinel: a worker that dequeues *this* exits. Identity-compared, never run.
    private static final Runnable POISON = new Poison();

    private final ArrayBlockingQueue<Runnable> queue;
    private final Worker[] workers;

    public ThreadPoolExecutor(int nThreads) {
        this.queue = new ArrayBlockingQueue<Runnable>(1024);
        this.workers = new Worker[nThreads];
        for (int i = 0; i < nThreads; i++) {
            this.workers[i] = new Worker(this.queue);
            this.workers[i].start();
        }
    }

    public void execute(Runnable command) {
        try {
            queue.put(command);
        } catch (InterruptedException e) {
        }
    }

    public Future<?> submit(Runnable task) {
        FutureTask<Object> ft = new FutureTask<Object>(task, null);
        execute(ft);
        return ft;
    }

    public void shutdown() {
        // One pill per worker; FIFO guarantees the already-queued tasks run first.
        for (int i = 0; i < workers.length; i++) {
            try {
                queue.put(POISON);
            } catch (InterruptedException e) {
            }
        }
    }

    public void awaitTermination() throws InterruptedException {
        for (int i = 0; i < workers.length; i++) {
            workers[i].join();
        }
    }

    private static final class Poison implements Runnable {
        public void run() {
        }
    }

    private static final class Worker extends Thread {
        private final ArrayBlockingQueue<Runnable> queue;

        Worker(ArrayBlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        public void run() {
            for (;;) {
                Runnable task;
                try {
                    task = queue.take();
                } catch (InterruptedException e) {
                    return;
                }
                if (task == POISON) {
                    return;
                }
                task.run();
            }
        }
    }
}
