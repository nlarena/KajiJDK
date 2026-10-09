package java.sql;

/**
 * KajiLibrary's java.sql.RowId -- the address of a row inside the database.
 *
 * <p>It is the fastest identifier there is to get back to a row --faster than the primary key,
 * because it is usually the physical position-- and for that very reason the least reliable: **it
 * is not stable**. It can change if the row moves, and its lifetime depends on the database. It
 * serves within a transaction, not for storing.
 *
 * <p>It declares `equals`, `hashCode` and `toString` even though they come from `Object`, to state
 * their contract. This note said that is the way to require that they really be implemented; it is
 * not: `Object`'s methods already satisfy those declarations, so an implementation that does not
 * override them still compiles, and compares by identity.
 */
public interface RowId {

    /** Whether both identify the same row. */
    boolean equals(Object obj);

    /** The bytes of the identifier. */
    byte[] getBytes();

    /** A readable form. */
    String toString();

    int hashCode();
}
