package java.lang.ref;

import java.lang.ref.ReferenceQueue;

/**
 * A reference whose {@link #get()} <em>always</em> returns {@code null}.
 *
 * <p>That is not a limitation, it is the entire design. A phantom reference exists to tell you
 * <em>when</em> an object has become unreachable, never to give it back — because handing it back
 * would resurrect it, which is exactly the mistake {@code finalize()} made and why it was
 * deprecated. The only useful operation is being enqueued, so the constructor requires a queue:
 * a phantom reference with nowhere to report is useless by construction.
 *
 * <p>The pattern it enables is post-mortem cleanup: register a phantom reference, keep the
 * <em>resources</em> to release in a separate object that does not point back at the referent,
 * and when the reference turns up in the queue, release them. {@link Cleaner} is that pattern
 * packaged.
 *
 * <p><strong>In this VM</strong> the collector has no separate phantom phase — it clears and
 * enqueues the referent of every {@code Reference} subclass alike (see {@link SoftReference} for
 * the one exception). The observable difference from a weak reference is therefore only the
 * {@code null} guaranteed below, which is a library promise rather than a collector one. Without
 * finalization in the VM, the ordering guarantee a real phantom reference gives — enqueued only
 * <em>after</em> finalization — has nothing to order against.
 *
 * @param <T> the type of the referent
 */
public class PhantomReference<T> extends Reference<T> {

    /**
     * Creates a phantom reference to the given object, registered with a queue.
     *
     * @param referent the object to watch
     * @param queue the queue to enqueue onto once the referent is unreachable; a phantom
     *              reference with a {@code null} queue can never report anything
     */
    public PhantomReference(T referent, ReferenceQueue<? super T> queue) {
        super(referent, queue);
    }

    /**
     * Always returns {@code null}.
     *
     * @return {@code null}, always — the referent is deliberately unreachable through this class
     */
    public T get() {
        return null;
    }
}
