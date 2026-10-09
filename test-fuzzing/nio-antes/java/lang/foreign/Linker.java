package java.lang.foreign;

import java.lang.invoke.MethodHandle;
import java.util.Map;

/**
 * KajiLibrary's java.lang.foreign.Linker -- the bridge between Java and a native function.
 *
 * <p><strong>There is no linker in this library, and {@link #nativeLinker()} says so.</strong> The
 * interface is here in full because it is part of the package's shape and because code naming it has
 * to be able to compile; what there is not is an implementation, and there cannot be one: linking to
 * a native function calls for generating call code for the platform's convention, loading dynamic
 * libraries, and moving arguments between Java's stack and the system's. That is VM machinery, not
 * library.
 *
 * <p>That `nativeLinker()` throws is **not a lie but the branch the contract defines**: its javadoc
 * says it throws `UnsupportedOperationException` if the underlying native platform is not supported,
 * and that is exactly the situation. A linker that returned something would give a `MethodHandle` no
 * invocation on which can work, which is worse.
 *
 * <p>What this package **is** good for is everything that describes memory: the layouts,
 * {@link FunctionDescriptor}, and the segments over Java arrays. See {@link MemorySegment}.
 */
public interface Linker {

    /**
     * The platform's linker.
     *
     * @throws UnsupportedOperationException always, in this library. See the interface's note.
     */
    static Linker nativeLinker() {
        throw new UnsupportedOperationException(
                "KajiJDK has no native linker: linking calls for generating call code for the"
                        + " platform's convention, which is VM machinery");
    }

    /** A handle for calling the function sitting at that address. */
    MethodHandle downcallHandle(MemorySegment address, FunctionDescriptor function,
            Linker.Option... options);

    /** A handle with no fixed address: the address is passed as the first argument. */
    MethodHandle downcallHandle(FunctionDescriptor function, Linker.Option... options);

    /** A segment that, called from native code, runs that Java method. */
    MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, Arena arena,
            Linker.Option... options);

    /** The platform's default symbol lookup. */
    SymbolLookup defaultLookup();

    /** The canonical layouts of C's types on this platform (int, long, size_t...). */
    Map<String, MemoryLayout> canonicalLayouts();

    /**
     * A linking option.
     *
     * <p>It is declared empty on purpose: its JDK factories only make sense with a linker behind
     * them, and without one they would be constructors of objects nobody consumes.
     */
    interface Option {
    }
}
