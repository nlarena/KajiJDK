package java.util.concurrent;

// A rendezvous point for a fixed number of threads, reusable once tripped: each party
// calls {@link #await} and blocks until the last one arrives, at which point the optional
// barrier action runs and every party is released together.
//
// Generations are modelled as a counter rather than the JDK's Generation object: a waiter
// records the generation it arrived in and blocks until that number changes. `brokenGen`
// records the generation a {@link #reset} broke, so exactly the parties that were waiting
// then see a {@link BrokenBarrierException} — and the barrier itself is usable again.
//
// Single-exit style throughout (finding #105). The `throws InterruptedException` the JDK
// declares is omitted (KajiJDK has no interruption), but BrokenBarrierException and
// TimeoutException are declared: our javac enforces checked exceptions, and these two are
// genuinely raised.
public class CyclicBarrier {

    private final Object sync = new Object();
    private final int parties;
    // The action run by the last party to arrive, before the others are released.
    private final Runnable barrierCommand;
    // Parties still to arrive in the current generation.
    private int count;
    // The current generation; bumped when the barrier trips or is reset.
    private int generation;
    // The generation a reset broke, or -1 when none has been.
    private int brokenGen = -1;

    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException("parties <= 0");
        }
        this.parties = parties;
        this.barrierCommand = barrierAction;
        this.count = parties;
    }

    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    public int getParties() {
        return parties;
    }

    // Wait until every party has arrived. Returns this thread's arrival index:
    // `getParties() - 1` for the first to arrive, 0 for the last (which is the party
    // that runs the barrier action).
    public int await() throws BrokenBarrierException, InterruptedException {
        int index;
        synchronized (sync) {
            if (brokenGen == generation) {
                throw new BrokenBarrierException();
            }
            int gen = generation;
            count--;
            index = count;
            if (index == 0) {
                trip();
            } else {
                while (generation == gen) {
                    sync.wait();
                }
                if (brokenGen == gen) {
                    throw new BrokenBarrierException();
                }
            }
        }
        return index;
    }

    // Like {@link #await}, but gives up after the timeout — which **breaks** the barrier
    // for everyone still waiting, exactly as the JDK does (a party that never arrives
    // makes the rendezvous unachievable for all).
    public int await(long timeout, TimeUnit unit) throws BrokenBarrierException, TimeoutException, InterruptedException {
        int index;
        synchronized (sync) {
            if (brokenGen == generation) {
                throw new BrokenBarrierException();
            }
            int gen = generation;
            count--;
            index = count;
            if (index == 0) {
                trip();
            } else {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
                if (generation == gen) {
                    // Still the same generation: the barrier never tripped in time.
                    brokenGen = gen;
                    generation++;
                    count = parties;
                    sync.notifyAll();
                    throw new TimeoutException();
                }
                if (brokenGen == gen) {
                    throw new BrokenBarrierException();
                }
            }
        }
        return index;
    }

    // Run the barrier action (if any) and open the barrier for a fresh generation.
    // Called by the last party to arrive, with `sync` held.
    private void trip() {
        Runnable action = barrierCommand;
        if (action != null) {
            action.run();
        }
        generation++;
        count = parties;
        sync.notifyAll();
    }

    public boolean isBroken() {
        boolean b;
        synchronized (sync) {
            b = brokenGen == generation;
        }
        return b;
    }

    // Break the barrier for the parties waiting now (they get a BrokenBarrierException)
    // and return it to its initial, usable state.
    public void reset() {
        synchronized (sync) {
            brokenGen = generation;
            generation++;
            count = parties;
            sync.notifyAll();
        }
    }

    public int getNumberWaiting() {
        int n;
        synchronized (sync) {
            n = parties - count;
        }
        return n;
    }
}
