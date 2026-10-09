package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Date;
import java.util.TimerTask;

// A scheduler of deferred tasks: a background thread that runs TimerTasks, once or every so often.
//
// The design is three pieces and none of them makes sense alone:
//
//   1. A **priority queue** (`TimerQueue`) ordered by execution time. It is a binary heap and not a
//      sorted list because what is done all the time is "look at the next one" and "reinsert the one
//      that has just run" -- O(1) and O(log n) against O(n) for inserting in order.
//   2. A **thread** (`TimerThread`) that sleeps until the first one's time and runs it.
//   3. The **queue's monitor**, which synchronises the two: adding a task wakes the thread in case
//      the new one goes before the one it was waiting for.
//
// A single thread for every task, and that is contract, not a shortcut: a task that takes long holds
// up the ones behind it. It is the reason `ScheduledThreadPoolExecutor` exists.
//
// About the two ways of repeating, which is what gets confused most:
//
//   schedule(...)             **fixed delay**: the next is counted from when the previous FINISHED.
//                             If one run falls behind, the following ones all still run.
//   scheduleAtFixedRate(...)  **fixed rate**: the next is counted from the previous one's THEORETICAL
//                             time. If one falls behind, the following ones come out in a burst to
//                             catch up.
//
// The sign of `TimerTask.period` is the encoding: negative for fixed delay, positive for fixed rate.
// It comes from the JDK and that is why `scheduledExecutionTime` has to branch on the sign.
//
// **A deliberate divergence**: the `isDaemon` argument is stored and not used. Our `Thread` has no
// `setDaemon`, so the timer's thread is always a normal one; `cancel()` has to be called for it to
// end. In the JDK a daemon timer does not stop the VM from exiting.
public class Timer {

    // To give each thread a different name when one is not given.
    private static int serial = 0;

    private final TimerQueue queue = new TimerQueue();
    private final TimerThread thread;

    public Timer() {
        this(defaultName(), false);
    }

    public Timer(boolean isDaemon) {
        this(defaultName(), isDaemon);
    }

    public Timer(String name) {
        this(name, false);
    }

    public Timer(String name, boolean isDaemon) {
        this.thread = new TimerThread(this.queue);
        this.thread.setName(name);
        this.thread.start();
    }

    private static String defaultName() {
        synchronized (Timer.class) {
            serial = serial + 1;
            return "Timer-" + serial;
        }
    }

    // ---- scheduling -----------------------------------------------------------------------------

    // Once, in `delay` milliseconds' time.
    public void schedule(TimerTask task, long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        this.sched(task, System.currentTimeMillis() + delay, 0);
    }

    // Once, at the given time.
    public void schedule(TimerTask task, Date time) {
        this.sched(task, time.getTime(), 0);
    }

    // Repeated at a **fixed delay**: the period is counted from the end of the previous run.
    public void schedule(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, System.currentTimeMillis() + delay, -period);
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, firstTime.getTime(), -period);
    }

    // Repeated at a **fixed rate**: the period is counted from the previous run's theoretical time,
    // so a delay is made up for afterwards.
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, System.currentTimeMillis() + delay, period);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, firstTime.getTime(), period);
    }

    // What the six have in common. The two monitors are taken in this order --the queue first, then
    // the task-- and always in the same one, which is the only thing that avoids the deadlock against
    // `TimerTask.cancel`.
    private void sched(TimerTask task, long time, long period) {
        if (time < 0) {
            throw new IllegalArgumentException("Illegal execution time.");
        }
        synchronized (this.queue) {
            if (!this.thread.newTasksMayBeScheduled) {
                throw new IllegalStateException("Timer already cancelled.");
            }
            synchronized (task.lock) {
                if (task.state != TimerTask.VIRGIN) {
                    throw new IllegalStateException(
                            "Task already scheduled or cancelled");
                }
                task.nextExecutionTime = time;
                task.period = period;
                task.state = TimerTask.SCHEDULED;
            }
            this.queue.add(task);
            // If the one that has just come in is the first, the thread was sleeping until another
            // time.
            if (this.queue.getMin() == task) {
                this.queue.notify();
            }
        }
    }

    // ---- terminating ----------------------------------------------------------------------------

    // It discards what is pending and lets the thread end. A task already running is not
    // interrupted: cancelling is not killing.
    public void cancel() {
        synchronized (this.queue) {
            this.thread.newTasksMayBeScheduled = false;
            this.queue.clear();
            this.queue.notify();
        }
    }

    // It takes the already cancelled tasks out of the queue and returns how many it took out.
    //
    // It exists for a concrete reason: a cancelled task stays in the queue until its turn comes, so a
    // program that cancels many and adds many more piles up live rubbish. This method is the valve,
    // and it is not called on its own.
    public int purge() {
        int removedCount = 0;
        synchronized (this.queue) {
            int i = this.queue.size();
            while (i > 0) {
                TimerTask t = this.queue.get(i);
                boolean cancelled;
                synchronized (t.lock) {
                    cancelled = t.state == TimerTask.CANCELLED;
                }
                if (cancelled) {
                    this.queue.removeAt2(i);
                    removedCount = removedCount + 1;
                }
                i = i - 1;
            }
            if (removedCount > 0) {
                this.queue.reorder();
            }
        }
        return removedCount;
    }
}

// The binary heap of tasks, ordered by `nextExecutionTime`. Package-private.
//
// It is indexed from 1 and not from 0 on purpose: with base 1 `i`'s children are `2i` and `2i+1` and
// the parent is `i/2`, with no additions or subtractions. It is the classic convention and the
// reason slot 0 is left unused.
//
// It synchronises **nothing**: whoever uses it takes its monitor from outside. That way the Timer can
// do several operations under a single lock.
final class TimerQueue {

    private TimerTask[] queue = new TimerTask[128];
    private int size = 0;

    int size() {
        return this.size;
    }

    void add(TimerTask task) {
        if (this.size + 1 == this.queue.length) {
            TimerTask[] bigger = new TimerTask[2 * this.queue.length];
            System.arraycopy(this.queue, 0, bigger, 0, this.queue.length);
            this.queue = bigger;
        }
        this.size = this.size + 1;
        this.queue[this.size] = task;
        this.siftUpFrom(this.size);
    }

    // The task that goes first. Without taking it out.
    TimerTask getMin() {
        return this.queue[1];
    }

    TimerTask get(int i) {
        return this.queue[i];
    }

    void removeMin() {
        this.queue[1] = this.queue[this.size];
        this.queue[this.size] = null;
        this.size = this.size - 1;
        this.siftDownFrom(1);
    }

    // It takes out position `i`. It leaves the heap **unordered**: `purge` does many in a row and
    // reorders once at the end.
    void removeAt2(int i) {
        this.queue[i] = this.queue[this.size];
        this.queue[this.size] = null;
        this.size = this.size - 1;
    }

    // It changes the first one's time and moves it. It is what a repeated task does on going back
    // into the queue without leaving it.
    void rescheduleMin(long newTime) {
        this.queue[1].nextExecutionTime = newTime;
        this.siftDownFrom(1);
    }

    boolean isEmpty() {
        return this.size == 0;
    }

    void clear() {
        int i = 1;
        while (i <= this.size) {
            this.queue[i] = null;
            i = i + 1;
        }
        this.size = 0;
    }

    // It rebuilds the whole heap, from the bottom up. It is O(n) -- cheaper than n insertions.
    void reorder() {
        int i = this.size / 2;
        while (i >= 1) {
            this.siftDownFrom(i);
            i = i - 1;
        }
    }

    private void siftUpFrom(int k) {
        while (k > 1) {
            int parent = k / 2;
            if (this.queue[parent].nextExecutionTime <= this.queue[k].nextExecutionTime) {
                return;
            }
            this.exchange(k, parent);
            k = parent;
        }
    }

    private void siftDownFrom(int k) {
        while (2 * k <= this.size) {
            int child = 2 * k;
            if (child < this.size
                    && this.queue[child + 1].nextExecutionTime
                            < this.queue[child].nextExecutionTime) {
                child = child + 1;
            }
            if (this.queue[k].nextExecutionTime <= this.queue[child].nextExecutionTime) {
                return;
            }
            this.exchange(k, child);
            k = child;
        }
    }

    private void exchange(int a, int b) {
        TimerTask t = this.queue[a];
        this.queue[a] = this.queue[b];
        this.queue[b] = t;
    }
}

// The thread that runs the tasks. Package-private.
//
// The loop has a shape worth reading slowly, because every detail is there for something:
//
//   - It waits on the **queue's** monitor, not on its own: that way `schedule` can wake it by adding
//     a task that goes before the one it was waiting for.
//   - The task is run **outside** the `synchronized`. Running it inside would block anyone wanting to
//     schedule another while it lasts, and a task can take as long as it likes.
//   - A repeated task is rescheduled **before** running, not after. Were it done after, a task that
//     throws would never go back into the queue.
final class TimerThread extends Thread {

    // It is set to false on cancelling. The loop reads it under the queue's monitor.
    boolean newTasksMayBeScheduled = true;

    private final TimerQueue queue;

    TimerThread(TimerQueue queue) {
        this.queue = queue;
    }

    public void run() {
        this.loop();
        // On leaving, the queue is left empty and the thread dead. A cancelled Timer does not come
        // back.
        synchronized (this.queue) {
            this.newTasksMayBeScheduled = false;
            this.queue.clear();
        }
    }

    private void loop() {
        while (true) {
            TimerTask task;
            boolean due;
            synchronized (this.queue) {
                // Wait while there is nothing and tasks can still be added.
                while (this.queue.isEmpty() && this.newTasksMayBeScheduled) {
                    this.awaitOnQueue(0);
                }
                if (this.queue.isEmpty()) {
                    return; // cancelled and with nothing pending
                }
                task = this.queue.getMin();
                long now;
                long whenDue;
                synchronized (task.lock) {
                    if (task.state == TimerTask.CANCELLED) {
                        this.queue.removeMin();
                        continue;
                    }
                    now = System.currentTimeMillis();
                    whenDue = task.nextExecutionTime;
                    due = whenDue <= now;
                    if (due) {
                        if (task.period == 0) {
                            this.queue.removeMin();
                            task.state = TimerTask.EXECUTED;
                        } else if (task.period < 0) {
                            // Fixed delay: counted from NOW.
                            this.queue.rescheduleMin(now - task.period);
                        } else {
                            // Fixed rate: counted from the theoretical time.
                            this.queue.rescheduleMin(whenDue + task.period);
                        }
                    }
                }
                if (!due) {
                    this.awaitOnQueue(whenDue - now);
                }
            }
            if (due) {
                task.run();
            }
        }
    }

    // It waits on the queue's monitor, swallowing the interruption.
    //
    // It swallows it because the loop looks at the queue again anyway: a spurious interruption cannot
    // make a task run early nor be skipped.
    private void awaitOnQueue(long millis) {
        try {
            this.queue.wait(millis);
        } catch (InterruptedException e) {
            // back to the loop
        }
    }
}
