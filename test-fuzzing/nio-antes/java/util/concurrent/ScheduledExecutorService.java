package java.util.concurrent;

// An {@link ExecutorService} that can also run a task *later*, or *repeatedly*. It replaces
// the old Timer/TimerTask pair, and the reason is worth stating: a Timer owns exactly one
// thread, so one slow or throwing task delays or kills every other schedule. Here the
// schedule is just a queue of due-times drained by a pool, so tasks are isolated from each
// other and the pool size is a knob.
//
// The two repeating forms differ in what the period measures. `scheduleAtFixedRate` counts
// from each start, so runs happen every `period` regardless of how long they take (and pile
// up if a run overruns); `scheduleWithFixedDelay` counts from each *end*, so consecutive
// runs are always separated by at least `delay`. Pick fixed-rate when the schedule matters
// (a clock tick), fixed-delay when the gap matters (a polling loop that must not hammer).
//
// Subset: the JDK's schedule/scheduleAtFixedRate overloads taking a java.time.Duration are
// omitted, as are the `close`/`invokeAll` family inherited from ExecutorService.
public interface ScheduledExecutorService extends ExecutorService {

    // Run `command` once, after `delay`.
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    // Run `callable` once, after `delay`; the future carries its result.
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    // Run repeatedly, starting after `initialDelay`, then every `period` from each start.
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    // Run repeatedly, starting after `initialDelay`, with `delay` between end and next start.
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
