package java.nio.channels;

import java.io.IOException;

/**
 * KajiLibrary's java.nio.channels.FileLock — a lock over a stretch of file.
 *
 * <p>A lock is a **range**, not a file: `[position, position+size)`, and it can be shared --several
 * readers at a time-- or exclusive. That the range can go beyond the end of the file is not an
 * oversight of the design but what allows reserving beforehand the zone one is going to write in,
 * before it exists.
 *
 * <p>The lock is taken by **the whole VM**, not by the thread: two threads of this process that ask
 * for the same range do not exclude each other, they exclude **other processes**. It is the classic
 * source of confusion with this class and that is why it is written first.
 *
 * <h2>Where one is obtained</h2>
 *
 * <p>From {@link FileChannel#lock} and {@link FileChannel#tryLock}. This note used to say that
 * neither was there, because the only thing implementable over this VM would have been a lock
 * between the threads of this process --that is, the **opposite** guarantee to the one the name
 * promises--, and that without those two methods nobody could believe themselves protected by
 * something that did not protect them. The VM grew {@code Fs.lock} —{@code LockFileEx} on Windows,
 * {@code fcntl} on Unix— and with it the lock excludes other processes, which is what this class is
 * for; both methods are there.
 *
 * <p>What declaring it contributes besides: it is an abstract type whose contract can be read and
 * understood, and its computable parts --{@link #overlaps}, {@link #position()}, {@link #size()},
 * {@link #isShared()}-- are really implemented, because they are arithmetic over the fields and
 * depend on no native. Whoever implements a file system of their own inherits from here and only
 * has to put in {@link #isValid()} and {@link #release()}.
 *
 * <h2>What was left out</h2>
 *
 * <p><strong>Nothing.</strong> The two constructors and the nine public methods are here.
 */
public abstract class FileLock implements AutoCloseable {

    // The channel is kept as a `Channel` and not as a `FileChannel` because the two constructors
    // accept different hierarchies; `channel()` and `acquiredBy()` are the two views of this field.
    private final Channel lockedChannel;
    private final long pos;
    private final long len;
    private final boolean sharedFlag;

    /**
     * For a lock over a file channel.
     *
     * @throws IllegalArgumentException if the range is negative or overflows
     */
    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        check(position, size);
        if (channel == null) {
            throw new NullPointerException();
        }
        this.lockedChannel = channel;
        this.pos = position;
        this.len = size;
        this.sharedFlag = shared;

    }

    /**
     * For a lock over an asynchronous file channel.
     *
     * <p>It exists apart from the other one because {@link AsynchronousFileChannel} does not
     * inherit from {@link FileChannel}: they are two different hierarchies that give onto the same
     * file.
     */
    protected FileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
        check(position, size);
        if (channel == null) {
            throw new NullPointerException();
        }
        this.lockedChannel = channel;
        this.pos = position;
        this.len = size;
        this.sharedFlag = shared;

    }

    private static void check(long position, long size) {
        if (position < 0) {
            throw new IllegalArgumentException("negative position");
        }
        if (size < 0) {
            throw new IllegalArgumentException("negative size");
        }
        // The overflow is caught here and not when comparing ranges: a `position+size` that turns
        // the sign round would turn a huge lock into one that overlaps with nothing.
        if (position + size < 0) {
            throw new IllegalArgumentException("the range overflows");
        }
    }

    /**
     * The channel it was taken over, or `null` if it was an {@link AsynchronousFileChannel}.
     *
     * <p>Returning `null` in that case is what the JDK does, and it is uncomfortable but coherent:
     * the return type is {@link FileChannel} and an asynchronous channel is not one. {@link
     * #acquiredBy()} is the way of asking the same thing without surprises.
     */
    public final FileChannel channel() {
        if (this.lockedChannel instanceof FileChannel) {
            return (FileChannel) this.lockedChannel;
        }
        return null;
    }

    /** The channel it was taken over, whatever its type. */
    public Channel acquiredBy() {
        return this.lockedChannel;
    }

    /** Where the locked stretch starts. */
    public final long position() {
        return this.pos;
    }

    /**
     * How many bytes it covers.
     *
     * <p>It can go beyond the end of the file, and then the size of the lock does not change even
     * if the file grows: what was reserved was reserved.
     */
    public final long size() {
        return this.len;
    }

    /** Whether it is shared; if not, it is exclusive. */
    public final boolean isShared() {
        return this.sharedFlag;
    }

    /** Whether this lock and the given range step on at least one byte in common. */
    public final boolean overlaps(long position, long size) {
        if (position + size <= this.pos) {
            return false;
        }
        if (this.pos + this.len <= position) {
            return false;
        }
        return true;
    }

    /**
     * Whether the lock is still valid.
     *
     * <p>It stops being so on releasing it, on closing the channel, or on the VM shutting down.
     */
    public abstract boolean isValid();

    /** Releases the lock. Over an already invalid one it does nothing. */
    public abstract void release() throws IOException;

    /**
     * The same as {@link #release()}, for `try`-with-resources.
     *
     * <p>It is the reason the class implements `AutoCloseable`: a lock one forgets to release
     * blocks the others until the process dies.
     */
    public final void close() throws IOException {
        this.release();
    }

    public final String toString() {
        String mode;
        if (this.sharedFlag) {
            mode = "shared";
        } else {
            mode = "exclusive";
        }
        String state;
        if (this.isValid()) {
            state = "valid";
        } else {
            state = "invalid";
        }
        return "FileLock[" + mode + " " + this.pos + ":" + this.len + " " + state + "]";
    }
}
