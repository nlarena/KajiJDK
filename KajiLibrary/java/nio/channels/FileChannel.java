package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 * KajiLibrary's java.nio.channels.FileChannel — a channel over a file.
 *
 * <p>It is the channel this library makes with its own hands, and the one whose {@link #open(Path,
 * OpenOption...)} goes straight to the file natives. The network ones are made too —this note used
 * to say the VM had no network natives, and it has them— but they go out through a provider; this
 * one does not.
 *
 * <h2>How it is made, which is what one has to know before using it</h2>
 *
 * <p>This VM does not open descriptors: the only thing underneath is "read the whole file" and
 * "write it whole" (`jdk.internal.io.Fs`). A channel over that can be built in two ways, and the
 * choice is made here in plain sight:
 *
 * <ul>
 *   <li><strong>With a cache</strong>: read once on opening, work in memory, dump on closing. Fast,
 *       and **a liar**: what was written is not on the disk until closing, and a program that falls
 *       over without closing left nothing behind. It is the deal `java.io.FileOutputStream` already
 *       makes in this tree, documented there.
 *   <li><strong>On the fly</strong>: each read reads the file and each write rewrites it.
 *       <strong>It is what this channel does.</strong>
 * </ul>
 *
 * <p>The second is O(n) per operation --writing a byte at the end of a one-megabyte file moves a
 * megabyte-- and even so it is the right one, because it is the only one in which the contract is
 * fulfilled: when {@link #write} returns, the bytes **are on the disk**; whatever another process
 * writes is seen in the next read; and a power cut does not erase what had been written already.
 * Speed is paid for not lying, which is the right trade. Whoever needs speed has
 * `java.io.BufferedOutputStream` over {@link Channels#newOutputStream}, where the buffer is their
 * own decision and not a surprise.
 *
 * <p>A pleasant consequence of the above: {@link #force} has nothing to do and `SYNC`/`DSYNC`
 * fulfil themselves. It is not that they are ignored, it is that they were there already.
 *
 * <p>What **cannot** be promised is atomicity: rewriting the whole file is not a single step, so
 * two simultaneous writers over the same file step on each other. The JDK does not guarantee
 * anything there either without locks, but its window is of bytes and the one here is of the whole
 * file.
 *
 * <h2>What was left out on purpose</h2>
 *
 * <p><strong>Locking is in, mapping is not.</strong> This note used to say that both were out
 * because the VM had no native for them, and that the only lock implementable here would exclude
 * the threads of this VM and nobody else -- the same name with the opposite guarantee. That was
 * true of the argument, not of the limit: the VM now has {@code Fs.lock}, which goes to {@code
 * LockFileEx} on Windows and {@code fcntl} on Unix, so the lock excludes **other processes**, which
 * is what a file lock is for.
 *
 * <p><strong>And mapping is in too.</strong> The reason it was out is worth keeping: a
 * {@link java.nio.MappedByteBuffer} that was a copy would mean writes to it never reached the file,
 * silently. What it took to not do that was two things -- the VM grew {@code Fs.mapOpen}, and
 * {@link java.nio.ByteBuffer} grew storage hooks so that a buffer need not be backed by an array.
 * Before the second one there was nowhere to put a buffer that reads a mapping.
 *
 * <p>{@link MapMode} was always here, because it is a value and not a promise: its three constants
 * can be named, compared and stored without anything lying. Now there is also a {@code map()} to
 * pass them to.
 */
public abstract class FileChannel extends AbstractInterruptibleChannel
        implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel {

    protected FileChannel() {
    }

    // ---- opening ---------------------------------------------------------------------------------

    /**
     * Opens a channel over `path`.
     *
     * <p>With no writing option, it is opened for reading. `options` accepts the same as
     * `java.nio.file.Files`: `READ`, `WRITE`, `APPEND`, `TRUNCATE_EXISTING`, `CREATE`,
     * `CREATE_NEW`, `DELETE_ON_CLOSE`, `SYNC`, `DSYNC` and `NOFOLLOW_LINKS`.
     *
     * <p>`SYNC` and `DSYNC` are accepted because they **are fulfilled**: this channel writes to the
     * disk on every `write`. `SPARSE` is rejected --no sparse files are made here-- just as in
     * `Files`, instead of being accepted as a hint: silently ignoring an option the caller set for
     * a reason is how one finds out too late that the file takes up what it should not have.
     *
     * @throws IllegalArgumentException if the options contradict each other (`READ` with `APPEND`,
     *         or `APPEND` with `TRUNCATE_EXISTING`)
     * @throws UnsupportedOperationException if an option this VM cannot honour is asked for
     * @throws java.nio.file.NoSuchFileException if it does not exist and creating it was not asked
     *     for
     * @throws java.nio.file.FileAlreadyExistsException with `CREATE_NEW` if it was there already
     */
    // ---- mapping ---------------------------------------------------------------------------------

    /**
     * Maps a region of this channel's file into memory.
     *
     * <p>The bytes are the file's, not a copy of them: in {@link MapMode#READ_WRITE} a write to the
     * buffer reaches the file and every other mapper of it, and that is the whole point. In
     * {@link MapMode#PRIVATE} it does not -- the mapping is copy-on-write and the changes stay in
     * this process.
     *
     * <p>The mapping outlives the channel. Closing the channel does not take it down, and neither
     * does anything else a caller can say: the JDK is explicit that there is no way to ask for an
     * unmap, and this behaves the same.
     *
     * @param mode how the mapping may be used
     * @param position the first byte of the file to map
     * @param size how many bytes
     * @return the mapped buffer
     * @throws IOException if the region cannot be mapped
     * @throws NonReadableChannelException if the channel was not opened for reading
     * @throws NonWritableChannelException if {@code READ_WRITE} is asked of a read-only channel
     * @throws IllegalArgumentException if the position or the size is negative, or the size is
     *     larger than an {@code int}
     */
    public abstract java.nio.MappedByteBuffer map(MapMode mode, long position, long size)
            throws IOException;

    /**
     * The same region as a {@link java.lang.foreign.MemorySegment} tied to that arena.
     *
     * @param mode how the mapping may be used
     * @param offset the first byte of the file to map
     * @param size how many bytes
     * @param arena the arena whose lifetime the segment follows
     * @return the segment
     * @throws IOException if the region cannot be mapped
     * @throws UnsupportedOperationException always in this library: a segment over a mapping needs
     *     the foreign-memory machinery, and {@code java.lang.foreign} here has no way to name an
     *     address that is not in the heap. {@link #map(MapMode, long, long)} is the way to a
     *     mapping
     */
    public java.lang.foreign.MemorySegment map(MapMode mode, long offset, long size,
            java.lang.foreign.Arena arena) throws IOException {
        throw new UnsupportedOperationException(
                "a MemorySegment over a mapping needs foreign memory, which this library does not"
                        + " have; use map(MapMode, long, long)");
    }

    // ---- locks -----------------------------------------------------------------------------------

    /**
     * Takes an exclusive lock over the whole file, waiting if it has to.
     *
     * @return the lock
     * @throws IOException if it cannot be taken
     */
    public final FileLock lock() throws IOException {
        return lock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Takes a lock over that region, waiting if it has to.
     *
     * <p>The lock belongs to the whole virtual machine and its point is to exclude **other
     * processes**. It is not the way to keep the threads of one program apart.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return the lock
     * @throws IOException if it cannot be taken
     * @throws OverlappingFileLockException if this VM already holds an overlapping region
     */
    public abstract FileLock lock(long position, long size, boolean shared) throws IOException;

    /**
     * Takes an exclusive lock over the whole file without waiting.
     *
     * @return the lock, or {@code null} when another process holds it
     * @throws IOException if it cannot be taken
     */
    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Takes a lock over that region without waiting.
     *
     * <p>Returning {@code null} and throwing mean different things: {@code null} is "somebody else
     * has it", and {@link OverlappingFileLockException} is "you already have it", which is a bug in
     * the caller and not a race.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return the lock, or {@code null} when another process holds it
     * @throws IOException if it cannot be taken
     * @throws OverlappingFileLockException if this VM already holds an overlapping region
     */
    public abstract FileLock tryLock(long position, long size, boolean shared) throws IOException;

    // ---- opening ---------------------------------------------------------------------------------

    public static FileChannel open(Path path, OpenOption... options) throws IOException {
        if (options == null) {
            throw new NullPointerException();
        }
        return KajiFileChannel.openFile(path, options);
    }

    /**
     * Like the other one, with the options in a set and initial attributes.
     *
     * <p>`attrs` **has to come empty**: this VM does not know how to set permissions or owner on
     * creating, and accepting attributes that are afterwards not applied would leave a file with
     * permissions other than the ones asked for without anybody noticing. It is rejected instead of
     * being ignored.
     *
     * @throws UnsupportedOperationException if `attrs` brings anything
     */
    public static FileChannel open(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        if (options == null || attrs == null) {
            throw new NullPointerException();
        }
        if (attrs.length > 0) {
            throw new UnsupportedOperationException("initial attributes not supported");
        }
        OpenOption[] arr = new OpenOption[options.size()];
        int i = 0;
        for (OpenOption o : options) {
            arr[i] = o;
            i = i + 1;
        }
        return KajiFileChannel.openFile(path, arr);
    }

    // ---- reading and writing by current position -------------------------------------------------

    /** Reads from the current position and advances it. */
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * Reads spreading into several buffers, in order.
     *
     * <p>Filling one before starting the next is the contract, not a detail: it is what allows a
     * fixed-size header and its body to be read in a single call.
     */
    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    /** Like the other one, with every buffer of the array. */
    public final long read(ByteBuffer[] dsts) throws IOException {
        return this.read(dsts, 0, dsts.length);
    }

    /** Writes at the current position and advances it. */
    public abstract int write(ByteBuffer src) throws IOException;

    /** Writes gathering several buffers, in order. */
    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    /** Like the other one, with every buffer of the array. */
    public final long write(ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }

    // ---- position and size -----------------------------------------------------------------------

    /** The current position, in bytes from the beginning. */
    public abstract long position() throws IOException;

    /**
     * Moves the position.
     *
     * <p>Beyond the end is admitted: reading there gives -1 and writing there leaves a hole of
     * zeroes.
     */
    public abstract FileChannel position(long newPosition) throws IOException;

    /** The size of the file. */
    public abstract long size() throws IOException;

    /**
     * Cuts the file down to `size`.
     *
     * <p>If it was smaller already nothing happens --it does not grow it-- and if the position was
     * left beyond the new end, it becomes the new end.
     */
    public abstract FileChannel truncate(long size) throws IOException;

    /**
     * Forces the changes to the disk.
     *
     * <p>It does nothing, and it is not an omission: this channel writes to the disk on every
     * `write`, so when this is called nothing is left pending. See the note of the class.
     *
     * @param metaData if `false`, the metadata need not be forced
     */
    public abstract void force(boolean metaData) throws IOException;

    // ---- transfers -------------------------------------------------------------------------------

    /**
     * Copies up to `count` bytes from `position` of this file towards `target`.
     *
     * <p>It does not touch the current position of this channel --it does touch `target`'s--, which
     * is what allows using it from several threads over the same channel.
     */
    public abstract long transferTo(long position, long count, WritableByteChannel target)
            throws IOException;

    /** Copies up to `count` bytes of `src` towards `position` of this file. */
    public abstract long transferFrom(ReadableByteChannel src, long position, long count)
            throws IOException;

    // ---- reading and writing by absolute position ------------------------------------------------

    /**
     * Reads from `position` **without moving** the current position.
     *
     * @throws IllegalArgumentException if `position` is negative
     */
    public abstract int read(ByteBuffer dst, long position) throws IOException;

    /**
     * Writes at `position` without moving the current position.
     *
     * @throws IllegalArgumentException if `position` is negative
     */
    public abstract int write(ByteBuffer src, long position) throws IOException;

    // ---- MapMode ---------------------------------------------------------------------------------

    /**
     * The modes of a memory mapping.
     *
     * <p>It is not an `enum` --nor in the JDK-- because the list is left open: a file system
     * provider may add modes of its own, and an `enum` would rule that out for ever.
     */
    public static class MapMode {

        /** Read-only mapping. */
        public static final MapMode READ_ONLY = new MapMode("READ_ONLY");

        /** Read-write mapping; the changes reach the file. */
        public static final MapMode READ_WRITE = new MapMode("READ_WRITE");

        /** Copy on write: the changes stay in the mapping and do not touch the file. */
        public static final MapMode PRIVATE = new MapMode("PRIVATE");

        private final String name;

        // Package-private and not private --the JDK has it private-- so that `MapModes` can build
        // the modes of `jdk.nio.mapmode`. See that class's comment: it is the same bridge the JDK
        // makes with `SharedSecrets`, without the machinery. It is not API: it changes no public or
        // protected member of `MapMode`.
        MapMode(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }
}
