package java.nio;

import jdk.internal.io.Fs;

// A byte buffer whose bytes are a region of a file, mapped by the operating system.
//
// ===============================================================================================
// WHAT MAKES THIS DIFFERENT FROM EVERY OTHER BUFFER HERE
// ===============================================================================================
//
// It has no array. Every other buffer in this library is backed by a `byte[]`; this one is backed by
// a mapping, and reaching a byte means asking the VM. That is why `ByteBuffer` grew storage hooks --
// `at`, `peek`, `poke`, `newBuffer` -- and why the implementation had to be lifted out of
// `HeapByteBuffer`: before that there was nowhere to put a buffer like this.
//
// The distinction is not decoration. A mapped buffer that was a copy would take writes and never
// deliver them to the file, silently, and that is exactly why `map()` was left out of `FileChannel`
// for as long as it was.
//
// ===============================================================================================
// SLICES SHARE THE MAPPING AND DO NOT OWN IT
// ===============================================================================================
//
// `slice` and `duplicate` hand back another buffer over the same token, and nothing here unmaps: a
// mapping lives until the VM exits. That is what the JDK does too -- its mapped buffers are unmapped
// by the garbage collector, and it says plainly that there is no way to ask for it sooner.
//
// Package-private on purpose: `MappedBuffers.of` is the way in, and `FileChannel.map` is the only
// caller of that.
final class MappedBuffer extends MappedByteBuffer {

    private final int token;

    MappedBuffer(int token, int offset, int capacity, boolean readOnly) {
        super(null, offset, capacity);
        this.token = token;
        this.isReadOnly = readOnly;
    }

    @Override
    byte at(int p) {
        return (byte) Fs.mapGet(this.token, p);
    }

    @Override
    void at(int p, byte b) {
        Fs.mapPut(this.token, p, b);
    }

    @Override
    ByteBuffer newBuffer(int storageOffset, int capacity) {
        return new MappedBuffer(this.token, storageOffset, capacity, this.isReadOnly);
    }

    @Override
    int mapToken() {
        return this.token;
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    /**
     * Bulk read, straight out of the mapping.
     *
     * <p>The inherited version walks byte by byte, and every one of those is a call into the VM.
     * This is the same work in one.
     */
    @Override
    public ByteBuffer get(byte[] dst, int off, int length) {
        Buffer.checkBounds(off, length, dst.length);
        final int from = nextGetIndex(length);
        if (!Fs.mapRead(this.token, ix(from), dst, off, length)) {
            throw new BufferUnderflowException();
        }
        return this;
    }

    /** Bulk write, straight into the mapping. See {@link #get(byte[], int, int)}. */
    @Override
    public ByteBuffer put(byte[] src, int off, int length) {
        checkWritable();
        Buffer.checkBounds(off, length, src.length);
        final int to = nextPutIndex(length);
        if (!Fs.mapWrite(this.token, ix(to), src, off, length)) {
            throw new BufferOverflowException();
        }
        return this;
    }

    // ---- the four that MappedByteBuffer narrows --------------------------------------------------
    //
    // `ByteBuffer` already does all four, and correctly, now that they go through `newBuffer`. They
    // are written again here only because `MappedByteBuffer` declares them returning a
    // `MappedByteBuffer`, and our javac has no `super.slice()` to delegate to.

    @Override
    public MappedByteBuffer slice() {
        final MappedBuffer b = new MappedBuffer(this.token, ix(position()), remaining(),
                this.isReadOnly);
        b.bigEndian = bigEndian;
        b.nativeByteOrder = nativeByteOrder;
        return b;
    }

    @Override
    public MappedByteBuffer slice(int index, int length) {
        checkIndex(index, length);
        final MappedBuffer b = new MappedBuffer(this.token, ix(index), length, this.isReadOnly);
        b.bigEndian = bigEndian;
        b.nativeByteOrder = nativeByteOrder;
        return b;
    }

    @Override
    public MappedByteBuffer duplicate() {
        final MappedBuffer b = new MappedBuffer(this.token, offset, capacity(), this.isReadOnly);
        b.position(position());
        b.limit(limit());
        b.bigEndian = bigEndian;
        b.nativeByteOrder = nativeByteOrder;
        return b;
    }

    /**
     * Returns the shift that turns an element index into a byte offset.
     *
     * @return zero — for a byte buffer the two are the same number
     */
    @Override
    int scaleShifts() {
        return 0;
    }

    @Override
    public MappedByteBuffer compact() {
        checkWritable();
        final int n = remaining();
        final int from = position();
        for (int i = 0; i < n; i++) {
            at(ix(i), at(ix(from + i)));
        }
        position(n);
        limit(capacity());
        return this;
    }
}
