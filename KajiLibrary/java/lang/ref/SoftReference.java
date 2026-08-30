package java.lang.ref;

import java.lang.ref.ReferenceQueue;

/**
 * A reference that the collector clears only when it is short of memory.
 *
 * <p>The four reachability strengths are the point of this package, and each exists for one job:
 *
 * <ul>
 *   <li><strong>strong</strong> — an ordinary field; the object never dies while it is reachable
 *   <li><strong>soft</strong> — cleared <em>at the collector's discretion</em>, and only under
 *       memory pressure. That makes it the reference for a <em>cache</em>: keep the entry while
 *       there is room, drop it rather than run out
 *   <li><strong>weak</strong> — cleared as soon as nothing strong points at the referent, which
 *       is what a canonicalising map wants
 *   <li><strong>phantom</strong> — never readable at all; see {@link PhantomReference}
 * </ul>
 *
 * <p>The difference between soft and weak is therefore not <em>when</em> the object becomes
 * unreachable but <em>how eagerly</em> the collector acts on it, and that decision belongs to the
 * VM rather than to this class.
 *
 * <p><strong>This VM already implements the policy.</strong> KajiJDK's collector treats the
 * {@code referent} of every {@code Reference} as a weak edge <em>except</em> a
 * {@code SoftReference}'s while its soft policy is "retain", in which case the edge is traced as
 * an ordinary strong one and the referent survives. It recognises the case by subclass name, so
 * that code path existed with nothing to point at until this class was written.
 *
 * @param <T> the type of the referent
 */
public class SoftReference<T> extends Reference<T> {

    /**
     * Creates a soft reference to the given object, with no queue.
     *
     * @param referent the object to refer to softly
     */
    public SoftReference(T referent) {
        super(referent, null);
    }

    /**
     * Creates a soft reference to the given object, registered with a queue.
     *
     * <p>The reference is enqueued after the collector clears it, which is how a cache learns that
     * an entry is gone and can drop the rest of its bookkeeping.
     *
     * @param referent the object to refer to softly
     * @param queue the queue to enqueue onto when cleared, or {@code null}
     */
    public SoftReference(T referent, ReferenceQueue<? super T> queue) {
        super(referent, queue);
    }

    /**
     * The referent, or {@code null} once the collector has cleared it. Declared here (not merely
     * inherited) to match the reference, which overrides it to record access for its soft policy.
     * KajiJDK's soft policy needs no such bookkeeping, so this reads the referent directly — the
     * field is package-private in {@link Reference} and this class shares its package. (This is why
     * it does not call {@code super.get()}, which our compiler rejects — finding #125.)
     *
     * @return the referent, or {@code null}
     */
    public T get() {
        return (T) this.referent;
    }
}
