package jdk.dynalink;

import java.util.Arrays;
import java.util.Objects;

/**
 * Una {@link Operation} atada a uno o mas {@link Namespace}, en orden de preferencia.
 *
 * <p>Es inmutable y se compara por valor. El constructor rechaza que la operacion base sea a su
 * vez un `NamespaceOperation` o un {@link NamedOperation}: eso es lo que fija el orden de
 * anidamiento en una sola direccion y hace que desarmar una operacion no tenga casos.
 *
 * @since 9
 */
public final class NamespaceOperation implements Operation {

    private final Operation baseOperation;
    private final Namespace[] namespaces;

    /**
     * @throws IllegalArgumentException si la base ya esta decorada, o si no se paso ningun
     *         espacio de nombres.
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

    /** La operacion sin la decoracion de espacios de nombres. */
    public Operation getBaseOperation() {
        return baseOperation;
    }

    /** Copia del arreglo de espacios de nombres — el interno no se expone. */
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

    /** La base de `op` si esta decorada con espacios de nombres; `op` misma si no lo esta. */
    public static Operation getBaseOperation(final Operation op) {
        return op instanceof NamespaceOperation ? ((NamespaceOperation) op).getBaseOperation() : op;
    }

    /** Los espacios de nombres de `op`, o un arreglo vacio si no tiene. */
    public static Namespace[] getNamespaces(final Operation op) {
        return op instanceof NamespaceOperation ? ((NamespaceOperation) op).getNamespaces() : new Namespace[0];
    }

    /** Si `op` es exactamente `baseOperation` decorada con un conjunto que incluye `namespace`. */
    public static boolean contains(final Operation op, final Operation baseOperation, final Namespace namespace) {
        if (op instanceof NamespaceOperation) {
            final NamespaceOperation no = (NamespaceOperation) op;
            return no.baseOperation.equals(baseOperation) && no.contains(namespace);
        }
        return false;
    }
}
