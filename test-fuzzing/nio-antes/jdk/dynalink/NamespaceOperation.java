package jdk.dynalink;

import java.util.Arrays;
import java.util.Objects;

/**
 * An {@link Operation} bound to one or more {@link Namespace}, in order of preference.
 *
 * <p>It is immutable and compares by value. The constructor refuses a base operation that is itself
 * a `NamespaceOperation` or a {@link NamedOperation}: that is what fixes the nesting order in a
 * single direction and makes taking an operation apart a case-free job.
 *
 * @since 9
 */
public final class NamespaceOperation implements Operation {

    private final Operation baseOperation;
    private final Namespace[] namespaces;

    /**
     * @throws IllegalArgumentException if the base is already decorated, or if no namespace was
     *         passed.
     */
    public NamespaceOperation(final Operation baseOperation, final Namespace... namespaces) {
        this.baseOperation = Objects.requireNonNull(baseOperation, "baseOperation is null");
        if (baseOperation instanceof NamedOperation) {
            throw new IllegalArgumentException("baseOperation is a NamedOperation");
        } else if (baseOperation instanceof NamespaceOperation) {
            throw new IllegalArgumentException("baseOperation is a NamespaceOperation");
        }
        this.namespaces = Objects.requireNonNull(namespaces, "namespaces array is null").clone();
        if (namespaces.length < 1) {
            throw new IllegalArgumentException("Must specify at least one namespace");
        }
        for (int i = 0; i < namespaces.length; ++i) {
            final int fi = i;
            Objects.requireNonNull(namespaces[i], () -> "operations[" + fi + "] is null");
        }
    }

    /** The operation without the namespace decoration. */
    public Operation getBaseOperation() {
        return baseOperation;
    }

    /** A copy of the namespace array — the internal one is not exposed. */
    public Namespace[] getNamespaces() {
        return namespaces.clone();
    }

    public int getNamespaceCount() {
        return namespaces.length;
    }

    public Namespace getNamespace(final int i) {
        try {
            return namespaces[i];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(Integer.toString(i));
        }
    }

    public boolean contains(final Namespace namespace) {
        Objects.requireNonNull(namespace);
        for (final Namespace component : namespaces) {
            if (component.equals(namespace)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NamespaceOperation) {
            final NamespaceOperation other = (NamespaceOperation) obj;
            return baseOperation.equals(other.baseOperation) && Arrays.equals(namespaces, other.namespaces);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return baseOperation.hashCode() + 31 * Arrays.hashCode(namespaces);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(baseOperation).append(':');
        b.append(namespaces[0]);
        for (int i = 1; i < namespaces.length; ++i) {
            b.append('|').append(namespaces[i]);
        }
        return b.toString();
    }

    /** The base of `op` if it is decorated with namespaces; `op` itself if it is not. */
    public static Operation getBaseOperation(final Operation op) {
        return op instanceof NamespaceOperation ? ((NamespaceOperation) op).getBaseOperation() : op;
    }

    /** The namespaces of `op`, or an empty array if it has none. */
    public static Namespace[] getNamespaces(final Operation op) {
        return op instanceof NamespaceOperation ? ((NamespaceOperation) op).getNamespaces() : new Namespace[0];
    }

    /** Whether `op` is exactly `baseOperation` decorated with a set that includes `namespace`. */
    public static boolean contains(final Operation op, final Operation baseOperation, final Namespace namespace) {
        if (op instanceof NamespaceOperation) {
            final NamespaceOperation no = (NamespaceOperation) op;
            return no.baseOperation.equals(baseOperation) && no.contains(namespace);
        }
        return false;
    }
}
