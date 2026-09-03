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
// TIERING. A phaser may name a parent, and then it is one *party* of that parent rather than a
// barrier of its own: the phase number, the termination flag and the waiting all live at the
// ROOT of the tree, and a child only keeps its own party count. When a child's last local party
// arrives, the child arrives at its parent; when a child's first party registers, the child
// registers itself with its parent; when its last party deregisters, it deregisters. So a tree
// of phasers behaves exactly like one phaser with all the leaves' parties, which is the point --
// in the JDK tiering exists to spread CAS contention over several state words, and the semantics
// were never supposed to change. Here there is no contention to spread, so what is implemented is
// only the part that IS observable: the shared phase, and the parent/child registration cascade.
//
// Single-exit style throughout (finding #105).
public class Phaser {

    private final Object sync = new Object();
    // The tree. `root` is `this` for an untiered phaser, and both are final -- a phaser's place
    // in the tree is fixed at construction, so no reader ever needs a lock to follow them.
    private final Phaser parent;
    private final Phaser root;
    // The current phase number, always >= 0; the negative values callers see are produced on
    // the way out by terminatedPhase(), never stored. Meaningful only at the ROOT: a child reads
    // its parent's, all the way up, which is what makes the whole tree advance as one.
    private int phase;
    // Parties currently registered.
    private int parties;
    // Of those, how many have not yet arrived at the current phase.
    private int unarrived;
    private boolean terminated;

    public Phaser() {
        this(null, 0);
    }

    public Phaser(int parties) {
        this(null, parties);
    }

    /** A child of {@code parent} with no parties of its own yet. */
    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    /**
     * A child of {@code parent} with {@code parties} parties.
     *
     * <p>Registering with the parent happens here only when there is at least one party: an empty
     * child is not yet anybody's party, exactly as in the JDK. A party registered later takes the
     * child from zero to one and *that* is when the child joins its parent.
     *
     * @param parent the phaser this one is a party of, or null for a root
     */
    public Phaser(Phaser parent, int parties) {
        if (parties < 0) {
            throw new IllegalArgumentException("parties < 0");
        }
        this.parent = parent;
        if (parent == null) {
            this.root = this;
        } else {
            this.root = parent.root;
        }
        this.parties = parties;
        this.unarrived = parties;
        if (parent != null && parties > 0) {
            parent.register();
        }
    }

    // The phase number as a *terminated* phaser reports it: the same number with the sign bit
    // set, hence negative. Written as `1 << 31` rather than `Integer.MIN_VALUE` because
    // reading a static field of another compiled class traps at run time (finding #110).
    private static int terminatedPhase(int p) {
        return p | (1 << 31);
    }

    // The phase to report right now: negative once terminated. Read from the ROOT, because that
    // is where both the number and the termination flag live -- a child that answered from its own
    // fields would report a phase the rest of the tree had already left.
    private int currentPhase() {
        int p;
        synchronized (root.sync) {
            if (root.terminated) {
                p = terminatedPhase(root.phase);
            } else {
                p = root.phase;
            }
        }
        return p;
    }

    // Whether the tree has terminated. Same reasoning: the flag belongs to the root.
    private boolean treeTerminated() {
        boolean t;
        synchronized (root.sync) {
            t = root.terminated;
        }
        return t;
    }

    // Add `n` parties to the current phase. They count as *unarrived*, so a party that
    // registers mid-phase must itself arrive before the phase can advance — which is exactly
    // what makes "spawn a helper and have it join this round" work.
    private int doRegister(int n) {
        int p;
        boolean joinParent = false;
        synchronized (sync) {
            if (treeTerminated()) {
                p = currentPhase();
            } else {
                // Zero to nonzero is when a child becomes a party of its parent -- see the note
                // on the tiering constructor.
                if (parties == 0 && parent != null) {
                    joinParent = true;
                }
                parties = parties + n;
                unarrived = unarrived + n;
                p = currentPhase();
            }
        }
        // Outside our own monitor: the registration may cascade all the way to the root, and the
        // other parties of *this* phaser have no business waiting for that climb.
        if (joinParent) {
            parent.register();
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
        return doArrive(false);
    }

    // Arrive and give up registration in one step, then leave without waiting. Returns the
    // phase arrived at. This is how a worker retires: the parties it leaves behind are one
    // fewer, so the *next* phase completes with one fewer arrival -- and if it was the last
    // party, the phaser terminates (or, in a tree, leaves its parent).
    public int arriveAndDeregister() {
        return doArrive(true);
    }

    /**
     * The body of both arrivals.
     *
     * <p>The interesting half is the branch on {@code parent}. A ROOT that runs out of unarrived
     * parties advances the phase itself. A CHILD instead resets its own round and reports a
     * single arrival to its parent -- which is the whole of tiering: N parties of a child count
     * as one party of its parent, so the tree completes a phase exactly when every leaf has.
     *
     * <p>The parent is called after the monitor is released. Holding a child's lock across a call
     * that may climb to the root would make every party of that child wait for the climb, and it
     * is the one place where the tree's depth would become visible as latency.
     */
    private int doArrive(boolean deregister) {
        int p;
        boolean tellParentArrived = false;
        boolean tellParentGone = false;
        synchronized (sync) {
            if (treeTerminated()) {
                p = currentPhase();
            } else {
                if (unarrived <= 0) {
                    throw new IllegalStateException("Attempted arrival of unregistered party");
                }
                p = currentPhase();
                unarrived = unarrived - 1;
                if (deregister) {
                    parties = parties - 1;
                }
                if (unarrived == 0) {
                    if (parent == null) {
                        // parties is already decremented, so onAdvance sees the population that
                        // will actually run the next phase.
                        advance();
                    } else {
                        unarrived = parties;
                        tellParentArrived = true;
                    }
                }
                if (deregister && parties == 0 && parent != null) {
                    // Nothing left here, so this phaser stops being a party of its parent. It
                    // arrives *and* deregisters, so the parent's round is not left short.
                    tellParentGone = true;
                    tellParentArrived = false;
                }
            }
        }
        if (tellParentGone) {
            parent.arriveAndDeregister();
        } else if (tellParentArrived) {
            parent.arrive();
        }
        return p;
    }

    // Arrive and block until every other registered party has arrived too -- the barrier
    // proper. Returns the phase number of the phase that just *started* (negative if the
    // arrival terminated the phaser instead), which is what makes a worker loop of the form
    // `while (phaser.arriveAndAwaitAdvance() >= 0)` terminate on its own.
    //
    // Written as arrive-then-await, exactly as the JDK writes it, and that is not a shortcut: the
    // arrival must not be made while holding a lock the wait would then need, or a tiered phaser
    // would deadlock against its own root. Between the two calls the phase may already have
    // moved, which awaitAdvance handles by returning at once.
    public int arriveAndAwaitAdvance() {
        return awaitAdvance(arrive());
    }

    /**
     * Blocks until the phaser leaves the given phase, **without** arriving at it.
     *
     * <p>Useful for an observer that is not one of the parties. A negative argument -- the value a
     * terminated phaser reports -- returns immediately, so a loop reading the previous call's
     * result unwinds by itself.
     *
     * <p>The wait is on the ROOT's monitor and takes no other lock. Every phaser in a tree shares
     * one phase, so there is exactly one place to wait, and waiting anywhere else would mean
     * missing the advance that a sibling's arrival caused.
     *
     * <p>Not interruptible: advancing a phase is not something a party may abandon halfway, since
     * the others would wait for ever for an arrival that never comes. The interrupt is not lost --
     * the thread is marked again on the way out. {@link #awaitAdvanceInterruptibly} is the
     * abortable form.
     */
    public int awaitAdvance(int p) {
        int result;
        boolean interrumpido = false;
        if (p < 0) {
            result = p;
        } else {
            synchronized (root.sync) {
                while (root.phase == p && !root.terminated) {
                    try {
                        root.sync.wait();
                    } catch (InterruptedException e) {
                        interrumpido = true;
                    }
                }
                if (root.terminated) {
                    result = terminatedPhase(root.phase);
                } else {
                    result = root.phase;
                }
            }
            if (interrumpido) {
                Thread.currentThread().interrupt();
            }
        }
        return result;
    }

    /**
     * {@link #awaitAdvance} that gives up when the thread is interrupted.
     *
     * <p>The difference is not cosmetic: this one leaves without arriving and without advancing
     * anything, so the caller inherits responsibility for whatever the other parties are still
     * waiting for. That is the trade the two methods offer -- a wait that cannot be aborted and a
     * phase that always completes, or a wait that can and a phase that may not.
     */
    public int awaitAdvanceInterruptibly(int p) throws InterruptedException {
        int result;
        if (p < 0) {
            result = p;
        } else {
            synchronized (root.sync) {
                while (root.phase == p && !root.terminated) {
                    root.sync.wait();
                }
                if (root.terminated) {
                    result = terminatedPhase(root.phase);
                } else {
                    result = root.phase;
                }
            }
        }
        return result;
    }

    /**
     * The same with a deadline.
     *
     * @throws TimeoutException if the phase had not advanced when the time ran out -- and note
     *         that this leaves the phaser exactly as it was, since waiting never changed it
     */
    public int awaitAdvanceInterruptibly(int p, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        int result;
        if (p < 0) {
            result = p;
        } else {
            long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
            synchronized (root.sync) {
                long remaining = deadline - System.currentTimeMillis();
                while (root.phase == p && !root.terminated && remaining > 0L) {
                    root.sync.wait(remaining);
                    remaining = deadline - System.currentTimeMillis();
                }
                if (root.phase == p && !root.terminated) {
                    throw new TimeoutException();
                }
                if (root.terminated) {
                    result = terminatedPhase(root.phase);
                } else {
                    result = root.phase;
                }
            }
        }
        return result;
    }

    // Terminate the phaser now, releasing every waiter with a negative phase. Unlike
    // {@link CyclicBarrier#reset} this is not recoverable: termination is permanent, and it
    // is the escape hatch for a worker that fails and would otherwise leave the rest blocked
    // forever waiting for an arrival that will never come.
    public void forceTermination() {
        // Terminated at the root, because that is where the flag lives: terminating a child alone
        // would leave the rest of the tree waiting for a party that is never coming back.
        synchronized (root.sync) {
            root.terminated = true;
            root.sync.notifyAll();
        }
    }

    public boolean isTerminated() {
        return treeTerminated();
    }

    // The phaser this one is a party of, or null if it is a root.
    public Phaser getParent() {
        return parent;
    }

    // The top of the tree -- `this` for an untiered phaser. The root is where the phase, the
    // termination flag and every waiter actually are.
    public Phaser getRoot() {
        return root;
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
