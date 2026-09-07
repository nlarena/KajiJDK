package jdk.dynalink;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Objects;

/**
 * Carrier of a {@link Lookup}, with the distinction between handing it out and using it inside.
 *
 * <p>A `Lookup` is a credential: whoever holds it can reach the private members of the class that
 * created it. That is why there are two accessors for the same field. {@link #getLookup()} is the
 * public one, the one a third-party linker calls; {@link #getLookupPrivileged()} is `protected` and
 * is used by the subclasses that are already on the inside — {@link CallSiteDescriptor} calls it in
 * `equals`, `hashCode` and `toString`, where going through the control would make no sense.
 *
 * <p>Both are `final`: the separation would be worth nothing if a subclass could redefine which of
 * the two returns what.
 *
 * @since 9
 */
public class SecureLookupSupplier {

    /**
     * Name of the `RuntimePermission` that historically guarded {@link #getLookup()}.
     *
     * <p>It is kept because it is public API and `final`, even though the `SecurityManager` has been
     * permanently disabled since JDK 24 and the check no longer happens.
     */
    public static final String GET_LOOKUP_PERMISSION_NAME = "dynalink.getLookup";

    private final MethodHandles.Lookup lookup;

    public SecureLookupSupplier(final MethodHandles.Lookup lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    /** The lookup, for the external caller. */
    public final Lookup getLookup() {
        return lookup;
    }

    /** The same lookup, for the subclasses that already operate on the inside. */
    protected final Lookup getLookupPrivileged() {
        return lookup;
    }
}
