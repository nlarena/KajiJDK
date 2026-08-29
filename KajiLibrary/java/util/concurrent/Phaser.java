package java.util.concurrent;

// A reusable barrier whose number of parties is **dynamic**. That one word is the whole
// reason this class exists next to {@link CyclicBarrier}: a CyclicBarrier is built around a
// fixed `parties` count decided in its constructor, so a thread cannot join the rendezvous
// later and a thread that finishes early cannot leave without wedging everyone else. A
// Phaser lets parties {@link #register} and {@link #arriveAndDeregister} between — and even
// during — phases, so the barrier tracks a *changing* population of workers.
//
// The model is a sequence of numbered **phases**. Every registered party arrives at the
// current phase; when the last one arrives the phase *advances*: the phase number goes up by
// one, the arrival count is reset, and everyone waiting is released together. Between the
// last arrival and the release sits {@link #onAdvance}, the hook a subclass overrides to
// decide whether the phaser should instead **terminate** — which is how you write "run
// exactly N rounds, then stop" without a separate flag that every worker has to poll.
//
// Termination is reported in the *sign* of the phase number, as in the JDK: once terminated
// every phase-returning method hands back a negative value, so a worker loop written as
// `while (phaser.arriveAndAwaitAdvance() >= 0)` unwinds by itself. A terminated phaser is
// inert — arriving, registering and deregistering all become no-ops that just report the
// negative phase.
//
// The JDK packs phase, parties and unarrived into one 64-bit state word driven by CAS. Here
// the three are plain ints guarded by one monitor, and a waiter blocks until the phase
// number it recorded is no longer the current one — on a runtime that interleaves threads
// between opcodes the two are observably the same, and the phase-number-as-generation trick
// is the same one {@link CyclicBarrier} uses.
//
// Subset: the tiered (parent/child) phasers are not modelled — {@code getParent}, {@code
// getRoot} and the two Phaser-taking constructors are absent. Tiering exists only to spread
// CAS contention across a tree of phasers, which is a cost we do not pay: a monitor-based
// phaser gets nothing from being split.
//
// Single-exit style throughout (finding #105).
public class Phaser {

    private final Object sync = new Object();
    // The current phase number, always >= 0; the negative values callers see are produced on
    // the way out by terminated(), never stored.
    private int phase;
    // Parties currently registered.
    private int parties;
    // Of those, how many have not yet arrived at the current phase.
    private int unarrived;
    private boolean terminated;

    public Phaser() {
        this(0);
    }

    public Phaser(int parties) {
        if (parties < 0) {
            throw new IllegalArgumentException("parties < 0");
        }
        this.parties = parties;
        this.unarrived = parties;
    }

    // The phase number as a *terminated* phaser reports it: the same number with the sign bit
    // set, hence negative. Written as `1 << 31` rather than `Integer.MIN_VALUE` because
    // reading a static field of another compiled class traps at run time (finding #110).
    private static int terminatedPhase(int p) {
        return p | (1 << 31);
    }

    // The phase to report right now: negative once terminated. Caller holds sync.
    private int currentPhase() {
        int p;
        if (terminated) {
            p = terminatedPhase(phase);
        } else {
            p = phase;
        }
        return p;
    }

    // Add `n` parties to the current phase. They count as *unarrived*, so a party that
    // registers mid-phase must itself arrive before the phase can advance — which is exactly
    // what makes "spawn a helper and have it join this round" work.
    private int doRegister(int n) {
        int p;
        synchronized (sync) {
            if (terminated) {
                p = terminatedPhase(phase);
            } else {
                parties = parties + n;
                unarrived = unarrived + n;
                p = phase;
            }
        }
        return p;
    }

    // Register one party. Returns the phase it is registered for.
    public int register() {
        return doRegister(1);
    }

    // Register `parties` parties at once. Cheaper than a loop of register(), and atomic:
    // the whole group joins the same phase.
    // (The parameter is deliberately not called `parties`: it would shadow the field of that
    // name, and we would rather not find out how the frozen javac resolves the collision.)
    public int bulkRegister(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("parties < 0");
        }
        int p;
        if (n == 0) {
            p = getPhase();
        } else {
            p = doRegister(n);
        }
        return p;
    }

    // The last arrival of a phase runs this: consult the termination hook, then either
    // terminate or open the next phase and release everyone. Caller holds sync.
    //
    // onAdvance is user code running under our monitor, as it is in the JDK — it is meant to
    // be a short decision (`return phase >= 4`), not somewhere to block.
    private void advance() {
        boolean stop = onAdvance(phase, parties);
        if (stop || parties == 0) {
            terminated = true;
        } else {
            phase = phase + 1;
            if (phase < 0) {
                // Wrap rather than go negative: the sign bit is reserved for termination.
                phase = 0;
            }
            unarrived = parties;
        }
        sync.notifyAll();
    }

    // The termination decision, taken once per phase by the party that completes it, just
    // before the others are released. Returning true terminates the phaser.
    //
    // The default terminates exactly when the last party has deregistered — a phaser with no
    // parties has nothing left to synchronize. Override to stop after a fixed number of
    // rounds, or when some computed result has converged.
    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    // Arrive at the current phase **without** waiting for the others. Returns the phase
    // arrived at (negative if terminated). This is the "producer" half of a phaser: a thread
    // that only needs to signal its progress, not to synchronize with it.
    public int arrive() {
        int p;
        synchronized (sync) {
            if (terminated) {
                p = terminatedPhase(phase);
            } else {
                if (unarrived <= 0) {
                    throw new IllegalStateException("Attempted arrival of unregistered party");
                }
                p = phase;
                unarrived = unarrived - 1;
                if (unarrived == 0) {
                    advance();
                }
            }
        }
        return p;
    }

    // Arrive and give up registration in one step, then leave without waiting. Returns the
    // phase arrived at. This is how a worker retires: the parties it leaves behind are one
    // fewer, so the *next* phase completes with one fewer arrival — and if it was the last
    // party, the phaser terminates.
    public int arriveAndDeregister() {
        int p;
        synchronized (sync) {
            if (terminated) {
                p = terminatedPhase(phase);
            } else {
                if (unarrived <= 0) {
                    throw new IllegalStateException("Attempted arrival of unregistered party");
                }
                p = phase;
                parties = parties - 1;
                unarrived = unarrived - 1;
                if (unarrived == 0) {
                    // parties is already decremented, so onAdvance sees the population that
                    // will actually run the next phase.
                    advance();
                }
            }
        }
        return p;
    }

    // Arrive and block until every other registered party has arrived too — the barrier
    // proper. Returns the phase number of the phase that just *started* (negative if the
    // arrival terminated the phaser instead), which is what makes a worker loop of the form
    // `while (phaser.arriveAndAwaitAdvance() >= 0)` terminate on its own.
    public int arriveAndAwaitAdvance() {
        int next;
        synchronized (sync) {
            if (terminated) {
                next = terminatedPhase(phase);
            } else {
                if (unarrived <= 0) {
                    throw new IllegalStateException("Attempted arrival of unregistered party");
                }
                int arrivedAt = phase;
                unarrived = unarrived - 1;
                if (unarrived == 0) {
                    advance();
                } else {
                    // The phase number *is* the generation: block until it moves on, or until
                    // someone terminates the phaser out from under us.
                    while (phase == arrivedAt && !terminated) {
                        sync.wait();
                    }
                }
                next = currentPhase();
            }
        }
        return next;
    }

    // Block until the phaser leaves the given phase, **without** arriving at it. Useful for
    // an observer that is not one of the parties. Returns the phase that follows; a negative
    // argument (the value a terminated phaser reports) returns immediately.
    public int awaitAdvance(int p) {
        int result;
        synchronized (sync) {
            if (p < 0) {
                result = p;
            } else {
                while (phase == p && !terminated) {
                    sync.wait();
                }
                result = currentPhase();
            }
        }
        return result;
    }

    // Terminate the phaser now, releasing every waiter with a negative phase. Unlike
    // {@link CyclicBarrier#reset} this is not recoverable: termination is permanent, and it
    // is the escape hatch for a worker that fails and would otherwise leave the rest blocked
    // forever waiting for an arrival that will never come.
    public void forceTermination() {
        synchronized (sync) {
            terminated = true;
            sync.notifyAll();
        }
    }

    public boolean isTerminated() {
        boolean t;
        synchronized (sync) {
            t = terminated;
        }
        return t;
    }

    // The current phase, or a negative value once terminated.
    public final int getPhase() {
        int p;
        synchronized (sync) {
            p = currentPhase();
        }
        return p;
    }

    public int getRegisteredParties() {
        int n;
        synchronized (sync) {
            n = parties;
        }
        return n;
    }

    // How many registered parties have already arrived at the current phase.
    public int getArrivedParties() {
        int n;
        synchronized (sync) {
            n = parties - unarrived;
        }
        return n;
    }

    // How many are still to arrive before the phase advances.
    public int getUnarrivedParties() {
        int n;
        synchronized (sync) {
            n = unarrived;
        }
        return n;
    }
}
