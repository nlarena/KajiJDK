package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.Optional;
import java.util.function.Function;

// The low-level view of a `.class` being read: the byte array with access by offset, plus the pool
// already built. It is what an {@link AttributeMapper} receives in order to interpret an attribute's
// body, and it is the only place in the API where absolute offsets are spoken of.
//
// Every `read*` validates the range before touching the array and throws `ConstantPoolException` if
// the offset falls outside the file. That decision is deliberate: a reader returning garbage for a
// truncated file is worse than one that fails.
public interface ClassReader extends ConstantPool {

    /** The custom attribute mappers registered when the file was opened. */
    Function<Utf8Entry, AttributeMapper<?>> customAttributes();

    /** The class's `access_flags`, raw. */
    int flags();

    /** The `this_class` entry. */
    ClassEntry thisClassEntry();

    /** The `super_class` entry; empty if the index is 0. */
    Optional<ClassEntry> superclassEntry();

    /** The file's length in bytes. */
    int classfileLength();

    /** The entry whose index is the u2 sitting at `offset`. */
    PoolEntry readEntry(int offset);

    /** Like `readEntry`, demanding that the entry be of class `cls`. */
    <T extends PoolEntry> T readEntry(int offset, Class<T> cls);

    /** Like `readEntry`, but it returns `null` if the index is 0. */
    PoolEntry readEntryOrNull(int offset);

    /** Like `readEntry(int, Class)`, but it returns `null` if the index is 0. */
    <T extends PoolEntry> T readEntryOrNull(int offset, Class<T> cls);

    /** The unsigned byte at `offset`. */
    int readU1(int offset);

    /** The two unsigned bytes at `offset`. */
    int readU2(int offset);

    /** The signed byte at `offset`. */
    int readS1(int offset);

    /** The two signed bytes at `offset`. */
    int readS2(int offset);

    /** The four bytes at `offset`, as an `int`. */
    int readInt(int offset);

    /** The eight bytes at `offset`, as a `long`. */
    long readLong(int offset);

    /** The four bytes at `offset`, as a `float`. */
    float readFloat(int offset);

    /** The eight bytes at `offset`, as a `double`. */
    double readDouble(int offset);

    /** A copy of `len` bytes starting at `offset`. */
    byte[] readBytes(int offset, int len);

    /** It copies `len` bytes from `offset` into `buf`. */
    void copyBytesTo(BufWriter buf, int offset, int len);
}
