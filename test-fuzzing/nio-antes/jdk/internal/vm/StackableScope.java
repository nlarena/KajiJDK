package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.StackableScope -- a scope that is stacked per thread.
 *
 * <p>It is the base of structured concurrency: a scope is opened, others are nested inside, and
 * they are closed **in reverse order**. The word "stackable" is the promise: if on closing one we
 * find it was not the top one, somebody skipped the order, and that is detected instead of let
 * through.
 *
 * <p>The stack is **per thread** and lives in a {@link ThreadLocal}. In the JDK it lives in a field
 * of `Thread`, which is faster and arrives earlier during start-up; here `Thread` cannot be touched
 * from this package, and a `ThreadLocal` gives exactly the same semantics. It is an internal, and
 * internals are free.
 *
 * <p>The distinction between {@link #tryPop()} and {@link #popForcefully()} is the one that governs
 * the class: the first pops this scope **only if it is the top one** and returns `false` if it is
 * not --the normal road, where the disorder is reported--; the second pops everything that was left
 * above, closing it, and is the road of the exception, where we already know something went wrong
 * and the stack has to be left clean.
 */
public class StackableScope {

    // The head of the stack of the current thread. One `ThreadLocal` per class, not per instance:
    // the stack is a single one per thread and all the scopes share that view.
    private static final ThreadLocal<StackableScope> HEAD = new ThreadLocal<StackableScope>();

    private final Thread owner;
    private StackableScope previous;
    private boolean pushed;

    /**
     * @param shared whether the scope does **not** belong to a particular thread
     */
    StackableScope(boolean shared) {
        this.owner = shared ? null : Thread.currentThread();
    }

    /** A scope of the thread that builds it. */
    protected StackableScope() {
        this(false);
    }

    /** The owner thread, or `null` if it is shared. */
    public Thread owner() {
        return this.owner;
    }

    /** It pushes this scope onto the current thread and returns it, for chaining. */
    public StackableScope push() {
        this.previous = StackableScope.HEAD.get();
        StackableScope.HEAD.set(this);
        this.pushed = true;
        return this;
    }

    /**
     * It pops this scope **if it is the top one**.
     *
     * @return `false` if it was not, and then the stack is left intact
     */
    public boolean tryPop() {
        if (StackableScope.HEAD.get() != this) {
            return false;
        }
        StackableScope.HEAD.set(this.previous);
        this.previous = null;
        this.pushed = false;
        return true;
    }

    /**
     * It pops this scope **and everything left above it**, closing each one.
     *
     * <p>It is used when there was already an error: what matters is that the thread is left with a
     * coherent stack, not respecting an order somebody already broke. Each scope above receives
     * {@link #tryClose()}, so a subclass with something to release finds out.
     *
     * @return whether this scope was on the stack
     */
    public boolean popForcefully() {
        if (!this.pushed) {
            return false;
        }
        StackableScope cur = StackableScope.HEAD.get();
        while (cur != null && cur != this) {
            StackableScope next = cur.previous;
            cur.tryClose();
            cur.previous = null;
            cur.pushed = false;
            cur = next;
        }
        StackableScope.HEAD.set(this.previous);
        this.previous = null;
        this.pushed = false;
        return true;
    }

    /** It empties the stack of the current thread, closing everything there is. */
    public static void popAll() {
        StackableScope cur = StackableScope.HEAD.get();
        while (cur != null) {
            StackableScope next = cur.previous;
            cur.tryClose();
            cur.previous = null;
            cur.pushed = false;
            cur = next;
        }
        StackableScope.HEAD.set(null);
    }

    /** The scope immediately below this one, or `null`. */
    public StackableScope enclosingScope() {
        return this.previous;
    }

    /**
     * The nearest scope further down that is of that type, or `null`.
     *
     * <p>It is looked for with {@link Class#isInstance}, that is a **subclass** counts too: whoever
     * asks for a `ThreadContainer` wants the nearest container, whatever its class.
     */
    public <T extends StackableScope> T enclosingScope(Class<T> type) {
        StackableScope cur = this.previous;
        while (cur != null) {
            if (type.isInstance(cur)) {
                return (T) cur;
            }
            cur = cur.previous;
        }
        return null;
    }

    /** The previous one, for the package. */
    StackableScope previous() {
        return this.previous;
    }

    /**
     * What has to be done when closing this scope.
     *
     * <p>A bare `StackableScope` has nothing to release, so it says yes. The subclasses that hold
     * something --threads, bindings-- override it; it is the hook through which {@link
     * #popForcefully()} and {@link #popAll()} notify them.
     *
     * @return whether it could be closed
     */
    protected boolean tryClose() {
        return true;
    }

    /** The head of the stack of the current thread, for the package. */
    static StackableScope head() {
        return StackableScope.HEAD.get();
    }
}
