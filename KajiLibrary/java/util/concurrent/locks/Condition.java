package java.util.concurrent.locks;

// A condition variable factored out of a {@link Lock} — the {@code Object.wait}/{@code
// notify} of the explicit-lock world. A thread {@code await}s a condition (atomically
// releasing the lock and parking); another thread {@code signal}s it (waking a waiter,
// which re-acquires the lock before returning). The caller must hold the owning lock.
//
// This header used to say the timed forms ({@code awaitNanos}, {@code await(long,TimeUnit)}) and
// {@code awaitUntil(Date)} were intentionally omitted. They are not: all three are declared below.
// The two things that blocked them are gone -- the implementations carry a per-waiter signalled
// flag, so a signal is told from a deadline, and {@code java.util.Date} exists. All seven methods
// are here.
public interface Condition {

    // Release the lock and wait until signalled, then re-acquire the lock.
    void await() throws InterruptedException;

    // Like {@link #await}, but not responsive to interruption. The distinction is real here: our
    // {@code Object.wait()} does throw {@code InterruptedException}, so {@link #await} declares it
    // and this one swallows the interruption and re-marks the flag before returning.
    void awaitUninterruptibly();

    // Wake one thread waiting on this condition.
    /**
     * It waits until it is signalled or `time` passes.
     *
     * @return `false` if the deadline ran out before the signal
     */
    boolean await(long time, java.util.concurrent.TimeUnit unit)
            throws InterruptedException;

    /**
     * It waits until it is signalled or `nanosTimeout` nanoseconds pass.
     *
     * <p>It returns **what was left of the deadline**, and that detail is what makes it useful in a
     * loop: a wait can wake with no signal --a *spurious wakeup*-- and has to wait again, but
     * only for the rest. With a `boolean` it cannot be done, because how much has passed is not
     * known.
     *
     * @return the nanoseconds left over; zero or less if the deadline ran out
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * It waits until it is signalled or `deadline` arrives.
     *
     * @return `false` if the date arrived before the signal
     */
    boolean awaitUntil(java.util.Date deadline) throws InterruptedException;

    void signal();

    // Wake all threads waiting on this condition.
    void signalAll();
}
