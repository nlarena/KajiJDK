package jdk.internal.vm;

import java.lang.ScopedValue;

/**
 * KajiLibrary's jdk.internal.vm.ScopedValueContainer -- the scope where the bindings of
 * {@link ScopedValue} live.
 *
 * <p>A `ScopedValue` is bound only while a call lasts, and that call may nest others. This
 * container is what marks where each stretch starts and ends: it is pushed before running the body
 * and popped afterwards, whatever happens.
 *
 * <p>That it is a {@link StackableScope} is what makes the bindings be undone **in reverse order**
 * even if the body leaves through an exception -- it is exactly the problem the base class solves.
 */
public class ScopedValueContainer extends StackableScope {

    /**
     * A snapshot of the bindings in force, together with the container where they were.
     *
     * <p>It is a `record` because it is that and nothing else: two values that travel together and
     * are compared by contents. It serves so that a thread that starts inside a scope inherits what
     * there was, without keeping a live reference to a scope that may already have closed.
     *
     * @param scopedValueBindings the bindings
     * @param container the container that had them
     */
    public record BindingsSnapshot(Object scopedValueBindings, ScopedValueContainer container) {
    }

    protected ScopedValueContainer() {
        super();
    }

    /** The nearest container of that type on the current thread, or `null`. */
    public static <T extends ScopedValueContainer> T latest(Class<T> type) {
        StackableScope head = StackableScope.head();
        if (head == null) {
            return null;
        }
        if (type.isInstance(head)) {
            return (T) head;
        }
        return head.enclosingScope(type);
    }

    /** The nearest container of the current thread, or `null`. */
    public static ScopedValueContainer latest() {
        return ScopedValueContainer.latest(ScopedValueContainer.class);
    }

    /**
     * A snapshot of the bindings in force.
     *
     * <p>It returns a snapshot with `null` bindings when there are none --which is what
     * corresponds, and not a null snapshot: "there are no bindings" is a state, not the absence of
     * an answer.
     */
    public static BindingsSnapshot captureBindings() {
        return new BindingsSnapshot(null, ScopedValueContainer.latest());
    }

    /**
     * It runs `op` inside a new container.
     *
     * <p>The `finally` is the whole class: if the body throws, the container is popped **all the
     * same**. Without that, an exception would leave bindings alive on a thread that has already
     * left the scope, which is the kind of error that later shows up a thousand lines away.
     */
    public static void run(Runnable op) {
        if (op == null) {
            throw new NullPointerException("op");
        }
        ScopedValueContainer c = new ScopedValueContainer();
        c.push();
        try {
            op.run();
        } finally {
            c.popForcefully();
        }
    }

    /**
     * It runs `op` inside a new container and returns its result.
     *
     * <p>The variant that returns a value and that can throw a **checked** exception of its own:
     * hence the second type variable. It is what allows wrapping code that throws without forcing
     * it to be wrapped in something unchecked.
     */
    public static <V, X extends Throwable> V call(ScopedValue.CallableOp<V, X> op) throws X {
        if (op == null) {
            throw new NullPointerException("op");
        }
        ScopedValueContainer c = new ScopedValueContainer();
        c.push();
        try {
            return op.call();
        } finally {
            c.popForcefully();
        }
    }
}
