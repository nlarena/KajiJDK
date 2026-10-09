package jdk.dynalink;

/**
 * What a dynamic call site wants to do.
 *
 * <p>An operation is **data**, not an action: it describes the intent (read, write, call, construct)
 * without deciding on whom or how. Decoration is by composition and in a fixed order — namespace
 * first, name afterwards — so that `GET.withNamespace(PROPERTY).named("x")` gives a `NamedOperation`
 * wrapping a `NamespaceOperation` wrapping `GET`. The reverse order is forbidden by
 * {@link NamespaceOperation}'s constructors, and that is why taking an operation apart is always the
 * same pair of steps: {@link NamedOperation#getBaseOperation} and then
 * {@link NamespaceOperation#getBaseOperation}.
 *
 * <p>The interface has no abstract methods: an implementation only has to exist and know how to
 * compare itself. The language's five verbs are in {@link StandardOperation}.
 *
 * @since 9
 */
public interface Operation {

    /** This operation, restricted to a single namespace. */
    default NamespaceOperation withNamespace(final Namespace namespace) {
        return withNamespaces(namespace);
    }

    /**
     * This operation over several namespaces, **in order of preference**: the linker tries the first
     * one it can satisfy and only moves on to the next if it found nothing.
     */
    default NamespaceOperation withNamespaces(final Namespace... namespaces) {
        return new NamespaceOperation(this, namespaces);
    }

    /** This operation with a fixed name, known at link time. */
    default NamedOperation named(final Object name) {
        return new NamedOperation(this, name);
    }
}
