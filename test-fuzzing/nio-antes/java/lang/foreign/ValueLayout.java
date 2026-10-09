package java.lang.foreign;

import java.nio.ByteOrder;

/**
 * KajiLibrary's java.lang.foreign.ValueLayout -- the layout of a value that can be read and written
 * in one go: Java's eight primitives, plus the address.
 *
 * <p>It is the leaf of the layout tree. Everything else --sequences, structs, unions-- is composed
 * of these, and they are the only ones a {@link MemorySegment} knows how to read directly.
 *
 * <p>It carries three things a composite layout does not have: the **Java type** it travels in
 * ({@link #carrier()}), the **byte order** ({@link #order()}), and --therefore-- a well-defined
 * reading. The eight nested subtypes (`OfInt`, `OfLong`, ...) exist so the compiler can tell them
 * apart: `segment.get(JAVA_INT, 0)` returns an `int` and not an `Object` because the overload is
 * chosen by the layout's type.
 *
 * <h2>The constants, and why there are two of each</h2>
 *
 * <p>`JAVA_INT` aligns to 4; `JAVA_INT_UNALIGNED` to 1. The difference matters more than it looks: a
 * segment over a `byte[]` has a maximum alignment of **1**, so `segment.get(JAVA_INT, 0)` over an
 * array of bytes **fails**, and the unaligned version has to be used. It is no whim of the library's:
 * it is what the JDK does, and the reason is that the JVM does not guarantee where a `byte[]` falls
 * in memory.
 *
 * <p>`JAVA_BYTE` and `JAVA_BOOLEAN` have no unaligned twin because they take one byte: they are
 * already aligned to 1.
 *
 * <h2>`varHandle()`</h2>
 *
 * <p>{@link #varHandle()} --the `JAVA_INT.varHandle()` shortcut for the `VarHandle` with coordinates
 * `(MemorySegment, long)`-- is here and it works. It used to be absent for the same tool-chain
 * reason as {@link MemoryLayout}'s five; see that interface's note for what changed. `VhTest` covers
 * it.
 *
 * <p>{@link MemorySegment#get(ValueLayout.OfInt, long)} and its family do exactly the same reading
 * taking the layout as an argument instead of baked into a handle, and stay the shorter way for a
 * one-off access.
 */
public interface ValueLayout extends MemoryLayout {

    /**
     * The `JAVA_INT.varHandle()` shortcut: **this** value's {@link java.lang.invoke.VarHandle}, with
     * no path.
     *
     * <p>Its coordinates are the segment and the displacement, and nothing else -- there are no
     * steps to open.
     */
    default java.lang.invoke.VarHandle varHandle() {
        return java.lang.invoke.VarHandles.ofSegment(this, 0L, new long[0]);
    }

    /** The Java type this layout travels in: `int.class`, `long.class`... */
    Class<?> carrier();

    /** The byte order it is read and written with. */
    ByteOrder order();

    /** The same layout with another byte order. */
    ValueLayout withOrder(ByteOrder order);

    ValueLayout withName(String name);

    ValueLayout withoutName();

    ValueLayout withByteAlignment(long byteAlignment);

    // ---- the eight subtypes -----------------------------------------------------------------------
    //
    // Each one narrows the three `with*` to itself. That is not decoration: it is what allows writing
    // `JAVA_INT.withName("x").withOrder(BIG_ENDIAN)` without casting, and --more importantly-- what
    // makes `get(JAVA_INT.withName("x"), 0)` keep choosing the overload that returns `int`.

    /** A `boolean`'s layout. */
    interface OfBoolean extends ValueLayout {
        OfBoolean withName(String name);

        OfBoolean withoutName();

        OfBoolean withByteAlignment(long byteAlignment);

        OfBoolean withOrder(ByteOrder order);
    }

    /** A `byte`'s layout. */
    interface OfByte extends ValueLayout {
        OfByte withName(String name);

        OfByte withoutName();

        OfByte withByteAlignment(long byteAlignment);

        OfByte withOrder(ByteOrder order);
    }

    /** A `char`'s layout. */
    interface OfChar extends ValueLayout {
        OfChar withName(String name);

        OfChar withoutName();

        OfChar withByteAlignment(long byteAlignment);

        OfChar withOrder(ByteOrder order);
    }

    /** A `short`'s layout. */
    interface OfShort extends ValueLayout {
        OfShort withName(String name);

        OfShort withoutName();

        OfShort withByteAlignment(long byteAlignment);

        OfShort withOrder(ByteOrder order);
    }

    /** An `int`'s layout. */
    interface OfInt extends ValueLayout {
        OfInt withName(String name);

        OfInt withoutName();

        OfInt withByteAlignment(long byteAlignment);

        OfInt withOrder(ByteOrder order);
    }

    /** A `long`'s layout. */
    interface OfLong extends ValueLayout {
        OfLong withName(String name);

        OfLong withoutName();

        OfLong withByteAlignment(long byteAlignment);

        OfLong withOrder(ByteOrder order);
    }

    /** A `float`'s layout. */
    interface OfFloat extends ValueLayout {
        OfFloat withName(String name);

        OfFloat withoutName();

        OfFloat withByteAlignment(long byteAlignment);

        OfFloat withOrder(ByteOrder order);
    }

    /** A `double`'s layout. */
    interface OfDouble extends ValueLayout {
        OfDouble withName(String name);

        OfDouble withoutName();

        OfDouble withByteAlignment(long byteAlignment);

        OfDouble withOrder(ByteOrder order);
    }

    // ---- the constants ----------------------------------------------------------------------------

    /** `boolean`, one byte. */
    ValueLayout.OfBoolean JAVA_BOOLEAN = Layouts.ofBoolean();

    /** `byte`, one byte. */
    ValueLayout.OfByte JAVA_BYTE = Layouts.ofByte();

    /** `char`, two bytes, aligned to 2. */
    ValueLayout.OfChar JAVA_CHAR = Layouts.ofChar(2L);

    /** `char` with no alignment constraint. */
    ValueLayout.OfChar JAVA_CHAR_UNALIGNED = Layouts.ofChar(1L);

    /** `short`, two bytes, aligned to 2. */
    ValueLayout.OfShort JAVA_SHORT = Layouts.ofShort(2L);

    /** `short` with no alignment constraint. */
    ValueLayout.OfShort JAVA_SHORT_UNALIGNED = Layouts.ofShort(1L);

    /** `int`, four bytes, aligned to 4. */
    ValueLayout.OfInt JAVA_INT = Layouts.ofInt(4L);

    /** `int` with no alignment constraint -- the one needed over a `byte[]`. */
    ValueLayout.OfInt JAVA_INT_UNALIGNED = Layouts.ofInt(1L);

    /** `long`, eight bytes, aligned to 8. */
    ValueLayout.OfLong JAVA_LONG = Layouts.ofLong(8L);

    /** `long` with no alignment constraint. */
    ValueLayout.OfLong JAVA_LONG_UNALIGNED = Layouts.ofLong(1L);

    /** `float`, four bytes, aligned to 4. */
    ValueLayout.OfFloat JAVA_FLOAT = Layouts.ofFloat(4L);

    /** `float` with no alignment constraint. */
    ValueLayout.OfFloat JAVA_FLOAT_UNALIGNED = Layouts.ofFloat(1L);

    /** `double`, eight bytes, aligned to 8. */
    ValueLayout.OfDouble JAVA_DOUBLE = Layouts.ofDouble(8L);

    /** `double` with no alignment constraint. */
    ValueLayout.OfDouble JAVA_DOUBLE_UNALIGNED = Layouts.ofDouble(1L);

    /**
     * An address, eight bytes.
     *
     * <p>Eight and not four because this is a 64-bit model. The JDK fits it to the platform; here it
     * is fixed, and that is a difference that shows if somebody describes a 32-bit native structure.
     */
    AddressLayout ADDRESS = Layouts.address(8L);

    /** An address with no alignment constraint. */
    AddressLayout ADDRESS_UNALIGNED = Layouts.address(1L);
}
