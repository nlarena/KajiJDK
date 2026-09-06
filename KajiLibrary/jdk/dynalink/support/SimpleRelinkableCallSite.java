package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.GuardedInvocation;

/**
 * The simplest call site: it remembers <strong>one</strong> invocation at a time.
 *
 * <h2>The strategy</h2>
 *
 * <p>Every relink throws away the previous one. The target becomes the new invocation with its
 * guard, and the fallback path for when the guard fails links again from scratch.
 *
 * <p>It is the monomorphic cache: blindingly fast when the site always sees the same receiver type
 * —which is the vast majority of the sites in any program— and pathological when it sees two
 * alternating, because then every call throws away the previous call's link and links again.
 *
 * <p>{@link ChainedCallSite} is there for that case, and it accumulates. Choosing between the two is
 * the only decision to be made here, and the criterion is how many different types are expected.
 *
 * <h2>Why {@code relink} and {@code resetAndRelink} do the same thing</h2>
 *
 * <p>Because there is nothing to reset: the difference between the two methods is whether it is
 * worth keeping what was learnt, and this site never keeps anything. In {@link ChainedCallSite} they
 * do differ.
 *
 * @since 9
 */
public class SimpleRelinkableCallSite extends AbstractRelinkableCallSite {

    /**
     * A site with that descriptor.
     *
     * @param descriptor the descriptor
     */
    public SimpleRelinkableCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * Installs the new invocation, discarding the previous one.
     *
     * @param guardedInvocation the invocation with its guard
     * @param relinkAndInvoke the fallback path, which links again
     */
    public void relink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        setTarget(guardedInvocation.compose(relinkAndInvoke));
    }

    /**
     * The same as {@link #relink}: this site accumulates nothing to reset.
     *
     * @param guardedInvocation the invocation with its guard
     * @param relinkAndInvoke the fallback path, which links again
     */
    public void resetAndRelink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        relink(guardedInvocation, relinkAndInvoke);
    }
}
