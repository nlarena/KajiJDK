package java.lang.foreign;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * KajiLibrary's java.lang.foreign.SegmentAllocator -- something that knows how to hand out memory.
 *
 * <p>A single abstract operation --{@link #allocate(long, long)}-- and twenty-three convenient forms
 * on top. That proportion is the design: whoever writes an allocator of their own implements **one**
 * of the twenty-four and inherits the rest working.
 *
 * <p>The `allocateFrom` methods are the most used and it is worth understanding what they do: they
 * reserve and **write** in one go. `allocateFrom(JAVA_INT, 1, 2, 3)` gives a twelve-byte segment
 * with those three integers inside, which is the "build an array to pass to a function" pattern.
 */
public interface SegmentAllocator {

    /**
     * It reserves `byteSize` bytes with that alignment.
     *
     * @throws IllegalArgumentException if the size is negative or the alignment is not a positive
     *     power of two
     */
    MemorySegment allocate(long byteSize, long byteAlignment);

    /** It reserves `byteSize` bytes, demanding no alignment. */
    default MemorySegment allocate(long byteSize) {
        return this.allocate(byteSize, 1L);
    }

    /** It reserves what that layout takes up, with its alignment. */
    default MemorySegment allocate(MemoryLayout layout) {
        if (layout == null) {
            throw new NullPointerException("layout");
        }
        return this.allocate(layout.byteSize(), layout.byteAlignment());
    }

    /** It reserves `count` copies of that layout. */
    default MemorySegment allocate(MemoryLayout elementLayout, long count) {
        if (elementLayout == null) {
            throw new NullPointerException("elementLayout");
        }
        if (count < 0L) {
            throw new IllegalArgumentException("count negativa: " + count);
        }
        return this.allocate(elementLayout.byteSize() * count, elementLayout.byteAlignment());
    }

    /** It reserves and writes that string, in UTF-8 and with the trailing zero. */
    default MemorySegment allocateFrom(String str) {
        return this.allocateFrom(str, StandardCharsets.UTF_8);
    }

    /**
     * The same with another charset.
     *
     * <p>The trailing zero's byte counts, and that is why the segment is one byte longer than the
     * encoding: without it, whoever reads the string on the other side would keep reading up to
     * whatever next zero happened to be around.
     */
    default MemorySegment allocateFrom(String str, Charset charset) {
        if (str == null) {
            throw new NullPointerException("str");
        }
        byte[] raw = str.getBytes(charset);
        MemorySegment s = this.allocate((long) raw.length + 1L, 1L);
        s.setString(0L, str, charset);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfByte layout, byte value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfChar layout, char value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfShort layout, short value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfInt layout, int value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfLong layout, long value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfFloat layout, float value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that value. */
    default MemorySegment allocateFrom(ValueLayout.OfDouble layout, double value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes that segment's address. */
    default MemorySegment allocateFrom(AddressLayout layout, MemorySegment value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfByte elementLayout, byte... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfChar elementLayout, char... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfShort elementLayout, short... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfInt elementLayout, int... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfLong elementLayout, long... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfFloat elementLayout, float... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and writes those values. */
    default MemorySegment allocateFrom(ValueLayout.OfDouble elementLayout, double... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** It reserves and copies `elementCount` elements from another segment. */
    default MemorySegment allocateFrom(ValueLayout elementLayout, MemorySegment source,
            ValueLayout sourceElementLayout, long sourceOffset, long elementCount) {
        MemorySegment s = this.allocate(elementLayout, elementCount);
        MemorySegment.copy(source, sourceElementLayout, sourceOffset, s, elementLayout, 0L,
                elementCount);
        return s;
    }

    /**
     * An allocator that goes **slicing** that segment, front to back.
     *
     * <p>It is for sharing out a block reserved once among several small reservations. It runs out
     * when the segment runs out, and then it throws: it does not grow.
     */
    static SegmentAllocator slicingAllocator(MemorySegment segment) {
        if (segment == null) {
            throw new NullPointerException("segment");
        }
        return new SlicingAllocator(segment);
    }

    /**
     * An allocator that returns **always the same prefix** of that segment.
     *
     * <p>It is for reusing a single buffer in a loop without reserving each time. And for that very
     * reason care is needed: two reservations in a row return the same memory, so the second
     * overwrites the first. The JDK documents it the same way; it is not an oversight but the point.
     */
    static SegmentAllocator prefixAllocator(MemorySegment segment) {
        if (segment == null) {
            throw new NullPointerException("segment");
        }
        return new PrefixAllocator(segment);
    }
}

// It goes slicing the segment front to back, respecting whatever alignment is asked for.
final class SlicingAllocator implements SegmentAllocator {

    private final MemorySegment block;
    private long used;

    SlicingAllocator(MemorySegment block) {
        this.block = block;
        this.used = 0L;
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0L) {
            throw new IllegalArgumentException("size negativo: " + byteSize);
        }
        Layouts.requireAlignment(byteAlignment);
        long start = this.used;
        long remaining = start % byteAlignment;
        if (remaining != 0L) {
            start = start + (byteAlignment - remaining);
        }
        if (start + byteSize > this.block.byteSize()) {
            throw new IndexOutOfBoundsException(
                    "no room left: " + byteSize + " bytes y quedan "
                            + (this.block.byteSize() - start));
        }
        MemorySegment s = this.block.asSlice(start, byteSize);
        this.used = start + byteSize;
        return s;
    }
}

// It always returns the same prefix. Each reservation overwrites the previous one, and that is
// what it is asked for.
final class PrefixAllocator implements SegmentAllocator {

    private final MemorySegment block;

    PrefixAllocator(MemorySegment block) {
        this.block = block;
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0L || byteSize > this.block.byteSize()) {
            throw new IndexOutOfBoundsException(
                    "the prefix asked for does not fit: " + byteSize + " of " + this.block.byteSize());
        }
        Layouts.requireAlignment(byteAlignment);
        return this.block.asSlice(0L, byteSize);
    }
}
