package java.lang.foreign;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.lang.foreign.MemorySegment -- a **region of memory** with a known size, read and
 * written by offset.
 *
 * <h2>What is here and what is not</h2>
 *
 * <p><strong>This library's segments live over Java arrays.</strong> {@link #ofArray(byte[])} and its
 * six siblings are real and do exactly what they say; an {@link Arena} also hands out segments,
 * backed by an array chosen according to the alignment asked of it. What there is not is memory
 * **outside** the heap: this VM neither reserves nor frees system memory, so {@link #isNative()} is
 * `false` for everything usable.
 *
 * <p>The visible consequence, and it is worth keeping in mind: an `Arena.ofConfined().allocate(16)`
 * gives a native segment in the JDK and a heap one here. Everything else --size, slices, reading,
 * writing, closing the scope-- behaves the same.
 *
 * <h2>Alignment, which is where the surprises come from</h2>
 *
 * <p>A segment over a `byte[]` has {@link #maxByteAlignment()} equal to **1**, because the JVM makes
 * no promise about where an array of bytes falls in memory. That is why
 * `segment.get(ValueLayout.JAVA_INT, 0)` over a `byte[]` **fails**, and `JAVA_INT_UNALIGNED` has to
 * be used. It is not a limitation of this library: it is what the JDK does, and it is why the
 * unaligned constants exist.
 *
 * <p>A segment over a `long[]` has alignment 8 and takes `JAVA_LONG` without further ado.
 *
 * <h2>The scope</h2>
 *
 * <p>A segment belongs to a {@link Scope}, and when the scope closes the segment can no longer be
 * used. That is what turns a memory error into an exception: without a scope, using a segment after
 * freeing it would be undefined behaviour, and with one it is an `IllegalStateException` with the
 * exact line.
 */
public interface MemorySegment {

    /** How many bytes it covers. */
    long byteSize();

    /**
     * This segment's address.
     *
     * <p>For a heap one it is the **offset inside the array** backing it, not a memory address: a
     * Java array moves when the collector compacts, so it does not have one.
     */
    long address();

    /** The array backing it, if it is a heap one. */
    Optional<Object> heapBase();

    /**
     * Whether it lives outside Java's heap.
     *
     * <p>Always `false` except for {@link #NULL} and the ones from {@link #ofAddress(long)}, which
     * have no backing. See the interface's note.
     */
    boolean isNative();

    /** Whether it is mapped from a file. Always `false`: this VM maps no files. */
    boolean isMapped();

    /** Whether it refuses writes. */
    boolean isReadOnly();

    /** The maximum alignment this segment can guarantee. */
    long maxByteAlignment();

    /** The scope it belongs to. */
    Scope scope();

    /**
     * Whether that thread can use it.
     *
     * <p>Always `true`: this library's scopes are not confined to a thread. In the JDK an
     * `Arena.ofConfined()` only lets its segments be used from the thread that created it, and there
     * this can give `false`.
     */
    boolean isAccessibleBy(Thread thread);

    /** The same segment, allowing no writes. */
    MemorySegment asReadOnly();

    /** From that offset to the end. */
    MemorySegment asSlice(long offset);

    /** From that offset, with that length. */
    MemorySegment asSlice(long offset, long newSize);

    /** From that offset, with that length and that alignment demanded. */
    MemorySegment asSlice(long offset, long newSize, long byteAlignment);

    /** The slice that layout describes, from that offset. */
    MemorySegment asSlice(long offset, MemoryLayout layout);

    /**
     * The part of `other` overlapping with this one, if they overlap.
     *
     * <p>It only makes sense between two segments with the **same backing**: two different arrays
     * never overlap, even if their offsets coincide.
     */
    Optional<MemorySegment> asOverlappingSlice(MemorySegment other);

    /**
     * The same segment seen with another size.
     *
     * @throws UnsupportedOperationException in this library when the segment has no backing:
     *     enlarging a segment with no memory behind it would produce one claiming to cover bytes
     *     nobody can read, which is exactly the kind of lie this project does not write.
     */
    MemorySegment reinterpret(long newSize);

    /** See {@link #reinterpret(long)}. */
    MemorySegment reinterpret(Arena arena, java.util.function.Consumer<MemorySegment> cleanup);

    /** See {@link #reinterpret(long)}. */
    MemorySegment reinterpret(long newSize, Arena arena,
            java.util.function.Consumer<MemorySegment> cleanup);

    /** It copies `src`'s contents to the start of this one. */
    MemorySegment copyFrom(MemorySegment src);

    /** It writes that byte over the whole segment. */
    MemorySegment fill(byte value);

    /**
     * The offset of the first byte where this one and `other` differ, or `-1` if they are equal.
     *
     * <p>If one is a prefix of the other, it returns the shorter one's length: that is where they
     * "differ", which is the useful answer for comparing.
     */
    long mismatch(MemorySegment other);


    /** This segment as a {@link ByteBuffer}. */
    ByteBuffer asByteBuffer();

    /** That layout's elements, as a stream. */
    Stream<MemorySegment> elements(MemoryLayout elementLayout);

    /** That layout's elements, as a spliterator. */
    Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout);

    /** A zero-terminated UTF-8 string, from that offset. */
    String getString(long offset);

    /** The same, with another charset. */
    String getString(long offset, java.nio.charset.Charset charset);

    /** It writes a zero-terminated UTF-8 string at that offset. */
    void setString(long offset, String str);

    /** The same, with another charset. */
    void setString(long offset, String str, java.nio.charset.Charset charset);

    // ---- the four mapped-file operations ----------------------------------------------------------
    //
    // All four exist for a segment mapped from a file, which this VM does not do. They do not refuse
    // with an exception because the JDK's contract already defines what to do for a segment that is
    // **not** mapped, and that is what they do here.

    /** Whether it is loaded in memory. `false`: there is no mapping. */
    boolean isLoaded();

    /** It suggests loading it. It does nothing: there is no mapping. */
    void load();

    /** It suggests unloading it. It does nothing: there is no mapping. */
    void unload();

    /** It forces the write to disk. It does nothing: there is no mapping. */
    void force();

    /** It reads a `boolean` at that offset. */
    boolean get(ValueLayout.OfBoolean layout, long offset);

    /** It writes a `boolean` at that offset. */
    void set(ValueLayout.OfBoolean layout, long offset, boolean value);

    /** It reads `boolean` number `index`: the offset is `index * layout.byteSize()`. */
    boolean getAtIndex(ValueLayout.OfBoolean layout, long index);

    /** It writes `boolean` number `index`. */
    void setAtIndex(ValueLayout.OfBoolean layout, long index, boolean value);

    /** It reads a `byte` at that offset. */
    byte get(ValueLayout.OfByte layout, long offset);

    /** It writes a `byte` at that offset. */
    void set(ValueLayout.OfByte layout, long offset, byte value);

    /** It reads `byte` number `index`: the offset is `index * layout.byteSize()`. */
    byte getAtIndex(ValueLayout.OfByte layout, long index);

    /** It writes `byte` number `index`. */
    void setAtIndex(ValueLayout.OfByte layout, long index, byte value);

    /** It reads a `char` at that offset. */
    char get(ValueLayout.OfChar layout, long offset);

    /** It writes a `char` at that offset. */
    void set(ValueLayout.OfChar layout, long offset, char value);

    /** It reads `char` number `index`: the offset is `index * layout.byteSize()`. */
    char getAtIndex(ValueLayout.OfChar layout, long index);

    /** It writes `char` number `index`. */
    void setAtIndex(ValueLayout.OfChar layout, long index, char value);

    /** It reads a `short` at that offset. */
    short get(ValueLayout.OfShort layout, long offset);

    /** It writes a `short` at that offset. */
    void set(ValueLayout.OfShort layout, long offset, short value);

    /** It reads `short` number `index`: the offset is `index * layout.byteSize()`. */
    short getAtIndex(ValueLayout.OfShort layout, long index);

    /** It writes `short` number `index`. */
    void setAtIndex(ValueLayout.OfShort layout, long index, short value);

    /** It reads an `int` at that offset. */
    int get(ValueLayout.OfInt layout, long offset);

    /** It writes an `int` at that offset. */
    void set(ValueLayout.OfInt layout, long offset, int value);

    /** It reads `int` number `index`: the offset is `index * layout.byteSize()`. */
    int getAtIndex(ValueLayout.OfInt layout, long index);

    /** It writes `int` number `index`. */
    void setAtIndex(ValueLayout.OfInt layout, long index, int value);

    /** It reads a `long` at that offset. */
    long get(ValueLayout.OfLong layout, long offset);

    /** It writes a `long` at that offset. */
    void set(ValueLayout.OfLong layout, long offset, long value);

    /** It reads `long` number `index`: the offset is `index * layout.byteSize()`. */
    long getAtIndex(ValueLayout.OfLong layout, long index);

    /** It writes `long` number `index`. */
    void setAtIndex(ValueLayout.OfLong layout, long index, long value);

    /** It reads a `float` at that offset. */
    float get(ValueLayout.OfFloat layout, long offset);

    /** It writes a `float` at that offset. */
    void set(ValueLayout.OfFloat layout, long offset, float value);

    /** It reads `float` number `index`: the offset is `index * layout.byteSize()`. */
    float getAtIndex(ValueLayout.OfFloat layout, long index);

    /** It writes `float` number `index`. */
    void setAtIndex(ValueLayout.OfFloat layout, long index, float value);

    /** It reads a `double` at that offset. */
    double get(ValueLayout.OfDouble layout, long offset);

    /** It writes a `double` at that offset. */
    void set(ValueLayout.OfDouble layout, long offset, double value);

    /** It reads `double` number `index`: the offset is `index * layout.byteSize()`. */
    double getAtIndex(ValueLayout.OfDouble layout, long index);

    /** It writes `double` number `index`. */
    void setAtIndex(ValueLayout.OfDouble layout, long index, double value);

    /** It reads an address at that offset, as a zero-length segment. */
    MemorySegment get(AddressLayout layout, long offset);

    /** It writes `value`'s address at that offset. */
    void set(AddressLayout layout, long offset, MemorySegment value);

    /** It reads address number `index`. */
    MemorySegment getAtIndex(AddressLayout layout, long index);

    /** It writes address number `index`. */
    void setAtIndex(AddressLayout layout, long index, MemorySegment value);

    /** The contents as a `byte[]`, read with that layout. */
    byte[] toArray(ValueLayout.OfByte elementLayout);

    /** The contents as a `char[]`, read with that layout. */
    char[] toArray(ValueLayout.OfChar elementLayout);

    /** The contents as a `short[]`, read with that layout. */
    short[] toArray(ValueLayout.OfShort elementLayout);

    /** The contents as a `int[]`, read with that layout. */
    int[] toArray(ValueLayout.OfInt elementLayout);

    /** The contents as a `long[]`, read with that layout. */
    long[] toArray(ValueLayout.OfLong elementLayout);

    /** The contents as a `float[]`, read with that layout. */
    float[] toArray(ValueLayout.OfFloat elementLayout);

    /** The contents as a `double[]`, read with that layout. */
    double[] toArray(ValueLayout.OfDouble elementLayout);

    /**
     * The null segment: address zero, length zero.
     *
     * <p>Length zero and not "invalid": it is what a C `NULL` **is** -- an address that cannot be
     * read. Any access fails on bounds, which is the right answer.
     */
    MemorySegment NULL = HeapSegment.nullSegment();

    /** A zero-length segment at that address. */
    static MemorySegment ofAddress(long address) {
        return HeapSegment.atAddress(address);
    }

    /** A segment over a {@link ByteBuffer}. */
    static MemorySegment ofBuffer(java.nio.Buffer buffer) {
        return HeapSegment.fromBuffer(buffer);
    }

    /** A segment over that array. Maximum alignment: 1. */
    static MemorySegment ofArray(byte[] arr) {
        return HeapSegment.ofArray0(arr, 1);
    }

    /** A segment over that array. Maximum alignment: 2. */
    static MemorySegment ofArray(char[] arr) {
        return HeapSegment.ofArray0(arr, 2);
    }

    /** A segment over that array. Maximum alignment: 2. */
    static MemorySegment ofArray(short[] arr) {
        return HeapSegment.ofArray0(arr, 2);
    }

    /** A segment over that array. Maximum alignment: 4. */
    static MemorySegment ofArray(int[] arr) {
        return HeapSegment.ofArray0(arr, 4);
    }

    /** A segment over that array. Maximum alignment: 8. */
    static MemorySegment ofArray(long[] arr) {
        return HeapSegment.ofArray0(arr, 8);
    }

    /** A segment over that array. Maximum alignment: 4. */
    static MemorySegment ofArray(float[] arr) {
        return HeapSegment.ofArray0(arr, 4);
    }

    /** A segment over that array. Maximum alignment: 8. */
    static MemorySegment ofArray(double[] arr) {
        return HeapSegment.ofArray0(arr, 8);
    }

    /**
     * The first byte where those two ranges differ, relative to each one's start.
     *
     * <p>It is **static** and not an instance method, unlike the one-argument version, and the reason
     * reads off the signature: here the two segments come in on an equal footing, each with its own
     * range. Neither of the two is "this".
     */
    static long mismatch(MemorySegment srcSegment, long srcFromOffset, long srcToOffset,
            MemorySegment dstSegment, long dstFromOffset, long dstToOffset) {
        return HeapSegment.differenceBetween(srcSegment, srcFromOffset, srcToOffset, dstSegment,
                dstFromOffset, dstToOffset);
    }

    /** It copies between two segments. */
    static void copy(MemorySegment srcSegment, long srcOffset, MemorySegment dstSegment,
            long dstOffset, long bytes) {
        HeapSegment.copyRange(srcSegment, srcOffset, dstSegment, dstOffset, bytes);
    }

    /** It copies between two segments, element by element according to the layouts. */
    static void copy(MemorySegment srcSegment, ValueLayout srcElementLayout, long srcOffset,
            MemorySegment dstSegment, ValueLayout dstElementLayout, long dstOffset,
            long elementCount) {
        HeapSegment.copyElements(srcSegment, srcElementLayout, srcOffset, dstSegment,
                dstElementLayout, dstOffset, elementCount);
    }

    /** It copies from a Java array into a segment. */
    static void copy(Object srcArray, int srcIndex, MemorySegment dstSegment,
            ValueLayout dstLayout, long dstOffset, int elementCount) {
        HeapSegment.copyFromArray(srcArray, srcIndex, dstSegment, dstLayout, dstOffset,
                elementCount);
    }

    /** It copies from a segment into a Java array. */
    static void copy(MemorySegment srcSegment, ValueLayout srcLayout, long srcOffset,
            Object dstArray, int dstIndex, int elementCount) {
        HeapSegment.copyToArray(srcSegment, srcLayout, srcOffset, dstArray, dstIndex,
                elementCount);
    }

    /**
     * A segment's lifetime scope.
     *
     * <p>It is what turns a memory error into an exception: a segment whose scope has closed cannot
     * be used, and the attempt fails with the exact line instead of reading garbage.
     */
    interface Scope {

        /** Whether it is still open. */
        boolean isAlive();
    }
}
