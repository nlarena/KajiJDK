package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.AddressLayout -- a **pointer**'s layout.
 *
 * <p>It is a {@link ValueLayout} plus one thing: it can declare **what it points at**
 * ({@link #withTargetLayout}). Without that declaration a pointer is a number and cannot be
 * followed; with it, `PathElement.dereferenceElement()` can go down to the other side.
 *
 * <p>That the target is optional and not mandatory is faithful to the C this package describes:
 * there, pointers to `void` exist, and forcing a target would mean inventing one.
 */
public interface AddressLayout extends ValueLayout {

    /** The layout it points at, if one was declared. */
    Optional<MemoryLayout> targetLayout();

    /**
     * The same pointer, declaring what it points at.
     *
     * @throws IllegalArgumentException if the target is `null`
     */
    AddressLayout withTargetLayout(MemoryLayout layout);

    /** The same pointer, with no target declared. */
    AddressLayout withoutTargetLayout();

    AddressLayout withName(String name);

    AddressLayout withoutName();

    AddressLayout withByteAlignment(long byteAlignment);

    AddressLayout withOrder(ByteOrder order);
}
