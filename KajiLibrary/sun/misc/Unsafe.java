package sun.misc;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Direct access to memory and to the layout of objects, bypassing the type system.
 *
 * <h2>Why a class like this exists</h2>
 *
 * <p>Because the Java library implements itself. {@code java.util.concurrent} needs
 * compare-and-swap; a direct {@code ByteBuffer} needs memory off the heap; deserialisation needs to
 * create an object without calling its constructor. None of that can be written in Java, and this
 * class is the hole through which the VM offers it.
 *
 * <p>It was never public API: it lives in {@code sun.misc}, the package that by convention means
 * "not for you". That the {@code jdk.unsupported} module exports it is an acknowledgement of
 * reality --half the industry depends on it-- and not a promise.
 *
 * <h2>Why {@link #getUnsafe} fails</h2>
 *
 * <p>It fails in the JDK as well: it throws {@code SecurityException} unless the caller was loaded
 * by the boot or the platform loader. That is why everybody gets it by reflection over the {@code
 * theUnsafe} field instead of calling this method.
 *
 * <p>Here it throws for every caller, and that is not an addition: it is the behaviour the class
 * has for any application code, which is the only code there is to call it here.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The eighty-odd methods throw {@link UnsupportedOperationException} naming what is missing.
 * (The note said "a hundred and something"; there are 87 methods, and every one but {@link
 * #getUnsafe} throws this.) It is not laziness: each of them <strong>is</strong> an operation of
 * the VM. Reading a field by its offset in bytes, reserving memory off the heap, creating an object
 * with no constructor or parking a thread are not things that can be implemented in Java --if they
 * could be, this class would not exist--.
 *
 * <p>The alternative would be to return zeros and write nothing, and that is exactly the case the
 * house avoids: a {@code compareAndSwapInt} that answers {@code true} without having changed
 * anything produces silently corrupt concurrent structures.
 *
 * <p>What is there is the <strong>shape</strong>: the class, the {@code theUnsafe} field with its
 * exact name, and the signatures. Code that uses it compiles; the day the VM has the primitives,
 * the bodies are filled in without anybody having to recompile against something else.
 */
public final class Unsafe {

    private static final String NOT_THERE =
            "this operation is a primitive of the VM (access to memory by offset, reservation off "
            + "the heap, creation with no constructor or parking of threads) and this VM does not "
            + "expose it";

    /**
     * What {@link #staticFieldOffset} returns for a field that has no offset.
     *
     * <p>It is the only constant of this class with a value that means something: the others
     * describe the memory layout of this VM, and this VM does not expose it.
     */
    public static final int INVALID_FIELD_OFFSET = -1;

    // The constants of the layout of arrays.
    //
    // In the JDK they are not compile-time constants: they are computed when the class is
    // initialised by asking the VM how it lays out each type of array. Here they are computed as
    // well --in a static block, not as literals-- so that they do not get inlined into whoever
    // reads them, which is what would happen if they were a bare `= 0`.
    //
    // They are ZERO, and zero is neither a real offset nor a real scale: no Java array starts at
    // byte 0 of its own object, because the header comes first. So the value reads as "not known"
    // and not as a datum.
    //
    // That this cannot cause silent damage is no accident: EVERY method that would consume these
    // numbers --the `get`/`put` by offset-- throws. There is no way for a computation made with
    // them to end up reading the wrong memory, because it never gets to read it.

    /** The offset of the first element of a {@code boolean[]}; see the note above. */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET;

    /** The offset of the first element of a {@code byte[]}; see the note above. */
    public static final int ARRAY_BYTE_BASE_OFFSET;

    /** The offset of the first element of a {@code short[]}; see the note above. */
    public static final int ARRAY_SHORT_BASE_OFFSET;

    /** The offset of the first element of a {@code char[]}; see the note above. */
    public static final int ARRAY_CHAR_BASE_OFFSET;

    /** The offset of the first element of a {@code int[]}; see the note above. */
    public static final int ARRAY_INT_BASE_OFFSET;

    /** The offset of the first element of a {@code long[]}; see the note above. */
    public static final int ARRAY_LONG_BASE_OFFSET;

    /** The offset of the first element of a {@code float[]}; see the note above. */
    public static final int ARRAY_FLOAT_BASE_OFFSET;

    /** The offset of the first element of a {@code double[]}; see the note above. */
    public static final int ARRAY_DOUBLE_BASE_OFFSET;

    /** The offset of the first element of a {@code Object[]}; see the note above. */
    public static final int ARRAY_OBJECT_BASE_OFFSET;

    /** How many bytes each element of a {@code boolean[]} takes up; see the note above. */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE;

    /** How many bytes each element of a {@code byte[]} takes up; see the note above. */
    public static final int ARRAY_BYTE_INDEX_SCALE;

    /** How many bytes each element of a {@code short[]} takes up; see the note above. */
    public static final int ARRAY_SHORT_INDEX_SCALE;

    /** How many bytes each element of a {@code char[]} takes up; see the note above. */
    public static final int ARRAY_CHAR_INDEX_SCALE;

    /** How many bytes each element of a {@code int[]} takes up; see the note above. */
    public static final int ARRAY_INT_INDEX_SCALE;

    /** How many bytes each element of a {@code long[]} takes up; see the note above. */
    public static final int ARRAY_LONG_INDEX_SCALE;

    /** How many bytes each element of a {@code float[]} takes up; see the note above. */
    public static final int ARRAY_FLOAT_INDEX_SCALE;

    /** How many bytes each element of a {@code double[]} takes up; see the note above. */
    public static final int ARRAY_DOUBLE_INDEX_SCALE;

    /** How many bytes each element of a {@code Object[]} takes up; see the note above. */
    public static final int ARRAY_OBJECT_INDEX_SCALE;

    /** The size of a pointer in this VM, in bytes; see the note above. */
    public static final int ADDRESS_SIZE;

    static {
        // A static block and not literals: that way they are not inlined into whoever reads them,
        // just as in the JDK, where they come from asking the VM.
        final int unknown = 0;
        ARRAY_BOOLEAN_BASE_OFFSET = unknown;
        ARRAY_BYTE_BASE_OFFSET = unknown;
        ARRAY_SHORT_BASE_OFFSET = unknown;
        ARRAY_CHAR_BASE_OFFSET = unknown;
        ARRAY_INT_BASE_OFFSET = unknown;
        ARRAY_LONG_BASE_OFFSET = unknown;
        ARRAY_FLOAT_BASE_OFFSET = unknown;
        ARRAY_DOUBLE_BASE_OFFSET = unknown;
        ARRAY_OBJECT_BASE_OFFSET = unknown;
        ARRAY_BOOLEAN_INDEX_SCALE = unknown;
        ARRAY_BYTE_INDEX_SCALE = unknown;
        ARRAY_SHORT_INDEX_SCALE = unknown;
        ARRAY_CHAR_INDEX_SCALE = unknown;
        ARRAY_INT_INDEX_SCALE = unknown;
        ARRAY_LONG_INDEX_SCALE = unknown;
        ARRAY_FLOAT_INDEX_SCALE = unknown;
        ARRAY_DOUBLE_INDEX_SCALE = unknown;
        ARRAY_OBJECT_INDEX_SCALE = unknown;
        ADDRESS_SIZE = unknown;
    }

    /**
     * The only instance.
     *
     * <p>Private and with that name on purpose: it is the field the whole ecosystem reaches by
     * reflection, and renaming it would break more code than taking the class away.
     */
    private static final Unsafe theUnsafe = new Unsafe();

    private Unsafe() {
    }


    /**
     * The instance, in the JDK, when the caller was loaded by the boot or the platform loader.
     *
     * <p>The note said this returns the instance to callers loaded by the system loader. It does
     * not: here it throws for every caller, because only application code can reach it.
     *
     * @return it does not return
     * @throws SecurityException always; the JDK throws it for any caller outside the boot and
     *     platform loaders
     */
    public static Unsafe getUnsafe() {
        throw new SecurityException("Unsafe");
    }


    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int getInt(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putInt(Object o, long offset, int x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public Object getObject(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putObject(Object o, long offset, Object x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public boolean getBoolean(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putBoolean(Object o, long offset, boolean x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public byte getByte(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putByte(Object o, long offset, byte x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public short getShort(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putShort(Object o, long offset, short x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public char getChar(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putChar(Object o, long offset, char x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long getLong(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putLong(Object o, long offset, long x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public float getFloat(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putFloat(Object o, long offset, float x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public double getDouble(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a field by its offset in bytes, bypassing the type system and the control of
     * access.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putDouble(Object o, long offset, double x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public byte getByte(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putByte(long address, byte x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public short getShort(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putShort(long address, short x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public char getChar(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putChar(long address, char x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int getInt(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putInt(long address, int x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long getLong(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putLong(long address, long x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public float getFloat(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putFloat(long address, float x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads a value at an absolute memory address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public double getDouble(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It writes a value at an absolute memory address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putDouble(long address, double x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads or writes a native pointer at an absolute address.
     *
     * @param address the memory address
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long getAddress(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads or writes a native pointer at an absolute address.
     *
     * @param address the memory address
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putAddress(long address, long x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reserves memory off the heap, asked of the system and not of the collector: whoever
     * asks for it has to free it.
     *
     * @param bytes how many bytes
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long allocateMemory(long bytes) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It resizes a block reserved with {@link #allocateMemory}; the caller still has to free it.
     *
     * @param address the memory address
     * @param bytes how many bytes
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long reallocateMemory(long address, long bytes) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It fills a run of memory with one byte value.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param bytes how many bytes
     * @param value the byte to fill with
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void setMemory(Object o, long offset, long bytes, byte value) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It fills a run of memory with one byte value.
     *
     * @param address the memory address
     * @param bytes how many bytes
     * @param value the byte to fill with
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void setMemory(long address, long bytes, byte value) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It copies a run of memory from one place to another.
     *
     * @param srcBase the source object, or {@code null}
     * @param srcOffset the source offset
     * @param destBase the destination object, or {@code null}
     * @param destOffset the destination offset
     * @param bytes how many bytes
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset,
            long bytes) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It copies a run of memory from one place to another.
     *
     * @param srcAddress the source address
     * @param destAddress the destination address
     * @param bytes how many bytes
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It frees a block reserved with {@link #allocateMemory}.
     *
     * @param address the memory address
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void freeMemory(long address) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It finds out the memory layout the VM chose: it turns a {@code Field} into the offset
     * the accesses by offset need.
     *
     * @param f the field
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long objectFieldOffset(Field f) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It finds out the memory layout the VM chose: it turns a {@code Field} into the offset
     * the accesses by offset need.
     *
     * @param f the field
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long staticFieldOffset(Field f) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The object the static fields of the class of that {@code Field} live in, which the
     * accesses by offset take as their base.
     *
     * @param f the field
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public Object staticFieldBase(Field f) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The offset of the first element of an array of that class, inside the array object.
     *
     * @param arrayClass the class of the array
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int arrayBaseOffset(Class<?> arrayClass) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * How many bytes each element of an array of that class takes up.
     *
     * @param arrayClass the class of the array
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int arrayIndexScale(Class<?> arrayClass) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The size of a native pointer, in bytes.
     *
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int addressSize() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The size of a page of memory of the system, in bytes.
     *
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int pageSize() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It creates an instance <strong>without calling any constructor</strong>. It is how the
     * serialisation frameworks rebuild an object without running its initialisation.
     *
     * @param cls the class to instantiate
     * @return it does not return
     * @throws InstantiationException declared by the signature; it is never thrown
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public Object allocateInstance(Class<?> cls) throws InstantiationException {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It throws a checked exception without declaring it, breaking the check of the compiler on
     * purpose.
     *
     * @param ee the exception to throw
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void throwException(Throwable ee) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It compares and swaps in one single indivisible step. It is the primitive all of
     * {@code java.util.concurrent} is built on.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param expected the value one expects to find
     * @param x the value
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It compares and swaps in one single indivisible step. It is the primitive all of
     * {@code java.util.concurrent} is built on.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param expected the value one expects to find
     * @param x the value
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final boolean compareAndSwapInt(Object o, long offset, int expected, int x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It compares and swaps in one single indivisible step. It is the primitive all of
     * {@code java.util.concurrent} is built on.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param expected the value one expects to find
     * @param x the value
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final boolean compareAndSwapLong(Object o, long offset, long expected, long x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public Object getObjectVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putObjectVolatile(Object o, long offset, Object x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int getIntVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putIntVolatile(Object o, long offset, int x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public boolean getBooleanVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putBooleanVolatile(Object o, long offset, boolean x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public byte getByteVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putByteVolatile(Object o, long offset, byte x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public short getShortVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putShortVolatile(Object o, long offset, short x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public char getCharVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putCharVolatile(Object o, long offset, char x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public long getLongVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putLongVolatile(Object o, long offset, long x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public float getFloatVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putFloatVolatile(Object o, long offset, float x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public double getDoubleVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Like the ordinary access, but with {@code volatile} semantics: the reading sees the last
     * thing written by any thread and the writing becomes visible to all of them.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putDoubleVolatile(Object o, long offset, double x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A writing with a release barrier but no acquire barrier: cheaper than the
     * {@code volatile} one, and enough when the only thing that matters is that what was written
     * before is seen before.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putOrderedObject(Object o, long offset, Object x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A writing with a release barrier but no acquire barrier: cheaper than the
     * {@code volatile} one, and enough when the only thing that matters is that what was written
     * before is seen before.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putOrderedInt(Object o, long offset, int x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A writing with a release barrier but no acquire barrier: cheaper than the
     * {@code volatile} one, and enough when the only thing that matters is that what was written
     * before is seen before.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param x the value
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void putOrderedLong(Object o, long offset, long x) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It wakes up a thread parked with {@link #park}. It is the primitive {@code LockSupport} rests
     * on.
     *
     * @param thread the thread to wake up
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void unpark(Object thread) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It parks the current thread until it is woken up or the deadline passes. It is the
     * blocking primitive {@code LockSupport} rests on and, above it, every lock of the library.
     *
     * @param isAbsolute whether the deadline is an absolute instant or a relative time
     * @param time the deadline
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void park(boolean isAbsolute, long time) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The load average of the system, the same the operating system reports.
     *
     * @param loadavg the array where to leave the averages
     * @param nelems how many averages to ask for
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public int getLoadAverage(double[] loadavg, int nelems) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads and modifies in one single indivisible step, returning the previous value.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param delta how much to add
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final int getAndAddInt(Object o, long offset, int delta) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads and modifies in one single indivisible step, returning the previous value.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param delta how much to add
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final long getAndAddLong(Object o, long offset, long delta) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads and modifies in one single indivisible step, returning the previous value.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param newValue the new value
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final int getAndSetInt(Object o, long offset, int newValue) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads and modifies in one single indivisible step, returning the previous value.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param newValue the new value
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final long getAndSetLong(Object o, long offset, long newValue) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It reads and modifies in one single indivisible step, returning the previous value.
     *
     * @param o the object, or {@code null} for an absolute address
     * @param offset the offset in bytes inside the object
     * @param newValue the new value
     * @return it does not return
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A memory barrier: no reading before it is reordered with a reading or writing after it.
     *
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void loadFence() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A memory barrier: no writing before it is reordered with a reading or writing after it.
     *
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void storeFence() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A full memory barrier: no access before it is reordered with an access after it.
     *
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void fullFence() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It frees the memory of a direct {@code ByteBuffer} without waiting for the collector.
     *
     * @param directBuffer the direct buffer
     * @throws UnsupportedOperationException always; see the note of the class
     */
    public void invokeCleaner(java.nio.ByteBuffer directBuffer) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

}
