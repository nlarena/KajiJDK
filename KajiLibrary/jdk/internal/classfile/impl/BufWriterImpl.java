package jdk.internal.classfile.impl;

import java.lang.classfile.BufWriter;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.PoolEntry;

/**
 * The byte buffer a `.class` is written into.
 *
 * <p>It grows by itself, in powers of two: doubling is what makes writing N bytes cost O(N) in
 * total and not O(N^2). Nothing more than that.
 *
 * <p><strong>`patchInt` is the reason this is an array and not a stream.</strong> Several
 * structures of the format carry up front a length that is known only after writing the contents
 * --that of an attribute, that of `code`-- and the only way of not walking everything twice is to
 * leave the gap, go on, and come back to fill it.
 */
public final class BufWriterImpl implements BufWriter {

    private final ConstantPoolBuilder pool;
    private byte[] buf;
    private int size;

    /** A buffer that writes indices against that pool. */
    public BufWriterImpl(ConstantPoolBuilder pool) {
        this.pool = pool;
        this.buf = new byte[1024];
        this.size = 0;
    }

    public ConstantPoolBuilder constantPool() {
        return this.pool;
    }

    /**
     * Whether an entry of that pool can be written by its index as it stands.
     *
     * <p>Only if it is **the same** pool: an index of another pool names something else. Whoever
     * writes a foreign entry has to adopt it first.
     */
    public boolean canWriteDirect(ConstantPool other) {
        return this.pool == other;
    }

    /** It reserves room for at least that many more bytes. */
    public void reserveSpace(int freeBytes) {
        this.grow(this.size + freeBytes);
    }

    private void grow(int needed) {
        if (needed <= this.buf.length) {
            return;
        }
        int n = this.buf.length;
        while (n < needed) {
            n = n * 2;
        }
        byte[] grown = new byte[n];
        System.arraycopy(this.buf, 0, grown, 0, this.size);
        this.buf = grown;
    }

    public void writeU1(int x) {
        this.grow(this.size + 1);
        this.buf[this.size] = (byte) x;
        this.size = this.size + 1;
    }

    public void writeU2(int x) {
        this.grow(this.size + 2);
        this.buf[this.size] = (byte) (x >> 8);
        this.buf[this.size + 1] = (byte) x;
        this.size = this.size + 2;
    }

    public void writeInt(int x) {
        this.grow(this.size + 4);
        this.buf[this.size] = (byte) (x >> 24);
        this.buf[this.size + 1] = (byte) (x >> 16);
        this.buf[this.size + 2] = (byte) (x >> 8);
        this.buf[this.size + 3] = (byte) x;
        this.size = this.size + 4;
    }

    public void writeLong(long x) {
        this.writeInt((int) (x >> 32));
        this.writeInt((int) x);
    }

    public void writeFloat(float x) {
        this.writeInt(Float.floatToRawIntBits(x));
    }

    public void writeDouble(double x) {
        this.writeLong(Double.doubleToRawLongBits(x));
    }

    public void writeBytes(byte[] arr) {
        this.writeBytes(arr, 0, arr.length);
    }

    public void writeBytes(byte[] arr, int offset, int length) {
        this.grow(this.size + length);
        System.arraycopy(arr, offset, this.buf, this.size, length);
        this.size = this.size + length;
    }

    /** It rewrites `intSize` bytes at `offset` with that value. See the class note. */
    public void patchInt(int offset, int intSize, int value) {
        for (int i = 0; i < intSize; i++) {
            this.buf[offset + i] = (byte) (value >> ((intSize - 1 - i) * 8));
        }
    }

    /** It writes that value in `intSize` bytes. */
    public void writeIntBytes(int intSize, long value) {
        for (int i = 0; i < intSize; i++) {
            this.writeU1((int) (value >> ((intSize - 1 - i) * 8)));
        }
    }

    /**
     * The index of that entry, in two bytes.
     *
     * <p>If the entry comes from **another** pool it is adopted first. Without that, transforming a
     * class writes the original pool's indices into the new file: the `.class` comes out well
     * formed and points at anything, so it does not fail on writing but on reading, with a message
     * that says nothing about the place where the error was.
     */
    public void writeIndex(PoolEntry entry) {
        if (entry == null) {
            throw new NullPointerException("there is no pool entry to write");
        }
        this.writeU2(this.adopt(entry).index());
    }

    /** The index of that entry, or zero if it is `null`. */
    public void writeIndexOrZero(PoolEntry entry) {
        this.writeU2(entry == null ? 0 : this.adopt(entry).index());
    }

    /**
     * The index that entry has **in this pool**, adopting it if it came from another.
     *
     * <p>Whoever writes the index in a single byte (`ldc`) needs it, where `writeIndex` does not
     * serve. Every write of an index has to go through here or through `writeIndex`: writing
     * `entry.index()` bare is what produces a file that points at the wrong pool.
     */
    public int indexOf(PoolEntry entry) {
        return this.adopt(entry).index();
    }

    private PoolEntry adopt(PoolEntry entry) {
        if (entry.constantPool() == this.pool) {
            return entry;
        }
        if (this.pool instanceof ConstantPoolBuilderImpl) {
            return ((ConstantPoolBuilderImpl) this.pool).adoptEntry(entry);
        }
        // A pool that is not ours and that does not know how to adopt: there is no way of
        // translating the index, and writing it as it stands would be writing a file that lies.
        throw new IllegalArgumentException(
                "the entry comes from another pool and this one cannot adopt it: " + entry);
    }

    public int size() {
        return this.size;
    }

    /** The written bytes, in an array of exactly the right size. */
    public byte[] toByteArray() {
        byte[] out = new byte[this.size];
        System.arraycopy(this.buf, 0, out, 0, this.size);
        return out;
    }
}
