package java.lang.foreign;

import java.util.List;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.MemoryLayout -- the **shape** of a region of memory: how much it
 * takes up, how it is aligned, and --if it is composite-- what is inside it and in what order.
 *
 * <p>The first thing worth understanding is that a layout **is not memory**: it is a description.
 * Nothing is reserved, nothing to free, nothing that can be read or written. A `MemoryLayout` is to
 * a `MemorySegment` what a type is to a value.
 *
 * <p>And that is why this half of the package can be written entirely in plain Java, while the other
 * --the native linker-- cannot. See {@link Linker}'s note.
 *
 * <h2>Alignment, which is where nearly all the surprises come from</h2>
 *
 * <p>Each layout says how many bytes it takes **and** at what multiple it has to start. An `int`
 * takes 4 and aligns to 4; a `long` takes 8 and aligns to 8. Composing is not just adding sizes up:
 * a {@link #structLayout} demands that each member fall at an offset that is a multiple of its
 * alignment, and if it does not, it **fails instead of arranging things**. Putting a `long` after an
 * `int` does not make a struct: the padding has to be written by hand with {@link #paddingLayout}.
 *
 * <p>That may look inflexible and it is on purpose: a struct that arranges itself has a layout that
 * depends on the platform, and this whole package exists to describe memory **exactly**. Explicit
 * padding is the only way for the description to say the same thing everywhere.
 *
 * <h2>The five handle factories</h2>
 *
 * <p>{@link #varHandle}, {@link #arrayElementVarHandle}, {@link #byteOffsetHandle},
 * {@link #sliceHandle} and {@link #scaleHandle} are here and they work. They used to be absent, and
 * the reason was not scope but the tool chain: the VM did not intercept `VarHandle` and this javac
 * did not implement signature polymorphism (JLS 15.12.3), so the object they return could not be
 * invoked from a call site that compiled. Both halves have since landed --the interpreter routes a
 * `VarHandle` accessor through `SiteKind::VarHandleAccess`, and the compiler emits the polymorphic
 * descriptor-- and `MhLayoutTest` and `VhTest` cover the five.
 *
 * <p>Access by offset --{@link MemorySegment#get(ValueLayout.OfInt, long)} and company-- does the
 * same reading without a handle, and is still the shorter way when the access is used once:
 * `layout.byteOffset(path)` gives the offset and the segment's `get`/`set` reads it. What a handle
 * adds is being able to *keep* that access in an object, pass it around and compose it.
 */
public interface MemoryLayout {

    /** How many bytes it takes up. */
    long byteSize();

    /** At what multiple of bytes it has to start. */
    long byteAlignment();

    /** This layout's name, if it was given one. */
    Optional<String> name();

    /** The same layout with that name. */
    MemoryLayout withName(String name);

    /** The same layout with no name. */
    MemoryLayout withoutName();

    /**
     * The same layout with another alignment.
     *
     * @throws IllegalArgumentException if it is not a positive power of two
     */
    MemoryLayout withByteAlignment(long byteAlignment);

    /**
     * The offset in bytes that path leads to inside this layout.
     *
     * <p>A path is a succession of steps --"member `y`", "element 3"-- and each step goes down one
     * level. It is the way of naming a position inside a nested structure without computing offsets
     * by hand, which is exactly where the mistakes are made.
     */
    long byteOffset(PathElement... elements);

    /** The layout that path leads to. */
    MemoryLayout select(PathElement... elements);

    /**
     * A {@link java.lang.invoke.VarHandle} that reads and writes the value at the end of that path.
     *
     * <p>Its coordinates are the segment, the root layout's displacement within it, and **one `long`
     * per open step** of the path (the `sequenceElement()` with no index). So
     * `struct.varHandle(groupElement("x"))` is accessed with `vh.get(seg, 0L)` and
     * `sequence.varHandle(sequenceElement(), groupElement("x"))` with `vh.get(seg, 0L, i)`.
     *
     * <p>It is the same as `seg.get(valueLayout, layout.byteOffset(path) + displacement)`, with one
     * difference that is the whole point: the access is **kept in an object**, and can be passed
     * around, composed and used without repeating the path at each place.
     */
    default java.lang.invoke.VarHandle varHandle(PathElement... elements) {
        return Layouts.pathHandle(this, 0L, elements);
    }

    /**
     * The one above for an **array** of this layout: it adds an element index at the front.
     *
     * <p>It is equivalent to `sequenceLayout(this).varHandle(sequenceElement(), path)`, and that is
     * why its coordinates carry one `long` more than {@link #varHandle}'s: the element's index,
     * which is multiplied by {@link #byteSize()}.
     */
    default java.lang.invoke.VarHandle arrayElementVarHandle(PathElement... elements) {
        return Layouts.pathHandle(this, this.byteSize(), elements);
    }

    /**
     * A {@link java.lang.invoke.MethodHandle} computing that path's displacement:
     * `(long base, long... indices) -> long`.
     *
     * <p>It is {@link #byteOffset} kept in an object, with the path's open steps as arguments. It is
     * for composing -- which `byteOffset` does not allow, because it returns a number and not an
     * operation.
     */
    default java.lang.invoke.MethodHandle byteOffsetHandle(PathElement... elements) {
        return Layouts.offsetHandle(this, elements);
    }

    /**
     * A handle that **slices** the segment down to the layout at the end of the path:
     * `(MemorySegment, long base, long... indices) -> MemorySegment`.
     *
     * <p>The slice measures exactly what that layout measures, which is the difference from
     * computing the offset by hand and calling `asSlice`: the length comes from the layout and not
     * from the caller, so it cannot be got wrong.
     */
    default java.lang.invoke.MethodHandle sliceHandle(PathElement... elements) {
        return Layouts.sliceHandle0(this, elements);
    }

    /**
     * A handle scaling an index by this layout's size: `(long base, long index) -> long`.
     *
     * <p>It is {@link #scale} kept in an object, and it is the building block for walking an array
     * without repeating the multiplication at each place.
     */
    default java.lang.invoke.MethodHandle scaleHandle() {
        return java.lang.invoke.VarHandles.scale(this);
    }

    /**
     * The offset of an element in an array of this layout: `base + index * byteSize()`.
     *
     * @throws IllegalArgumentException if either is negative
     */
    long scale(long offset, long index);

    // ---- the four factories ---------------------------------------------------------------------

    /**
     * Padding: it takes up space and carries nothing.
     *
     * <p>Its alignment is **1** on purpose. Padding with an alignment of its own would impose a
     * constraint on where nothing may fall, which makes no sense.
     *
     * @throws IllegalArgumentException if `byteSize` is not positive
     */
    static PaddingLayout paddingLayout(long byteSize) {
        return Layouts.padding(byteSize);
    }

    /**
     * A sequence of `elementCount` copies of `elementLayout`, one after the other.
     *
     * @throws IllegalArgumentException if `elementCount` is negative, or if the total size overflows
     */
    static SequenceLayout sequenceLayout(long elementCount, MemoryLayout elementLayout) {
        return Layouts.sequence(elementCount, elementLayout);
    }

    /**
     * The members **one after the other**, like a C `struct`'s fields.
     *
     * @throws IllegalArgumentException if some member falls at an offset that does not respect its
     *     own alignment. See the class's note: padding goes in explicitly.
     */
    static StructLayout structLayout(MemoryLayout... elements) {
        return Layouts.struct(elements);
    }

    /**
     * The members **overlaid**, all starting at offset zero, like a C `union`.
     *
     * <p>The size is that of the largest and the alignment the strictest of them all.
     */
    static UnionLayout unionLayout(MemoryLayout... elements) {
        return Layouts.union(elements);
    }

    /**
     * One step of a path inside a layout.
     *
     * <p>It exists as a type of its own, and not as a `String` or an `int`, because the steps are of
     * different kinds --by name, by index, by all the elements-- and mixing them into a single type
     * would let a meaningless path be written.
     */
    interface PathElement {

        /** The member with that name inside a group. */
        static PathElement groupElement(String name) {
            return Layouts.byName(name);
        }

        /** The member at that position inside a group. */
        static PathElement groupElement(long index) {
            return Layouts.byPosition(index);
        }

        /** **All** of a sequence's elements: it is the step that opens a range, not a single one. */
        static PathElement sequenceElement() {
            return Layouts.allElements();
        }

        /** The element at that position of a sequence. */
        static PathElement sequenceElement(long index) {
            return Layouts.element(index);
        }

        /** A sequence's elements from `start` on, `step` at a time. */
        static PathElement sequenceElement(long start, long step) {
            return Layouts.elements0(start, step);
        }

        /**
         * It follows a pointer: it goes down to the layout an {@link AddressLayout} points at.
         *
         * <p>It is the only step that **leaves** the layout one is in, and that is why it can only
         * be taken over an address that declares what it points at (`withTargetLayout`).
         */
        static PathElement dereferenceElement() {
            return Layouts.dereference();
        }
    }
}
