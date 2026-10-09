package java.lang.foreign;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;

// The implementation of `MemorySegment` over a Java array. Package-private: it is reached through
// `MemorySegment.ofArray` or through an `Arena`.
//
// **All the access goes through two methods**, `read(i)` and `write(i, v)`, which work byte by byte
// over the backing array whatever its type. Everything else --the 36 get/set methods-- is built on
// top by composing or decomposing bytes according to the layout's order.
//
// That shape is on purpose and it is what makes the class reviewable: the only sharp part --how byte
// number i maps to an element of an `int[]`-- is in a single place, and if it is right it is right
// for all eight types. The alternative, one path per backing type per value type, is fifty-six paths
// that have to be believed one at a time.
//
// The price is speed, and here it does not matter: this library exists to be correct and readable.
final class HeapSegment implements MemorySegment {

    // The backing array, or `null` for the ones that have none (NULL and the `ofAddress` ones).
    private final Object base;
    // Bytes per array element: 1, 2, 4 or 8. It is what translates an offset in bytes into a
    // position in the array.
    private final int elemSize;
    // Where this segment starts **inside the backing**, in bytes.
    private final long start;
    private final long length;
    private final boolean readOnly;
    private final SegmentScope scope;

    HeapSegment(Object base, int elemSize, long start, long length, boolean readOnly,
            SegmentScope scope) {
        this.base = base;
        this.elemSize = elemSize;
        this.start = start;
        this.length = length;
        this.readOnly = readOnly;
        this.scope = scope;
    }

    // ---- factories ----------------------------------------------------------------------------

    static MemorySegment nullSegment() {
        return new HeapSegment(null, 1, 0L, 0L, true, SegmentScope.GLOBAL);
    }

    static MemorySegment atAddress(long address) {
        // Length zero, as in the JDK: a loose address does not say how much can be read from there,
        // and assuming would be inventing. It is enlarged with `reinterpret`, which cannot be done
        // here because there is no memory behind it -- see its javadoc.
        return new HeapSegment(null, 1, address, 0L, false, SegmentScope.GLOBAL);
    }

    static MemorySegment ofArray0(Object arr, int elemSize) {
        if (arr == null) {
            throw new NullPointerException("arr");
        }
        long n = HeapSegment.lengthOf(arr) * (long) elemSize;
        return new HeapSegment(arr, elemSize, 0L, n, false, SegmentScope.GLOBAL);
    }

    static MemorySegment fromBuffer(java.nio.Buffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (buffer instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) buffer;
            if (bb.hasArray()) {
                byte[] arr = bb.array();
                HeapSegment s = new HeapSegment(arr, 1, (long) (bb.arrayOffset() + bb.position()),
                        (long) bb.remaining(), bb.isReadOnly(), SegmentScope.GLOBAL);
                return s;
            }
        }
        // A buffer with no accessible array is a direct one, and those live outside the heap. It is
        // said to the caller's face instead of copying: copying would give a segment that is **not**
        // a view of the buffer, and the whole use of this is that it be a view.
        throw new UnsupportedOperationException(
                "a segment can only be made over a buffer with an accessible array");
    }

    // ---- the elementary bits ------------------------------------------------------------------

    public long byteSize() {
        return this.length;
    }

    public long address() {
        return this.start;
    }

    public Optional<Object> heapBase() {
        return Optional.ofNullable(this.base);
    }

    public boolean isNative() {
        return this.base == null;
    }

    public boolean isMapped() {
        return false;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public long maxByteAlignment() {
        // The array element's. A `byte[]` gives 1 and that is why `JAVA_INT` does not fit: the JVM
        // makes no promise about where an array of bytes falls.
        return (long) this.elemSize;
    }

    public MemorySegment.Scope scope() {
        return this.scope;
    }

    public boolean isAccessibleBy(Thread thread) {
        return true;
    }

    public MemorySegment asReadOnly() {
        return new HeapSegment(this.base, this.elemSize, this.start, this.length, true,
                this.scope);
    }

    // ---- slices -------------------------------------------------------------------------------

    public MemorySegment asSlice(long offset) {
        return this.asSlice(offset, this.length - offset);
    }

    public MemorySegment asSlice(long offset, long newSize) {
        this.requireRange(offset, newSize);
        return new HeapSegment(this.base, this.elemSize, this.start + offset, newSize,
                this.readOnly, this.scope);
    }

    public MemorySegment asSlice(long offset, long newSize, long byteAlignment) {
        Layouts.requireAlignment(byteAlignment);
        if ((this.start + offset) % byteAlignment != 0L) {
            throw new IllegalArgumentException(
                    "the slice at " + offset + " does not respect the alignment " + byteAlignment);
        }
        return this.asSlice(offset, newSize);
    }

    public MemorySegment asSlice(long offset, MemoryLayout layout) {
        return this.asSlice(offset, layout.byteSize(), layout.byteAlignment());
    }

    public Optional<MemorySegment> asOverlappingSlice(MemorySegment other) {
        if (!(other instanceof HeapSegment)) {
            return Optional.empty();
        }
        HeapSegment o = (HeapSegment) other;
        // Two different arrays **never** overlap, even if their offsets coincide. Comparing by
        // identity and not by contents is the only correct thing here.
        if (this.base == null || this.base != o.base) {
            return Optional.empty();
        }
        long from = Math.max(this.start, o.start);
        long to = Math.min(this.start + this.length, o.start + o.length);
        if (from >= to) {
            return Optional.empty();
        }
        return Optional.of(new HeapSegment(this.base, this.elemSize, from, to - from,
                this.readOnly, this.scope));
    }

    public MemorySegment reinterpret(long newSize) {
        if (this.base == null) {
            throw new UnsupportedOperationException(
                    "a segment with no memory behind it cannot be enlarged: it would cover bytes"
                            + " nobody can read");
        }
        long available = HeapSegment.lengthOf(this.base) * (long) this.elemSize;
        if (newSize < 0L || this.start + newSize > available) {
            throw new IllegalArgumentException(
                    "size " + newSize + " runs off the backing array");
        }
        return new HeapSegment(this.base, this.elemSize, this.start, newSize, this.readOnly,
                this.scope);
    }

    public MemorySegment reinterpret(Arena arena, java.util.function.Consumer<MemorySegment> cleanup) {
        return this.reinterpret(this.length);
    }

    public MemorySegment reinterpret(long newSize, Arena arena,
            java.util.function.Consumer<MemorySegment> cleanup) {
        return this.reinterpret(newSize);
    }

    // ---- the byte-by-byte access, which is the heart of the class -----------------------------
    //
    // An `int[]` in memory is a strip of bytes: byte j of element k is byte (k*4 + j) of the strip.
    // Reading it is pulling that byte out of the `int`, and on a little-endian machine byte 0 is the
    // lowest -- which is what the shift below does.
    //
    // KajiJDK's VM and the platforms it runs on are little-endian, and that assumption is here and
    // nowhere else: if some day there is a big-endian one, this is the only method that changes.

    private byte read(long i) {
        long abs = this.start + i;
        if (this.base instanceof byte[]) {
            return ((byte[]) this.base)[(int) abs];
        }
        int idx = (int) (abs / (long) this.elemSize);
        int shift = (int) (abs % (long) this.elemSize) * 8;
        long word = this.word(idx);
        return (byte) ((word >>> shift) & 0xFFL);
    }

    private void write(long i, byte v) {
        long abs = this.start + i;
        if (this.base instanceof byte[]) {
            ((byte[]) this.base)[(int) abs] = v;
            return;
        }
        int idx = (int) (abs / (long) this.elemSize);
        int shift = (int) (abs % (long) this.elemSize) * 8;
        long word = this.word(idx);
        long mask = ~(0xFFL << shift);
        long fresh = (word & mask) | (((long) v & 0xFFL) << shift);
        this.storeWord(idx, fresh);
    }

    // How many elements the backing array has.
    //
    // By a type switch and not through `java.lang.reflect.Array.getLength`, which does work: the
    // switch is what the rest of the class already does, so going out to reflection for this one
    // answer would be the odd thing.
    static long lengthOf(Object arr) {
        if (arr instanceof byte[]) {
            return (long) ((byte[]) arr).length;
        }
        if (arr instanceof char[]) {
            return (long) ((char[]) arr).length;
        }
        if (arr instanceof short[]) {
            return (long) ((short[]) arr).length;
        }
        if (arr instanceof int[]) {
            return (long) ((int[]) arr).length;
        }
        if (arr instanceof long[]) {
            return (long) ((long[]) arr).length;
        }
        if (arr instanceof float[]) {
            return (long) ((float[]) arr).length;
        }
        if (arr instanceof double[]) {
            return (long) ((double[]) arr).length;
        }
        throw new IllegalArgumentException("not a supported array of primitives");
    }

    // The backing's element `idx`, seen as the bits it takes up.
    private long word(int idx) {
        if (this.base instanceof char[]) {
            return (long) ((char[]) this.base)[idx];
        }
        if (this.base instanceof short[]) {
            return (long) ((short[]) this.base)[idx] & 0xFFFFL;
        }
        if (this.base instanceof int[]) {
            return (long) ((int[]) this.base)[idx] & 0xFFFFFFFFL;
        }
        if (this.base instanceof long[]) {
            return ((long[]) this.base)[idx];
        }
        if (this.base instanceof float[]) {
            return (long) Float.floatToRawIntBits(((float[]) this.base)[idx]) & 0xFFFFFFFFL;
        }
        if (this.base instanceof double[]) {
            return Double.doubleToRawLongBits(((double[]) this.base)[idx]);
        }
        throw new UnsupportedOperationException("unsupported backing");
    }

    private void storeWord(int idx, long v) {
        if (this.base instanceof char[]) {
            ((char[]) this.base)[idx] = (char) v;
            return;
        }
        if (this.base instanceof short[]) {
            ((short[]) this.base)[idx] = (short) v;
            return;
        }
        if (this.base instanceof int[]) {
            ((int[]) this.base)[idx] = (int) v;
            return;
        }
        if (this.base instanceof long[]) {
            ((long[]) this.base)[idx] = v;
            return;
        }
        if (this.base instanceof float[]) {
            ((float[]) this.base)[idx] = Float.intBitsToFloat((int) v);
            return;
        }
        if (this.base instanceof double[]) {
            ((double[]) this.base)[idx] = Double.longBitsToDouble(v);
            return;
        }
        throw new UnsupportedOperationException("unsupported backing");
    }

    // ---- composing and decomposing multi-byte values ------------------------------------------

    private long readN(long offset, int n, boolean bigEndian) {
        long v = 0L;
        int i = 0;
        while (i < n) {
            int pos = bigEndian ? i : n - 1 - i;
            v = (v << 8) | ((long) this.read(offset + (long) pos) & 0xFFL);
            i = i + 1;
        }
        return v;
    }

    private void writeN(long offset, int n, boolean bigEndian, long v) {
        int i = 0;
        while (i < n) {
            int pos = bigEndian ? n - 1 - i : i;
            this.write(offset + (long) pos, (byte) ((v >>> (8 * i)) & 0xFFL));
            i = i + 1;
        }
    }

    // ---- checks -------------------------------------------------------------------------------
    //
    // All three are done **before** touching anything, and in this order: first whether the segment
    // is still alive, then whether the range fits, and last the alignment. The order matters for the
    // message: on a closed segment, "out of range" would send the reader looking in the wrong
    // place.

    private void requireAlive() {
        if (!this.scope.isAlive()) {
            throw new IllegalStateException("this segment's scope has already closed");
        }
    }

    private void requireRange(long offset, long n) {
        if (offset < 0L || n < 0L || offset + n > this.length) {
            throw new IndexOutOfBoundsException(
                    "the range [" + offset + ", " + (offset + n) + ") runs off a segment of "
                            + this.length + " bytes");
        }
    }

    private void requireReadable(ValueLayout layout, long offset) {
        this.requireAlive();
        this.requireRange(offset, layout.byteSize());
        long a = layout.byteAlignment();
        if (a > (long) this.elemSize) {
            throw new IllegalArgumentException(
                    "layout " + layout + " demands alignment " + a + " and this segment only"
                            + " guarantees " + this.elemSize + "; use the UNALIGNED variant");
        }
        if ((this.start + offset) % a != 0L) {
            throw new IllegalArgumentException(
                    "offset " + offset + " does not respect the alignment " + a + " of " + layout);
        }
    }

    private void requireWritable(ValueLayout layout, long offset) {
        if (this.readOnly) {
            // `IllegalArgumentException` and not `UnsupportedOperationException`: it is what the
            // JDK throws, and changing it would force catching two different things depending on the
            // VM.
            throw new IllegalArgumentException("the segment is read-only");
        }
        this.requireReadable(layout, offset);
    }

    private static boolean isBig(ValueLayout layout) {
        return layout.order() == ByteOrder.BIG_ENDIAN;
    }

    // ---- the 36 get/set methods, generated ----------------------------------------------------
    //
    // They are generated because they are nearly identical and writing them by hand is where a
    // `Short` slips in where a `Char` belonged. The four steps are always the same: check, read the
    // bytes, compose the value, convert it to the Java type.

    public boolean get(ValueLayout.OfBoolean layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 1, HeapSegment.isBig(layout));
        return raw != 0L;
    }

    public void set(ValueLayout.OfBoolean layout, long offset, boolean value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 1, HeapSegment.isBig(layout), value ? 1L : 0L);
    }

    public boolean getAtIndex(ValueLayout.OfBoolean layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfBoolean layout, long index, boolean value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public byte get(ValueLayout.OfByte layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 1, HeapSegment.isBig(layout));
        return (byte) raw;
    }

    public void set(ValueLayout.OfByte layout, long offset, byte value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 1, HeapSegment.isBig(layout), (long) value);
    }

    public byte getAtIndex(ValueLayout.OfByte layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfByte layout, long index, byte value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public char get(ValueLayout.OfChar layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 2, HeapSegment.isBig(layout));
        return (char) raw;
    }

    public void set(ValueLayout.OfChar layout, long offset, char value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 2, HeapSegment.isBig(layout), (long) value);
    }

    public char getAtIndex(ValueLayout.OfChar layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfChar layout, long index, char value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public short get(ValueLayout.OfShort layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 2, HeapSegment.isBig(layout));
        return (short) raw;
    }

    public void set(ValueLayout.OfShort layout, long offset, short value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 2, HeapSegment.isBig(layout), (long) value);
    }

    public short getAtIndex(ValueLayout.OfShort layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfShort layout, long index, short value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public int get(ValueLayout.OfInt layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 4, HeapSegment.isBig(layout));
        return (int) raw;
    }

    public void set(ValueLayout.OfInt layout, long offset, int value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 4, HeapSegment.isBig(layout), (long) value);
    }

    public int getAtIndex(ValueLayout.OfInt layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfInt layout, long index, int value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public long get(ValueLayout.OfLong layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 8, HeapSegment.isBig(layout));
        return raw;
    }

    public void set(ValueLayout.OfLong layout, long offset, long value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 8, HeapSegment.isBig(layout), value);
    }

    public long getAtIndex(ValueLayout.OfLong layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfLong layout, long index, long value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public float get(ValueLayout.OfFloat layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 4, HeapSegment.isBig(layout));
        return Float.intBitsToFloat((int) raw);
    }

    public void set(ValueLayout.OfFloat layout, long offset, float value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 4, HeapSegment.isBig(layout), (long) Float.floatToRawIntBits(value));
    }

    public float getAtIndex(ValueLayout.OfFloat layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfFloat layout, long index, float value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public double get(ValueLayout.OfDouble layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 8, HeapSegment.isBig(layout));
        return Double.longBitsToDouble(raw);
    }

    public void set(ValueLayout.OfDouble layout, long offset, double value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 8, HeapSegment.isBig(layout), Double.doubleToRawLongBits(value));
    }

    public double getAtIndex(ValueLayout.OfDouble layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfDouble layout, long index, double value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    // The address is read as a `long` and returned as a zero-length segment: that is what a loose
    // address **is**, and enlarging it is the reader's decision.
    public MemorySegment get(AddressLayout layout, long offset) {
        this.requireReadable(layout, offset);
        long raw = this.readN(offset, 8, HeapSegment.isBig(layout));
        return MemorySegment.ofAddress(raw);
    }

    public void set(AddressLayout layout, long offset, MemorySegment value) {
        this.requireWritable(layout, offset);
        this.writeN(offset, 8, HeapSegment.isBig(layout),
                value == null ? 0L : value.address());
    }

    public MemorySegment getAtIndex(AddressLayout layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(AddressLayout layout, long index, MemorySegment value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public byte[] toArray(ValueLayout.OfByte elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        byte[] out = new byte[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public char[] toArray(ValueLayout.OfChar elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        char[] out = new char[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public short[] toArray(ValueLayout.OfShort elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        short[] out = new short[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public int[] toArray(ValueLayout.OfInt elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        int[] out = new int[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public long[] toArray(ValueLayout.OfLong elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        long[] out = new long[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public float[] toArray(ValueLayout.OfFloat elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        float[] out = new float[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public double[] toArray(ValueLayout.OfDouble elementLayout) {
        this.requireAlive();
        if (this.length % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "the segment of " + this.length + " bytes is not a multiple of " + elementLayout);
        }
        int n = (int) (this.length / elementLayout.byteSize());
        double[] out = new double[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    // ---- copying, filling, comparing -----------------------------------------------------------

    public MemorySegment copyFrom(MemorySegment src) {
        HeapSegment.copyRange(src, 0L, this, 0L, src.byteSize());
        return this;
    }

    public MemorySegment fill(byte value) {
        this.requireAlive();
        if (this.readOnly) {
            throw new IllegalArgumentException("the segment is read-only");
        }
        long i = 0L;
        while (i < this.length) {
            this.write(i, value);
            i = i + 1L;
        }
        return this;
    }

    public long mismatch(MemorySegment other) {
        return HeapSegment.differenceBetween(this, 0L, this.length, other, 0L, other.byteSize());
    }

    static long differenceBetween(MemorySegment a, long fromA, long toA, MemorySegment b,
            long fromB, long toB) {
        long nA = toA - fromA;
        long nB = toB - fromB;
        long common = Math.min(nA, nB);
        long i = 0L;
        while (i < common) {
            if (HeapSegment.byteAt(a, fromA + i) != HeapSegment.byteAt(b, fromB + i)) {
                return i;
            }
            i = i + 1L;
        }
        // If one is a prefix of the other, they "differ" where the short one ends. And if they are
        // the same length and everything matches, they differ nowhere: -1.
        return nA == nB ? -1L : common;
    }

    // ---- strings ------------------------------------------------------------------------------

    public String getString(long offset) {
        return this.getString(offset, StandardCharsets.UTF_8);
    }

    public String getString(long offset, Charset charset) {
        this.requireAlive();
        // The zero is looked for **first** and the decoding comes after: a C string ends where the
        // zero is, not where the segment is.
        long end = offset;
        while (end < this.length && this.read(end) != 0) {
            end = end + 1L;
        }
        if (end >= this.length) {
            throw new IndexOutOfBoundsException(
                    "there is no zero terminator from offset " + offset);
        }
        byte[] raw = new byte[(int) (end - offset)];
        int i = 0;
        while (i < raw.length) {
            raw[i] = this.read(offset + (long) i);
            i = i + 1;
        }
        return new String(raw, charset);
    }

    public void setString(long offset, String str) {
        this.setString(offset, str, StandardCharsets.UTF_8);
    }

    public void setString(long offset, String str, Charset charset) {
        this.requireAlive();
        if (this.readOnly) {
            throw new IllegalArgumentException("the segment is read-only");
        }
        byte[] raw = str.getBytes(charset);
        // The +1 is the terminator, and it is not optional: without it, `getString` would read up to
        // whatever next zero happened to be around.
        this.requireRange(offset, (long) raw.length + 1L);
        int i = 0;
        while (i < raw.length) {
            this.write(offset + (long) i, raw[i]);
            i = i + 1;
        }
        this.write(offset + (long) raw.length, (byte) 0);
    }

    // ---- views --------------------------------------------------------------------------------

    public ByteBuffer asByteBuffer() {
        this.requireAlive();
        // It copies, and that has to be said: in the JDK this is a **view** and writes are seen from
        // both sides. Here only a `byte[]` can be wrapped without copying, and this segment may sit
        // over any array. Copying and saying so is better than wrapping sometimes and sometimes
        // not.
        byte[] copy = new byte[(int) this.length];
        long i = 0L;
        while (i < this.length) {
            copy[(int) i] = this.read(i);
            i = i + 1L;
        }
        ByteBuffer bb = ByteBuffer.wrap(copy);
        return this.readOnly ? bb.asReadOnlyBuffer() : bb;
    }

    public Stream<MemorySegment> elements(MemoryLayout elementLayout) {
        List<MemorySegment> parts = new ArrayList<MemorySegment>();
        long sz = elementLayout.byteSize();
        if (sz <= 0L) {
            throw new IllegalArgumentException("the element has to take up something");
        }
        long i = 0L;
        while (i + sz <= this.length) {
            parts.add(this.asSlice(i, sz));
            i = i + sz;
        }
        return parts.stream();
    }

    public Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout) {
        return this.elements(elementLayout).spliterator();
    }

    // ---- the four mapped-file ones -------------------------------------------------------------

    public boolean isLoaded() {
        return false;
    }

    public void load() {
    }

    public void unload() {
    }

    public void force() {
    }

    // ---- identity -----------------------------------------------------------------------------
    //
    // Two segments are equal if they are **the same region**: same backing, same start, same length.
    // The contents are not compared, and that is deliberate -- two different regions with the same
    // bytes are not the same memory, and confusing them would be the worst possible error here.

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HeapSegment)) {
            return false;
        }
        HeapSegment o = (HeapSegment) obj;
        return this.base == o.base && this.start == o.start && this.length == o.length;
    }

    public int hashCode() {
        int h = this.base == null ? 0 : System.identityHashCode(this.base);
        h = h * 31 + (int) this.start;
        return h * 31 + (int) this.length;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MemorySegment{ kind: ");
        sb.append(this.base == null ? "native" : "heap");
        sb.append(", address: 0x");
        sb.append(Long.toHexString(this.start));
        sb.append(", byteSize: ");
        sb.append(this.length);
        sb.append(" }");
        return sb.toString();
    }

    // ---- the statics the interface delegates to ------------------------------------------------

    static byte byteAt(MemorySegment s, long i) {
        if (s instanceof HeapSegment) {
            return ((HeapSegment) s).read(i);
        }
        return s.get(ValueLayout.JAVA_BYTE, i);
    }

    static void copyRange(MemorySegment src, long srcOffset, MemorySegment dst, long dstOffset,
            long bytes) {
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("the destination is read-only");
        }
        // Back to front when they overlap and the destination comes after: otherwise the bytes still
        // waiting to be read get overwritten. It is the same care `System.arraycopy` takes.
        boolean backwards = src == dst && dstOffset > srcOffset;
        long i = backwards ? bytes - 1L : 0L;
        while (backwards ? i >= 0L : i < bytes) {
            byte b = HeapSegment.byteAt(src, srcOffset + i);
            HeapSegment.writeAt(dst, dstOffset + i, b);
            i = backwards ? i - 1L : i + 1L;
        }
    }

    private static void writeAt(MemorySegment s, long i, byte v) {
        if (s instanceof HeapSegment) {
            ((HeapSegment) s).requireAlive();
            ((HeapSegment) s).requireRange(i, 1L);
            ((HeapSegment) s).write(i, v);
            return;
        }
        s.set(ValueLayout.JAVA_BYTE, i, v);
    }

    static void copyElements(MemorySegment src, ValueLayout srcLayout, long srcOffset,
            MemorySegment dst, ValueLayout dstLayout, long dstOffset, long elementCount) {
        if (srcLayout.carrier() != dstLayout.carrier()) {
            throw new IllegalArgumentException(
                    "both layouts have to carry the same type");
        }
        // Element by element and not byte by byte: the two layouts may have **different byte
        // orders**, and a raw copy would leave the destination the wrong way round.
        long i = 0L;
        while (i < elementCount) {
            long fromOff = srcOffset + i * srcLayout.byteSize();
            long aOff = dstOffset + i * dstLayout.byteSize();
            long raw = HeapSegment.readRaw(src, srcLayout, fromOff);
            HeapSegment.writeRaw(dst, dstLayout, aOff, raw);
            i = i + 1L;
        }
    }

    private static long readRaw(MemorySegment s, ValueLayout l, long off) {
        HeapSegment h = (HeapSegment) s;
        h.requireReadable(l, off);
        return h.readN(off, (int) l.byteSize(), HeapSegment.isBig(l));
    }

    private static void writeRaw(MemorySegment s, ValueLayout l, long off, long v) {
        HeapSegment h = (HeapSegment) s;
        h.requireWritable(l, off);
        h.writeN(off, (int) l.byteSize(), HeapSegment.isBig(l), v);
    }

    static void copyFromArray(Object srcArray, int srcIndex, MemorySegment dst,
            ValueLayout dstLayout, long dstOffset, int elementCount) {
        int i = 0;
        while (i < elementCount) {
            long raw = HeapSegment.rawElement(srcArray, srcIndex + i);
            HeapSegment.writeRaw(dst, dstLayout, dstOffset + (long) i * dstLayout.byteSize(),
                    raw);
            i = i + 1;
        }
    }

    static void copyToArray(MemorySegment src, ValueLayout srcLayout, long srcOffset,
            Object dstArray, int dstIndex, int elementCount) {
        int i = 0;
        while (i < elementCount) {
            long raw = HeapSegment.readRaw(src, srcLayout,
                    srcOffset + (long) i * srcLayout.byteSize());
            HeapSegment.storeRawElement(dstArray, dstIndex + i, raw);
            i = i + 1;
        }
    }

    // The two below do what `Array.get`/`Array.set` would do, by a type switch and without boxing.
    // See `lengthOf`'s note.
    private static long rawElement(Object arr, int i) {
        if (arr instanceof byte[]) {
            return (long) ((byte[]) arr)[i];
        }
        if (arr instanceof char[]) {
            return (long) ((char[]) arr)[i];
        }
        if (arr instanceof short[]) {
            return (long) ((short[]) arr)[i];
        }
        if (arr instanceof int[]) {
            return (long) ((int[]) arr)[i];
        }
        if (arr instanceof long[]) {
            return ((long[]) arr)[i];
        }
        if (arr instanceof float[]) {
            return (long) Float.floatToRawIntBits(((float[]) arr)[i]);
        }
        if (arr instanceof double[]) {
            return Double.doubleToRawLongBits(((double[]) arr)[i]);
        }
        throw new IllegalArgumentException("not a supported array of primitives");
    }

    private static void storeRawElement(Object arr, int i, long raw) {
        if (arr instanceof byte[]) {
            ((byte[]) arr)[i] = (byte) raw;
            return;
        }
        if (arr instanceof char[]) {
            ((char[]) arr)[i] = (char) raw;
            return;
        }
        if (arr instanceof short[]) {
            ((short[]) arr)[i] = (short) raw;
            return;
        }
        if (arr instanceof int[]) {
            ((int[]) arr)[i] = (int) raw;
            return;
        }
        if (arr instanceof long[]) {
            ((long[]) arr)[i] = raw;
            return;
        }
        if (arr instanceof float[]) {
            ((float[]) arr)[i] = Float.intBitsToFloat((int) raw);
            return;
        }
        if (arr instanceof double[]) {
            ((double[]) arr)[i] = Double.longBitsToDouble(raw);
            return;
        }
        throw new IllegalArgumentException("not a supported array of primitives");
    }

}

// A segment's scope: a switch saying whether it can still be used.
//
// It is what turns a memory error into an exception. With no scope, using a segment after its arena
// closed would read whatever happened to be left there; with a scope it fails on the spot and with
// the exact line.
final class SegmentScope implements MemorySegment.Scope {

    // The one for the segments that never close: the `ofArray` ones, and the null one. A Java array
    // is freed by the collector when nobody is looking, so there is nothing to close.
    static final SegmentScope GLOBAL = new SegmentScope();

    private boolean alive = true;

    public boolean isAlive() {
        return this.alive;
    }

    void close0() {
        this.alive = false;
    }
}
