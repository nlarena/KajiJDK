package jdk.dynalink.linker.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * Several linkers presented as one: they are tried in order until one answers.
 *
 * <p>It is the simplest composition there is, and its cost is linear: a request no linker knows how
 * to handle walks the whole list before giving up. For the case where every component can decide by
 * the receiver's type, {@link CompositeTypeBasedGuardingDynamicLinker} is preferable — it uses that
 * to skip most of them.
 *
 * <p>The list is copied in the constructor: the composition is immutable and cannot be reordered
 * once built.
 *
 * @since 9
 */
public class CompositeGuardingDynamicLinker implements GuardingDynamicLinker {

    private final GuardingDynamicLinker[] linkers;

    /**
     * Composes the linkers in the order they arrive in.
     *
     * @param linkers the linkers
     */
    public CompositeGuardingDynamicLinker(
            final Iterable<? extends GuardingDynamicLinker> linkers) {
        final List<GuardingDynamicLinker> l = new ArrayList<GuardingDynamicLinker>();
        for (final GuardingDynamicLinker linker : linkers) {
            l.add(Objects.requireNonNull(linker));
        }
        this.linkers = l.toArray(new GuardingDynamicLinker[l.size()]);
    }

    /**
     * Whatever the first linker that knows how to handle the request answers.
     *
     * @return the invocation, or {@code null} if none of them knew
     */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest,
            final LinkerServices linkerServices) throws Exception {
        for (final GuardingDynamicLinker linker : linkers) {
            final GuardedInvocation invocation =
                    linker.getGuardedInvocation(linkRequest, linkerServices);
            if (invocation != null) {
                return invocation;
            }
        }
        return null;
    }
}
