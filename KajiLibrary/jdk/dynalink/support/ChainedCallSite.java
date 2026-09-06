package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.GuardedInvocation;

/**
 * A call site that <strong>accumulates</strong> invocations: the polymorphic cache.
 *
 * <h2>How the target ends up built</h2>
 *
 * <p>The invocations are chained one inside the other. If the first one's guard fails the second is
 * tried, if that one fails the third, and so on until they run out and only then is the site linked
 * again. A site that sees three receiver types ends up with all three invocations in place and never
 * links again.
 *
 * <p>It is the answer to the case {@link SimpleRelinkableCallSite} makes pathological: two types
 * alternating, where the monomorphic site relinks on every call.
 *
 * <h2>Why the chain has a cap</h2>
 *
 * <p>Because chaining stops paying off. Every link is one more guard evaluated before reaching the
 * one that serves, so a chain of fifty is slower than linking again. And the JIT cannot inline an
 * arbitrarily long chain, so past a certain point the site gets slower the more it learns.
 *
 * <p>The cap is {@link #getMaxChainLength}, eight by default, and it is a {@code protected} method
 * so that a subclass can change it knowing what it is doing.
 *
 * <h2>What happens on reaching the cap</h2>
 *
 * <p>The oldest invocation is dropped to make room for the new one. It is a cache by age, not by
 * frequency: no count is kept of which one is used most because counting on the hot path would cost
 * more than the better policy would save.
 *
 * <h2>Invalidated ones clean themselves up</h2>
 *
 * <p>Before the target is built, the invocations whose switch points have already been thrown are
 * dropped. It is the natural moment for it: the whole chain is being walked anyway, and a dead
 * invocation would take up one of the capped slots without ever being able to serve.
 *
 * @since 9
 */
public class ChainedCallSite extends AbstractRelinkableCallSite {

    /**
     * The invocations in force, from the oldest to the newest.
     *
     * <p>It is a linked list because the two operations performed are appending at the end and
     * removing from the front, and both are constant time there.
     */
    private final List<GuardedInvocation> invocations = new LinkedList<GuardedInvocation>();

    /**
     * A site with that descriptor.
     *
     * @param descriptor the descriptor
     */
    public ChainedCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * How many invocations are kept at most.
     *
     * <p>Eight, like the JDK. Overriding it upwards only makes sense once it has been measured that
     * the site sees more types than that and that the long chain still pays off.
     *
     * @return the cap
     */
    protected int getMaxChainLength() {
        return 8;
    }

    /**
     * Adds an invocation to the chain.
     *
     * @param guardedInvocation the invocation with its guard
     * @param relinkAndInvoke the fallback path, which links again
     */
    public void relink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        relinkInternal(guardedInvocation, relinkAndInvoke, false);
    }

    /**
     * Throws the whole chain away and starts over with this invocation.
     *
     * <p>The linker asks for it once it has decided the site is unstable: if the receiver keeps
     * changing, accumulating invocations only wastes memory and adds guards that will fail.
     *
     * @param guardedInvocation the invocation with its guard
     * @param relinkAndInvoke the fallback path, which links again
     */
    public void resetAndRelink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        relinkInternal(guardedInvocation, relinkAndInvoke, true);
    }

    private void relinkInternal(final GuardedInvocation invocation, final MethodHandle fallback,
            final boolean reset) {
        if (reset) {
            invocations.clear();
        } else {
            // Take the dead ones out before counting: otherwise an invalidated one could push out
            // one that still serves.
            final Iterator<GuardedInvocation> it = invocations.iterator();
            while (it.hasNext()) {
                if (it.next().hasBeenInvalidated()) {
                    it.remove();
                }
            }
            while (invocations.size() >= getMaxChainLength()) {
                invocations.remove(0);
            }
        }
        invocations.add(invocation);

        // The building goes from the end towards the front: the last one added ends up innermost,
        // with the real fallback behind it, and each earlier one wraps it. That way the first in the
        // list is the first one tried.
        MethodHandle target = fallback;
        for (int i = invocations.size() - 1; i >= 0; i--) {
            target = invocations.get(i).compose(target);
        }
        setTarget(target);
    }
}
