package java.lang.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles$Lookup;
import java.lang.invoke.TypeDescriptor;

/**
 * The bootstrap of a {@code record}'s three methods.
 *
 * <h2>Why a bootstrap and not emitted code</h2>
 *
 * <p>A {@code record} promises {@code equals}, {@code hashCode} and {@code toString} derived from its
 * components. The compiler could emit the three bodies into each record --compare field by field, mix
 * hashes, concatenate--, and that was the first idea. The problem is that it would freeze the exact
 * definition of "derived" into every class file ever compiled: changing how a hash is mixed would
 * force recompiling the world.
 *
 * <p>With an {@code invokedynamic} the class file only says <em>which</em> the components are; the
 * JDK decides at run time what to do with them. It is the same move
 * {@link java.lang.invoke.StringConcatFactory} made with concatenation.
 *
 * <h2>All three, from a single entry</h2>
 *
 * <p>{@link #bootstrap} serves all three, and tells them apart by the <strong>call site's
 * name</strong> --the {@code methodName} parameter--, not by the signature. It is the reason a
 * record's {@code BootstrapMethods} table has one entry with three call sites pointing at it.
 *
 * <h2>Here the VM does it</h2>
 *
 * <p>This VM implements the bootstrap in Rust and recognises the class by its name, so this body
 * never runs; the declaration exists so the call site's descriptor resolves and so whoever reads the
 * library finds the class where the JDK has it. It is the same treatment
 * {@code LambdaMetafactory} and {@code StringConcatFactory} get, and it is noted in
 * {@code intrinsecos.md}.
 *
 * @since 16
 */
public final class ObjectMethods {

    private ObjectMethods() {
    }

    /**
     * It builds a record's {@code equals}, {@code hashCode} or {@code toString}.
     *
     * <p>{@code MethodHandles$Lookup} with the <strong>binary</strong> name and not
     * {@code MethodHandles.Lookup}: the Java name of a nested type from another file does not resolve
     * (#101), and dodging that with an {@code import} emits {@code LLookup;}, a class that exists in
     * no package (#208). The binary name gives the JDK's exact descriptor, which is what a call site
     * needs in order to link.
     *
     * @param lookup the access context of the record being linked
     * @param methodName which of the three is asked for: {@code "equals"}, {@code "hashCode"} or
     *     {@code "toString"}
     * @param type the call site's shape
     * @param recordClass the record
     * @param names the components' names, separated by semicolons
     * @param getters one getter per component, in the same order as {@code names}
     */
    public static Object bootstrap(MethodHandles$Lookup lookup, String methodName,
            TypeDescriptor type, Class<?> recordClass, String names, MethodHandle... getters)
            throws Throwable {
        throw new UnsupportedOperationException("a record's methods are built by the VM");
    }
}
