package java.util.concurrent.locks;

// The {@link Condition} returned by {@link ReentrantLock#newCondition}. A top-level
// package-private class (not an inner class) that holds its owning lock explicitly —
// avoiding enclosing-instance capture, which the compiler does not yet generate reliably.
//
// The await protocol is lost-wakeup-free: the waiter enters the condition monitor
// (`cvar`) BEFORE releasing the lock, so a signaller — which must take `cvar` to notify —
// cannot slip its signal in between the release and the park. `cvar.wait()` then atomically
// releases `cvar` and parks; the signal can only land while the waiter is parked.
class ReentrantCondition implements Condition {

    private final ReentrantLock lock;
    private final Object cvar = new Object();
    // Those waiting on this condition. The list is kept and not a counter because
    // `getWaitingThreads` asks for the threads; the count would come out of it anyway.
    private final java.util.ArrayList<Thread> waiting = new java.util.ArrayList<Thread>();

    ReentrantCondition(ReentrantLock lock) {
        this.lock = lock;
    }

    // It declares `throws InterruptedException`, as the JDK does. The note that used to be here
    // said it did not, to dodge #104 --the frozen javac's class reader ignored the `Exceptions`
    // attribute of a classpath method, so an identical `throws` read as **wider** and was rejected.
    // That finding is closed, and along the way `Object.wait()` came to be seen as what it is: an
    // **interruptible** wait. Swallowing that interruption would take from the caller the only way
    // of getting a thread out of a wait.
    public void await() throws InterruptedException {
        int holds;
        synchronized (cvar) {
            waiting.add(Thread.currentThread());
            holds = lock.fullyRelease();
            cvar.wait();
            waiting.remove(Thread.currentThread());
        }
        lock.reacquire(holds);
    }

    /**
     * It waits **without** being interruptible.
     *
     * <p>The interruption is not lost: it is caught, the wait goes on, and at the end the thread is
     * marked interrupted again. It is the difference between "it does not interrupt me" and "I
     * swallow the interruption" -- the second leaves the thread not knowing somebody asked it to
     * stop.
     */
    public void awaitUninterruptibly() {
        int holds;
        boolean wasInterrupted = false;
        synchronized (cvar) {
            waiting.add(Thread.currentThread());
            holds = lock.fullyRelease();
            boolean done = false;
            while (!done) {
                try {
                    cvar.wait();
                    done = true;
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            waiting.remove(Thread.currentThread());
        }
        lock.reacquire(holds);
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * It waits with a deadline, in nanoseconds, and returns **what was left over**.
     *
     * <p>The deadline is measured with `System.nanoTime()` and not with the wall clock: it is the
     * only one that does not jump if somebody changes the system time, and a wait that shortens or
     * lengthens because the clock was moved is a very hard error to find.
     */
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        int holds;
        long leftOver;
        synchronized (cvar) {
            long startedAt = System.nanoTime();
            waiting.add(Thread.currentThread());
            holds = lock.fullyRelease();
            long millis = nanosTimeout / 1000000L;
            int nanos = (int) (nanosTimeout % 1000000L);
            if (millis > 0L || nanos > 0) {
                cvar.wait(millis, nanos);
            }
            waiting.remove(Thread.currentThread());
            leftOver = nanosTimeout - (System.nanoTime() - startedAt);
        }
        lock.reacquire(holds);
        return leftOver;
    }

    /** It waits with a deadline; `false` if the deadline ran out. */
    public boolean await(long time, java.util.concurrent.TimeUnit unit)
            throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return this.awaitNanos(unit.toNanos(time)) > 0L;
    }

    /**
     * It waits until a date.
     *
     * <p>Here the wall clock **is** used, and it has to be: the deadline is expressed as a moment on
     * the calendar, not as a duration. The consequence is the contract's -- if somebody moves the
     * system clock, this wait moves with it.
     */
    public boolean awaitUntil(java.util.Date deadline) throws InterruptedException {
        if (deadline == null) {
            throw new NullPointerException("deadline");
        }
        long remaining = deadline.getTime() - System.currentTimeMillis();
        if (remaining <= 0L) {
            return false;
        }
        this.awaitNanos(remaining * 1000000L);
        return System.currentTimeMillis() < deadline.getTime();
    }

    // ---- what the lock needs in order to answer its inspection queries ---------------------------

    boolean belongsTo(ReentrantLock other) {
        return this.lock == other;
    }

    boolean anyWaiting() {
        boolean any;
        synchronized (cvar) {
            any = !waiting.isEmpty();
        }
        return any;
    }

    int waitingCount() {
        int n;
        synchronized (cvar) {
            n = waiting.size();
        }
        return n;
    }

    java.util.Collection<Thread> theWaiters() {
        java.util.ArrayList<Thread> copy;
        synchronized (cvar) {
            copy = new java.util.ArrayList<Thread>(waiting);
        }
        return copy;
    }

    public void signal() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notify();
        }
    }

    public void signalAll() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notifyAll();
        }
    }
}
