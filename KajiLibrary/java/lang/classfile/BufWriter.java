package java.lang.classfile;

import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.PoolEntry;

// The buffer where a `.class` is built: a growing array of bytes, plus the constant pool the indices
// being written point into. It is big-endian throughout, like the format.
public interface BufWriter {

    /** The pool the indices written here are resolved against. */
    ConstantPoolBuilder constantPool();

    /** Whether `constantPool`'s indices can be written as they stand. */
    boolean canWriteDirect(ConstantPool constantPool);

    /** It asks for room for `freeBytes` more bytes. It is an optimisation; it changes no content. */
    void reserveSpace(int freeBytes);

    /** It writes one byte. */
    void writeU1(int x);

    /** It writes two bytes, big-endian. */
    void writeU2(int x);

    /** It writes four bytes, big-endian. */
    void writeInt(int x);

    /** It writes a `float` in its four-byte IEEE 754 form. */
    void writeFloat(float x);

    /** It writes eight bytes, big-endian. */
    void writeLong(long x);

    /** It writes a `double` in its eight-byte IEEE 754 form. */
    void writeDouble(double x);

    /** It writes the whole array. */
    void writeBytes(byte[] arr);

    /** It writes `length` bytes starting at `offset`. */
    void writeBytes(byte[] arr, int offset, int length);

    /** It overwrites `intSize` bytes at `offset` with `value`. It is what closes an
     * `attribute_length`. */
    void patchInt(int offset, int intSize, int value);

    /** It writes `value`'s low `intSize` bytes. */
    void writeIntBytes(int intSize, long value);

    /** It writes `entry`'s index as a u2. */
    void writeIndex(PoolEntry entry);

    /** Like `writeIndex`, but a `null` is written as index 0. */
    void writeIndexOrZero(PoolEntry entry);

    /** How many bytes it has written so far. */
    int size();
}
