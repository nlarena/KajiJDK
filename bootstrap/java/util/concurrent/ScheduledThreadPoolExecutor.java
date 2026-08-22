package java.util.concurrent;

// Minimal java.util.concurrent.ScheduledThreadPoolExecutor — runs tasks after a delay
// (`schedule`) or periodically (`scheduleAtFixedRate`). Tasks live in a **binary min-heap ordered
// by deadline** (`System.nanoTime` scale); a single worker thread pulls the earliest, waits (a timed
// monitor `wait`, so a newly-scheduled earlier task or `shutdown` wakes it early) until it is due,
// runs it, and — if periodic — recomputes its next deadline and re-heaps it. All heap/state access
// is under this object's monitor. (Simplified vs. the JDK: one worker (single-threaded), no
// `ScheduledFuture`/cancel, no scheduleWithFixedDelay; `shutdown` stops the worker — schedule your
// waits so pending one-shots have already run, as the tests do via a latch.)
public class ScheduledThreadPoolExecutor {
    private static final long MS_TO_NANOS = 1000000L;

    private static final class Task {
        final Runnable command;
        long time;         // absolute deadline, nanoTime scale
        final long period; // 0 = one-shot; > 0 = fixed-rate period in nanos

        Task(Runnable command, long time, long period) {
            this.command = command;
            this.time = time;
            this.period = period;
        }
    }

    private Task[] heap = new Task[16];
    private int size;
    private boolean shutdown;
    private final Thread worker;

    // corePoolSize is accepted for API familiarity; this minimal version always runs one worker.
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        this.worker = new Worker(this);
        this.worker.start();
    }

    public void schedule(Runnable command, long delayMs) {
        long t = System.nanoTime() + delayMs * MS_TO_NANOS;
        offer(new Task(command, t, 0));
    }

    public void scheduleAtFixedRate(Runnable command, long initialDelayMs, long periodMs) {
        long t = System.nanoTime() + initialDelayMs * MS_TO_NANOS;
        offer(new Task(command, t, periodMs * MS_TO_NANOS));
    }

    public synchronized void shutdown() {
        shutdown = true;
        notifyAll(); // wake the worker so it sees the flag and exits
    }

    public void awaitTermination() throws InterruptedException {
        worker.join();
    }

    private synchronized void offer(Task task) {
        offerLocked(task);
        notifyAll(); // a possibly-earlier task arrived — re-evaluate the worker's wait
    }

    // Blocking dequeue for the worker: return the earliest task once it is due, or null on shutdown.
    private synchronized Task nextDue() {
        for (;;) {
            if (shutdown) {
                return null;
            }
            if (size == 0) {
                waitQuietly(0); // 0 = wait until notified (a schedule or shutdown)
                continue;
            }
            long delayNs = heap[0].time - System.nanoTime();
            if (delayNs <= 0) {
                return pollLocked();
            }
            long ms = delayNs / MS_TO_NANOS;
            waitQuietly(ms <= 0 ? 1 : ms); // sub-millisecond delay → wait the 1 ms floor
        }
    }

    private synchronized void reschedule(Task task) {
        if (!shutdown) {
            task.time = System.nanoTime() + task.period;
            offerLocked(task);
        }
    }

    private void waitQuietly(long ms) {
        try {
            wait(ms);
        } catch (InterruptedException e) {
        }
    }

    // ---- binary min-heap keyed by Task.time (all callers hold the monitor) ----

    private void offerLocked(Task task) {
        if (size == heap.length) {
            Task[] bigger = new Task[heap.length * 2];
            for (int i = 0; i < size; i++) {
                bigger[i] = heap[i];
            }
            heap = bigger;
        }
        heap[size] = task;
        siftUp(size);
        size = size + 1;
    }

    private Task pollLocked() {
        Task top = heap[0];
        size = size - 1;
        heap[0] = heap[size];
        heap[size] = null;
        siftDown(0);
        return top;
    }

    private void siftUp(int k) {
        while (k > 0) {
            int parent = (k - 1) / 2;
            if (heap[k].time >= heap[parent].time) {
                break;
            }
            swap(k, parent);
            k = parent;
        }
    }

    private void siftDown(int k) {
        for (;;) {
            int left = 2 * k + 1;
            int right = 2 * k + 2;
            int smallest = k;
            if (left < size && heap[left].time < heap[smallest].time) {
                smallest = left;
            }
            if (right < size && heap[right].time < heap[smallest].time) {
                smallest = right;
            }
            if (smallest == k) {
                break;
            }
            swap(k, smallest);
            k = smallest;
        }
    }

    private void swap(int i, int j) {
        Task t = heap[i];
        heap[i] = heap[j];
        heap[j] = t;
    }

    private static final class Worker extends Thread {
        private final ScheduledThreadPoolExecutor stpe;

        Worker(ScheduledThreadPoolExecutor stpe) {
            this.stpe = stpe;
        }

        public void run() {
            for (;;) {
                Task task = stpe.nextDue();
                if (task == null) {
                    return; // shut down
                }
                task.command.run();
                if (task.period > 0) {
                    stpe.reschedule(task);
                }
            }
        }
    }
}
