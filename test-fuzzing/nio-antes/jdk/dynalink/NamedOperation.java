package jdk.dynalink;

import java.util.Objects;

/**
 * An {@link Operation} with the member's name already fixed at link time.
 *
 * <p>The distinction that matters: `GET:PROPERTY` takes the name as a run-time argument, while
 * `GET:PROPERTY:x` carries it inside. That lets the linker resolve the member **once** and leave a
 * direct invocation, instead of a lookup per call.
 *
 * <p>The name is an `Object` and not a `String` because there are languages with keys that are not
 * text (symbols, integers). All that is asked of it is `equals`/`hashCode`.
 *
 * @since 9
 */
public final class NamedOperation implements Operation {

    private final Operation baseOperation;
    private final Object name;

    /** @throws IllegalArgumentException if the base is already a `NamedOperation`. */
    public NamedOperation(final Operation baseOperation, final Object name) {
        if (baseOperation instanceof NamedOperation) {
            throw new IllegalArgumentException("baseOperation is a NamedOperation");
        }
        this.baseOperation = Objects.requireNonNull(baseOperation, "baseOperation is null");
        this.name = Objects.requireNonNull(name, "name is null");
    }

    public Operation getBaseOperation() {
        return baseOperation;
    }

    public Object getName() {
        return name;
    }

    public final NamedOperation changeName(final String newName) {
        return new NamedOperation(baseOperation, newName);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NamedOperation) {
            final NamedOperation other = (NamedOperation) obj;
            return baseOperation.equals(other.baseOperation) && name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return baseOperation.hashCode() + 31 * name.hashCode();
    }

    @Override
    public String toString() {
        return baseOperation.toString() + ":" + name.toString();
    }

    /** The base of `op` if it has a name; `op` itself if not. */
    public static Operation getBaseOperation(final Operation op) {
        return op instanceof NamedOperation ? ((NamedOperation) op).baseOperation : op;
    }

    /**
     * The name of `op`, or `null` if `op` is not a named operation.
     *
     * <p>The `null` is the right answer and not an invented value: "this operation carries no name"
     * is exactly what the caller is asking, and that is how the JDK specifies it.
     */
    public static Object getName(final Operation op) {
        return op instanceof NamedOperation ? ((NamedOperation) op).name : null;
    }
}
