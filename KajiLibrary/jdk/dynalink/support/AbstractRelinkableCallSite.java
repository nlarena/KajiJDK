package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MutableCallSite;
import java.util.Objects;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.RelinkableCallSite;

/**
 * The base of a relinkable call site: it holds the descriptor and does the initial installation.
 *
 * <h2>What {@code initialize} solves</h2>
 *
 * <p>The chicken-and-egg problem of a dynamic site. When the JVM first reaches an
 * {@code invokedynamic}, the site does not yet know what to call — knowing means seeing the
 * arguments, and seeing the arguments means the call has to happen.
 *
 * <p>The way out is to install as the initial target a method that <strong>links and then
 * invokes</strong>: the first call goes in there, that method looks at the arguments, decides,
 * reinstalls itself as the target and only then calls. From the second time on the site already
 * points at the right thing.
 *
 * <h2>Why it extends {@link MutableCallSite}</h2>
 *
 * <p>Because the target has to be able to change after it is installed, which is exactly what
 * "relinkable" means. A {@code ConstantCallSite} would not do, and a {@code VolatileCallSite} would
 * cost more for no reason: the race between two threads relinking the same site ends with one of the
 * two targets in place, and both are correct.
 *
 * <h2>What it does not do</h2>
 *
 * <p>It implements neither {@code relink} nor {@code resetAndRelink}: that is the caching strategy,
 * and it is what tells {@link SimpleRelinkableCallSite} from {@link ChainedCallSite}.
 *
 * @since 9
 */
public abstract class AbstractRelinkableCallSite extends MutableCallSite
        implements RelinkableCallSite {

    private final CallSiteDescriptor descriptor;

    /**
     * A site with that descriptor.
     *
     * @param descriptor the descriptor; its signature is the site's
     * @throws NullPointerException if it is {@code null}
     */
    protected AbstractRelinkableCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor.getMethodType());
        this.descriptor = descriptor;
    }

    /**
     * The descriptor, which does not change in the whole life of the site.
     *
     * @return the descriptor
     */
    public CallSiteDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Installs the method that links and then invokes.
     *
     * <p>{@code DynamicLinker.link} calls it exactly once. Calling it again, throwing away whatever
     * the site had learnt, is not provided for by the contract.
     *
     * @param relinkAndInvoke the method that links and then invokes
     */
    public void initialize(final MethodHandle relinkAndInvoke) {
        setTarget(Objects.requireNonNull(relinkAndInvoke));
    }
}
