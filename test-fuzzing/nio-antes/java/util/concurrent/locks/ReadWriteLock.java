package java.util.concurrent.locks;

// A pair of locks for shared reading and exclusive writing: many threads may hold the
// read lock at once (while no writer holds), but the write lock is exclusive.
public interface ReadWriteLock {

    // The lock for reading (shared).
    Lock readLock();

    // The lock for writing (exclusive).
    Lock writeLock();
}
