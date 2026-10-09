package jdk.dynalink.linker.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

/**
 * Composes linkers that decide by the receiver's type, with a cache per class.
 *
 * <h2>What it gains over {@link CompositeGuardingDynamicLinker}</h2>
 *
 * <p>That it does not walk the list. The first time a receiver of a given class shows up it asks
 * everyone who accepts it and keeps the answer; from then on it goes straight to the ones that serve.
 * In a chain of twenty linkers where only one understands {@code String}, that is the difference
 * between twenty questions per link and one.
 *
 * <p>The cache is a {@link ClassValue}, not a map: it hangs off the class itself and disappears when
 * the class is unloaded. With an ordinary map, dynamically loaded classes --and in a scripting
 * language there are many-- could never be freed.
 *
 * @since 9
 */
public class CompositeTypeBasedGuardingDynamicLinker implements TypeBasedGuardingDynamicLinker {

    private final LinkersByClass linkersByClass;

    /** The cache: for each class, which of the linkers accept it. */
    private static final class LinkersByClass
            extends ClassValue<List<TypeBasedGuardingDynamicLinker>> {

        private static final List<TypeBasedGuardingDynamicLinker> NONE =
                Collections.emptyList();

        private final TypeBasedGuardingDynamicLinker[] linkers;
        /** Precomputed single-element lists: by far the commonest case. */
        private final List<List<TypeBasedGuardingDynamicLinker>> singletons;

        LinkersByClass(final TypeBasedGuardingDynamicLinker[] linkers) {
            this.linkers = linkers;
            final List<List<TypeBasedGuardingDynamicLinker>> s =
                    new ArrayList<List<TypeBasedGuardingDynamicLinker>>(linkers.length);
            for (int i = 0; i < linkers.length; i++) {
                s.add(Collections.singletonList(linkers[i]));
            }
            this.singletons = s;
        }

        protected List<TypeBasedGuardingDynamicLinker> computeValue(final Class<?> clazz) {
            List<TypeBasedGuardingDynamicLinker> list = NONE;
            for (int i = 0; i < linkers.length; i++) {
                if (!linkers[i].canLinkType(clazz)) {
                    continue;
                }
                if (list == NONE) {
                    list = singletons.get(i);
                } else {
                    if (list.size() == 1) {
                        // Only here does the precomputed list, which is immutable, stop serving.
                        list = new ArrayList<TypeBasedGuardingDynamicLinker>(list);
                    }
                    list.add(linkers[i]);
                }
            }
            return list;
        }
    }

    /**
     * Composes the linkers in the order they arrive in.
     *
     * @param linkers the linkers
     */
    public CompositeTypeBasedGuardingDynamicLinker(
            final Iterable<? extends TypeBasedGuardingDynamicLinker> linkers) {
        final List<TypeBasedGuardingDynamicLinker> l =
                new ArrayList<TypeBasedGuardingDynamicLinker>();
        for (final TypeBasedGuardingDynamicLinker linker : linkers) {
            l.add(Objects.requireNonNull(linker));
        }
        this.linkersByClass = new LinkersByClass(
                l.toArray(new TypeBasedGuardingDynamicLinker[l.size()]));
    }

    /** Whether any of the composed ones accepts that class. */
    public boolean canLinkType(final Class<?> type) {
        return !linkersByClass.get(type).isEmpty();
    }

    /**
     * Whatever the first of the linkers accepting the receiver's class answers.
     *
     * <p>With no receiver there is no class to decide by, and so it returns {@code null} without
     * asking anyone: a type-based linker has nothing to say about an invocation with no arguments.
     */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest,
            final LinkerServices linkerServices) throws Exception {
        final Object receiver = linkRequest.getReceiver();
        if (receiver == null) {
            return null;
        }
        for (final TypeBasedGuardingDynamicLinker linker
                : linkersByClass.get(receiver.getClass())) {
            final GuardedInvocation invocation =
                    linker.getGuardedInvocation(linkRequest, linkerServices);
            if (invocation != null) {
                return invocation;
            }
        }
        return null;
    }

    /**
     * Groups the consecutive runs of type-based linkers, leaving the rest as they are.
     *
     * <p>The order matters and that is why it only groups <strong>consecutive</strong> ones: if there
     * is an ordinary linker between two type-based ones, joining the first two would move them ahead
     * of it, and the chain would stop meaning what its author wrote.
     *
     * @param linkers the linkers, in order
     * @return the optimised list, of the same length or shorter
     */
    public static List<GuardingDynamicLinker> optimize(
            final Iterable<? extends GuardingDynamicLinker> linkers) {
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        final List<TypeBasedGuardingDynamicLinker> run =
                new ArrayList<TypeBasedGuardingDynamicLinker>();
        for (final GuardingDynamicLinker linker : linkers) {
            Objects.requireNonNull(linker);
            if (linker instanceof TypeBasedGuardingDynamicLinker) {
                run.add((TypeBasedGuardingDynamicLinker) linker);
            } else {
                closeRun(out, run);
                out.add(linker);
            }
        }
        closeRun(out, run);
        return out;
    }

    private static void closeRun(final List<GuardingDynamicLinker> out,
            final List<TypeBasedGuardingDynamicLinker> run) {
        if (run.isEmpty()) {
            return;
        }
        if (run.size() == 1) {
            // A single one is not wrapped: the cache would save nothing and would add an indirection.
            out.addAll(run);
        } else {
            out.add(new CompositeTypeBasedGuardingDynamicLinker(run));
        }
        run.clear();
    }
}
