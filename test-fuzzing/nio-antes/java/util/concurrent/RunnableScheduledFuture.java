package java.util.concurrent;

// The scheduler's own view of a scheduled task: runnable (the worker executes it),
// a future (the submitter waits on it), delayed (the queue orders it) — plus the one bit
// only the scheduler cares about, whether the task repeats.
//
// {@link #isPeriodic} is the whole reason this type exists apart from
// {@link ScheduledFuture}: after running a task the executor must decide between discarding
// it and re-queuing it with a fresh deadline, and that decision belongs to the task.
public interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {

    // True if this task repeats on a period rather than running once.
    boolean isPeriodic();
}
