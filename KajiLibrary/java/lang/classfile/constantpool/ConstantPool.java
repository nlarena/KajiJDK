package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.util.Iterator;
import java.util.NoSuchElementException;

// A class's constant pool (JVMS §4.4), plus the `BootstrapMethods` attribute's table, which the API
// treats as a second half of the pool because `CONSTANT_Dynamic` and `CONSTANT_InvokeDynamic` index
// it just as they index the pool.
//
// `size()` is the file's `constant_pool_count`: one MORE than the highest usable index. Index 0 does
// not exist by definition of the format, and neither do the slots following a `long` or a `double`.
// The `iterator()` here skips both: it walks real entries, not slots.
public interface ConstantPool extends Iterable<PoolEntry> {

    /** The entry at `index`. It throws `ConstantPoolException` if the index is not a valid
     * entry. */
    PoolEntry entryByIndex(int index);

    /** The `constant_pool_count`: one more than the highest index. */
    int size();

    /**
     * The entry at `index`, demanding that it be of type `cls`. It throws `ConstantPoolException` if
     * the index is no good or if the entry is of another class -- which is the method's reason for
     * being: a reader that accepted the wrong entry here would let a malformed file through.
     */
    <T extends PoolEntry> T entryByIndex(int index, Class<T> cls);

    /** It walks the pool's real entries, in index order. */
    default Iterator<PoolEntry> iterator() {
        return new PoolIterator(this);
    }

    /** Entry `index` of the `BootstrapMethods` table. */
    BootstrapMethodEntry bootstrapMethodEntry(int index);

    /** How many bootstrap methods the class has. */
    int bootstrapMethodCount();
}

// The iterator of the `default` above. It is a package-private class and not an anonymous one
// because it has state --the index-- and because skipping the `long`/`double` gap reads better with a
// name.
final class PoolIterator implements Iterator<PoolEntry> {

    private final ConstantPool pool;
    private int index;

    PoolIterator(ConstantPool pool) {
        this.pool = pool;
        this.index = 1;
    }

    public boolean hasNext() {
        return this.index < this.pool.size();
    }

    public PoolEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        PoolEntry e = this.pool.entryByIndex(this.index);
        this.index += e.width();
        return e;
    }
}
