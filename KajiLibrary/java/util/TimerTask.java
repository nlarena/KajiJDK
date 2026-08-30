package java.util;

// A unit of work a Timer runs, once or repeatedly. Abstract because the work itself is the one
// thing this cannot supply: a subclass writes run().
//
// The package-private fields below are not decoration — they are the protocol between a task and
// the Timer that owns it. The Timer reads and writes `state`, `nextExecutionTime` and `period`
// directly while holding `lock`, which is why they live here rather than behind accessors.
public abstract class TimerTask implements Runnable {

    // The monitor guarding this task's mutable state. A task and its Timer synchronize on this
    // same object, so a cancel() racing the timer thread resolves one way or the other rather
    // than tearing.
    final Object lock = new Object();

    // Where the task is in its life cycle: one of the four constants below.
    int state = VIRGIN;

    // Created, never scheduled.
    static final int VIRGIN = 0;

    // Scheduled; may or may not have run yet. A repeating task stays here for its whole life.
    static final int SCHEDULED = 1;

    // A non-repeating task that already ran (or is running). A repeating task never reaches this.
    static final int EXECUTED = 2;

    // Cancelled, by cancel() or by its Timer being cancelled.
    static final int CANCELLED = 3;

    // The next time this should run, in Timer's own time base (millis).
    long nextExecutionTime;

    // Repetition: 0 for a one-shot, positive for fixed-rate, negative for fixed-delay. The sign
    // is the encoding, which is why scheduledExecutionTime has to branch on it.
    long period = 0;

    // A new task, not yet scheduled. Protected: instantiating a bare TimerTask would give you
    // something with no run() to call.
    protected TimerTask() {
    }

    // The work. Called by the timer thread.
    public abstract void run();

    // Cancels this task, and reports whether it would otherwise have run again.
    //
    // The return value is deliberately narrow: true only if the task was scheduled and had not
    // yet run to completion as a one-shot. Cancelling twice returns false the second time, and
    // cancelling a task that was never scheduled returns false — in both cases nothing was
    // prevented, which is exactly what the boolean means.
    public boolean cancel() {
        synchronized (this.lock) {
            boolean result = this.state == SCHEDULED;
            this.state = CANCELLED;
            return result;
        }
    }

    // The scheduled time of this task's most recent actual execution.
    //
    // Meant to be called from inside run(), where it answers "when was I supposed to run?" — the
    // gap against System.currentTimeMillis() is how a fixed-rate task notices it is running late
    // and can decide to skip work rather than pile up. Called outside run(), or before the first
    // execution, the value is meaningless.
    public long scheduledExecutionTime() {
        synchronized (this.lock) {
            if (this.period < 0) {
                return this.nextExecutionTime + this.period;
            }
            return this.nextExecutionTime - this.period;
        }
    }
}
