package java.lang.foreign;

// The implementation of `Arena` over Java arrays. Package-private: it is reached through `Arena`'s
// four statics.
//
// Each reservation is backed by **an array of its own**, of the type the alignment asked for gives.
// Handing out slices of one big block would be closer to what the JDK does, but it cannot be done: a
// Java array only guarantees **its element's** alignment, so a `long` in the middle of a `byte[]`
// would not be aligned no matter what the offset arithmetic said. One array per reservation is what
// makes the promised alignment true.
//
// The cost is one object per reservation, which for the use this package has here --describing and
// building structures, not handling megabytes-- is irrelevant next to correctness.
final class HeapArena implements Arena {

    // The global one never closes, and that is why there is only one.
    private static final HeapArena GLOBAL = new HeapArena(true);

    private final boolean global;
    private final SegmentScope scope;

    private HeapArena(boolean global) {
        this.global = global;
        this.scope = global ? SegmentScope.GLOBAL : new SegmentScope();
    }

    static Arena theGlobal() {
        return GLOBAL;
    }

    static Arena fresh() {
        return new HeapArena(false);
    }

    public MemorySegment.Scope scope() {
        return this.scope;
    }

    public void close() {
        if (this.global) {
            throw new UnsupportedOperationException("the global arena does not close");
        }
        if (!this.scope.isAlive()) {
            throw new IllegalStateException("this arena was already closed");
        }
        this.scope.close0();
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0L) {
            throw new IllegalArgumentException("negative size: " + byteSize);
        }
        Layouts.requireAlignment(byteAlignment);
        if (!this.scope.isAlive()) {
            throw new IllegalStateException("this arena has already been closed");
        }
        if (byteAlignment > 8L) {
            // Rejected instead of faked. The widest element of a Java array is 8 bytes; promising
            // 16 and backing it with a `long[]` would be saying something is aligned when there is
            // no way of knowing.
            throw new UnsupportedOperationException(
                    "this library backs memory with Java arrays and cannot guarantee an alignment"
                            + " greater than 8: " + byteAlignment);
        }
        // The array's type is chosen by the alignment, not by the size: that is where the
        // guarantee comes from.
        if (byteAlignment == 8L) {
            return this.wrap(new long[(int) roundUp(byteSize, 8L)], 8);
        }
        if (byteAlignment == 4L) {
            return this.wrap(new int[(int) roundUp(byteSize, 4L)], 4);
        }
        if (byteAlignment == 2L) {
            return this.wrap(new short[(int) roundUp(byteSize, 2L)], 2);
        }
        return this.wrap(new byte[(int) byteSize], 1);
    }

    // How many elements of `elemSize` bytes are needed to cover `bytes`. It rounds up: a request of
    // 5 bytes with alignment 4 needs two `int`, not one.
    private static long roundUp(long bytes, long sz) {
        return (bytes + sz - 1L) / sz;
    }

    private MemorySegment wrap(Object arr, int elemSize) {
        long n = HeapSegment.lengthOf(arr) * (long) elemSize;
        return new HeapSegment(arr, elemSize, 0L, n, false, this.scope);
    }
}
