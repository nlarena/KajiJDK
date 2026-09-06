package java.nio.channels;

import java.io.IOException;

// A file lock of this library: the token the VM handed back, and the file it was taken on.
//
// The token is all it takes to release it. The other side keeps the open descriptor that holds the
// lock -- a system lock lives as long as the descriptor that took it -- and `Fs.unlock` lets it
// fall.
//
// Package-private on purpose: `FileChannel.lock` and `AsynchronousFileChannel.lock` are the way in.
final class KajiFileLock extends FileLock {

    /**
     * The token of a lock that is reserved but not taken yet.
     *
     * <p>An asynchronous `lock` has to refuse an overlap **on the calling thread** -- that is what
     * the JDK does, and it has to, because the exception is thrown rather than delivered through the
     * future. So the region is claimed here first and the system lock is taken on the pool
     * afterwards. Between the two the lock is not valid, but it does block other overlaps: without
     * that the claim would be worth nothing.
     */
    static final int RESERVED = -2;

    private final String file;
    private int token;

    KajiFileLock(FileChannel channel, String file, long position, long size, boolean shared,
            int token) {
        super(channel, position, size, shared);
        this.file = file;
        this.token = token;
    }

    KajiFileLock(AsynchronousFileChannel channel, String file, long position, long size,
            boolean shared, int token) {
        super(channel, position, size, shared);
        this.file = file;
        this.token = token;
    }

    /** Which file it was taken on, in canonical form. */
    String file() {
        return this.file;
    }

    @Override
    public boolean isValid() {
        return this.token >= 0;
    }

    /** Whether this lock stands in the way of an overlapping request. See {@link #RESERVED}. */
    synchronized boolean blocksOverlap() {
        return this.token >= 0 || this.token == RESERVED;
    }

    /** Turns a reservation into a lock that was really taken. */
    synchronized void confirm(int token) {
        this.token = token;
    }

    /**
     * Releases the lock.
     *
     * <p>On one already invalid it does nothing, which is what the JDK does: releasing twice is not
     * an error, and treating it as one would make everybody using try-with-resources keep count.
     *
     * @throws IOException if the release fails
     */
    @Override
    public void release() throws IOException {
        final int t;
        synchronized (this) {
            if (this.token == -1) {
                return;
            }
            t = this.token;
            this.token = -1;
        }
        FileLockRegistry.remove(this);
        if (t >= 0) {
            jdk.internal.io.Fs.unlock(t);
        }
    }
}
