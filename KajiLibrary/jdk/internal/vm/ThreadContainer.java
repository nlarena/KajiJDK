package jdk.internal.vm;

import java.util.stream.Stream;

/**
 * KajiLibrary's jdk.internal.vm.ThreadContainer -- a group of threads with an owner.
 *
 * <p>It is what replaces `ThreadGroup` for structured concurrency, and the difference matters: a
 * `ThreadGroup` is a loose hierarchy nobody closes, while a container **is a scope** --it extends
 * {@link StackableScope}-- and therefore has a beginning and an end. That is the whole point: when
 * the scope ends, it is known which threads there were inside and it can be demanded that they have
 * finished.
 *
 * <p>Hence the hierarchy of containers is not kept with pointers from parent to child, but comes
 * from the **stack of scopes**: {@link #parent()} is the container that encloses this one on the
 * thread that opened it. A tree deduced from the nesting cannot fall out of step with it.
 *
 * <p>{@link #threads()} is abstract on purpose: how the threads are kept depends on the subclass
 * --{@link SharedThreadContainer} uses a concurrent set-- and this class does not choose for it.
 */
public abstract class ThreadContainer extends StackableScope {

    /**
     * @param shared whether the container does **not** belong to a particular thread
     */
    protected ThreadContainer(boolean shared) {
        super(shared);
    }

    /** The name, or `null` if it has none. */
    public String name() {
        return null;
    }

    /** The container that encloses this one, or `null` if it is top-level. */
    public ThreadContainer parent() {
        return ThreadContainers.parent(this);
    }

    /** The containers nested directly in this one. */
    public final Stream<ThreadContainer> children() {
        return ThreadContainers.children(this);
    }

    /**
     * How many threads it has.
     *
     * <p>It is counted by walking {@link #threads()} and not with a separate counter, and it is
     * deliberate: a counter falls out of step --a thread that dies without notice leaves it high
     * forever-- while counting what there is cannot lie. The cost is walking; the JDK's contract
     * already says the number is an estimate.
     */
    public long threadCount() {
        return this.threads().count();
    }

    /** The threads of this container. */
    public abstract Stream<Thread> threads();

    /** Notice that a thread started. The subclasses override it to note it down. */
    protected void onStart(Thread thread) {
    }

    /** Notice that a thread finished. */
    protected void onExit(Thread thread) {
    }

    /**
     * It registers a thread that started.
     *
     * <p>`final` --as in the JDK-- because it separates two things that are as well not mixed: this
     * is the entry point the VM uses, and {@link #onStart} is the hook the subclass overrides. If
     * `add` could be overridden, a subclass could keep the notice without calling the one above.
     */
    public final void add(Thread thread) {
        this.onStart(thread);
    }

    /** It deregisters a thread that finished. */
    public final void remove(Thread thread) {
        this.onExit(thread);
    }

    /** The bindings of {@link java.lang.ScopedValue} in force when this container was opened. */
    public ScopedValueContainer.BindingsSnapshot scopedValueBindings() {
        return null;
    }

    public String toString() {
        String n = this.name();
        String base = n != null ? n : this.getClass().getName();
        return base + "@" + Integer.toHexString(System.identityHashCode(this));
    }
}
