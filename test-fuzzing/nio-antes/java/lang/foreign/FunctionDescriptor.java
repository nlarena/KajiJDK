package java.lang.foreign;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.FunctionDescriptor -- a native function's **signature**: the
 * layouts of its arguments and that of its return.
 *
 * <p>Like the layouts, it is a description and nothing else: there is no function behind it. That is
 * why it is here in full even though {@link Linker} --the only thing that knows how to do anything
 * with it-- cannot be.
 *
 * <p>The return is **optional** and that is no detail: a function returning nothing is different
 * from one returning something of size zero. `void` has no layout, and modelling it with an empty
 * layout would confuse the two.
 */
public interface FunctionDescriptor {

    /** The return's layout, or empty if the function is `void`. */
    Optional<MemoryLayout> returnLayout();

    /** The arguments' layouts, in order. */
    List<MemoryLayout> argumentLayouts();

    /** The same descriptor with another return. */
    FunctionDescriptor changeReturnLayout(MemoryLayout newReturn);

    /** The same descriptor with no return: the function becomes `void`. */
    FunctionDescriptor dropReturnLayout();

    /** With those arguments added at the end. */
    FunctionDescriptor appendArgumentLayouts(MemoryLayout... addedLayouts);

    /**
     * With those arguments inserted at that position.
     *
     * @throws IllegalArgumentException if the position is out of range
     */
    FunctionDescriptor insertArgumentLayouts(int index, MemoryLayout... addedLayouts);

    /**
     * The equivalent {@link MethodType}: the Java types these layouts travel in.
     *
     * <p>It is the bridge between the description of the memory and the signature of the method
     * handling it. A composite layout has no Java type of its own, so only a descriptor made of
     * values can be converted.
     *
     * @throws UnsupportedOperationException if some layout is not a {@link ValueLayout}
     */
    MethodType toMethodType();

    /** A descriptor with a return. */
    static FunctionDescriptor of(MemoryLayout resLayout, MemoryLayout... argLayouts) {
        return Descriptor.create(resLayout, argLayouts);
    }

    /** A descriptor with no return. */
    static FunctionDescriptor ofVoid(MemoryLayout... argLayouts) {
        return Descriptor.create(null, argLayouts);
    }
}
