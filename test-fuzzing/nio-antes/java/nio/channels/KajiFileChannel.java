package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.MappedBuffers;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import jdk.internal.io.Fs;

/**
 * The implementation of {@link FileChannel} over the file natives of this VM.
 *
 * <p>It is what {@link FileChannel#open} returns, and it is not public on purpose: nobody should
 * write its name. The why of each operation touching the disk --and the price that has-- is
 * explained in the header of {@link FileChannel}; here there is only the how.
 *
 * <p>The three rules that repeat and are worth reading once:
 *
 * <ul>
 *   <li>every read starts with {@link #contents()}, which goes to the disk. There is no cache, so
 *       there is no cache to invalidate and no moment when what is seen stops being what there is;
 *   <li>every write is read-modify-write of the whole file. That is why it lives in a single place,
 *       {@link #writeThrough}: spread over five methods, sooner or later one of them would forget to
 *       keep the tail of the file and would truncate somebody else's data;
 *   <li>writing beyond the end fills with zeroes, which is what a sparse file of the JDK does when
 *       it is read.
 * </ul>
 */
final class KajiFileChannel extends FileChannel {

    // The limit of a `byte[]`. A file bigger than this does not fit in an array, and the error has
    // to come out as such and not as a `NegativeArraySizeException` from the innards.
    private static final long CAP = 2147483639L;

    private final String filePath;
    private final boolean readable;
    private final boolean writable;
    private final boolean appendMode;
    private final boolean deleteOnClose;

    private long pos = 0;

    private KajiFileChannel(String filePath, boolean readable, boolean writable, boolean appendMode,
            boolean deleteOnClose) {
        this.filePath = filePath;
        this.readable = readable;
        this.writable = writable;
        this.appendMode = appendMode;
        this.deleteOnClose = deleteOnClose;
    }

    // ---- opening --------------------------------------------------------------------------------

    static FileChannel openFile(Path path, OpenOption[] options) throws IOException {
        if (path == null) {
            throw new NullPointerException();
        }
        String p = path.toString();

        boolean readable = false;
        boolean writable = false;
        boolean appendMode = false;
        boolean truncateIt = false;
        boolean createIt = false;
        boolean createNew = false;
        boolean deleteOnClose = false;

        int i = 0;
        while (i < options.length) {
            OpenOption o = options[i];
            if (o == null) {
                throw new NullPointerException();
            }
            if (o == StandardOpenOption.READ) {
                readable = true;
            } else if (o == StandardOpenOption.WRITE) {
                writable = true;
            } else if (o == StandardOpenOption.APPEND) {
                appendMode = true;
            } else if (o == StandardOpenOption.TRUNCATE_EXISTING) {
                truncateIt = true;
            } else if (o == StandardOpenOption.CREATE) {
                createIt = true;
            } else if (o == StandardOpenOption.CREATE_NEW) {
                createNew = true;
            } else if (o == StandardOpenOption.DELETE_ON_CLOSE) {
                deleteOnClose = true;
            } else if (o == StandardOpenOption.SYNC || o == StandardOpenOption.DSYNC) {
                // They are accepted because they are fulfilled: this channel writes to the disk on every
                // `write`. There is no flag to keep; what they ask for is how it works already.
            } else if (o == LinkOption.NOFOLLOW_LINKS) {
                // With no links in the model, not following them is the only thing that can be done.
            } else {
                // `SPARSE` lands here. See the note of `FileChannel.open`.
                throw new UnsupportedOperationException(String.valueOf(o) + " not supported");
            }
            i = i + 1;
        }

        if (appendMode && readable) {
            throw new IllegalArgumentException("READ + APPEND not allowed");
        }
        if (appendMode && truncateIt) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        // With no access option at all, it is read. It is what the JDK says and what anybody who calls
        // `open(path)` plain expects.
        if (!readable && !writable && !appendMode) {
            readable = true;
        }

        boolean exists = (Fs.stat(p) & Fs.EXISTS) != 0;
        if (createNew && exists) {
            throw new FileAlreadyExistsException(p);
        }
        boolean mayCreate = (createIt || createNew) && (writable || appendMode);
        if (!exists && !mayCreate) {
            throw new NoSuchFileException(p);
        }
        if (!exists) {
            if (!Fs.writeAllBytes(p, new byte[0], false)) {
                throw new IOException("could not create " + p);
            }
        } else if (truncateIt && (writable || appendMode)) {
            if (!Fs.writeAllBytes(p, new byte[0], false)) {
                throw new IOException("could not truncate " + p);
            }
        }

        KajiFileChannel c = new KajiFileChannel(p, readable, writable || appendMode, appendMode, deleteOnClose);
        return c;
    }

    // ---- what is underneath ----------------------------------------------------------------------

    private byte[] contents() throws IOException {
        byte[] b = Fs.readAllBytes(this.filePath);
        if (b == null) {
            // It happens if the file disappeared after openFile. It is not a rare laboratory case: another
            // process can delete it at any moment, and returning an empty array would pass it off as a file
            // that was left at zero.
            throw new IOException("could not read " + this.filePath);
        }
        return b;
    }

    // It writes `len` bytes of `data` starting at `from`, keeping everything that was there before
    // and after that stretch. It returns `len`.
    private int writeThrough(long from, byte[] data, int off, int len) throws IOException {
        byte[] old = this.contents();
        long endPos = from + len;
        if (endPos > CAP) {
            throw new IOException("file too big for this VM");
        }
        int newLen = (int) Math.max((long) old.length, endPos);
        byte[] fresh;
        if (newLen == old.length) {
            fresh = old;
        } else {
            // The intermediate filling is left at zero by itself: `new byte[]` sets them already, which is
            // just the hole of zeroes that corresponds when writing beyond the end.
            fresh = new byte[newLen];
            System.arraycopy(old, 0, fresh, 0, old.length);
        }
        System.arraycopy(data, off, fresh, (int) from, len);
        if (!Fs.writeAllBytes(this.filePath, fresh, false)) {
            throw new IOException("could not write " + this.filePath);
        }
        return len;
    }

    private void requireOpen() throws IOException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void requireReadable() throws IOException {
        this.requireOpen();
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
    }

    private void requireWritable() throws IOException {
        this.requireOpen();
        if (!this.writable) {
            throw new NonWritableChannelException();
        }
    }

    // It takes what is left of `src`, as an array, and leaves the position of the buffer at the end.
    // Every write goes through here so that the advancing of the buffer's position is a single one
    // and not five.
    private static byte[] drain(ByteBuffer src) {
        int n = src.remaining();
        byte[] b = new byte[n];
        src.get(b, 0, n);
        return b;
    }

    // ---- reading ---------------------------------------------------------------------------------

    public int read(ByteBuffer dst) throws IOException {
        this.requireReadable();
        int n = this.readInto(dst, this.pos);
        if (n > 0) {
            this.pos = this.pos + n;
        }
        return n;
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        this.requireReadable();
        return this.readInto(dst, position);
    }

    private int readInto(ByteBuffer dst, long from) throws IOException {
        if (dst == null) {
            throw new NullPointerException();
        }
        if (dst.isReadOnly()) {
            throw new java.nio.ReadOnlyBufferException();
        }
        boolean ok = false;
        this.begin();
        try {
            byte[] data = this.contents();
            if (from >= data.length) {
                ok = true;
                // End of file is -1 even if the buffer was full; that there is no room left is another story
                // and goes below.
                return -1;
            }
            int room = dst.remaining();
            if (room == 0) {
                ok = true;
                return 0;
            }
            int n = (int) Math.min((long) room, (long) data.length - from);
            dst.put(data, (int) from, n);
            ok = true;
            return n;
        } finally {
            this.end(ok);
        }
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        checkRange(dsts, offset, length);
        this.requireReadable();
        long total = 0;
        int i = offset;
        while (i < offset + length) {
            ByteBuffer d = dsts[i];
            if (d.remaining() > 0) {
                int n = this.read(d);
                if (n < 0) {
                    // End of file. If something had been read already that is returned; if not, -1. Returning
                    // 0 in the second case would make a reading loop never end.
                    if (total == 0) {
                        return -1;
                    }
                    return total;
                }
                total = total + n;
                if (d.hasRemaining()) {
                    // The buffer was not filled, and this channel never returns short reads for another
                    // reason: it means the file has run out. Going on with the next buffer would only repeat
                    // the -1.
                    return total;
                }
            }
            i = i + 1;
        }
        return total;
    }

    // ---- escritura -------------------------------------------------------------------------------

    public int write(ByteBuffer src) throws IOException {
        this.requireWritable();
        if (src == null) {
            throw new NullPointerException();
        }
        boolean ok = false;
        this.begin();
        try {
            // In append mode the current position does not rule: the destination is always the end in
            // force at the moment of writing, which is the only thing that makes `APPEND` useful.
            long from;
            if (this.appendMode) {
                from = this.sizeOf();
            } else {
                from = this.pos;
            }
            byte[] b = drain(src);
            int n = this.writeThrough(from, b, 0, b.length);
            this.pos = from + n;
            ok = true;
            return n;
        } finally {
            this.end(ok);
        }
    }

    public int write(ByteBuffer src, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        this.requireWritable();
        if (this.appendMode) {
            // Writing at a chosen position contradicts the one thing `APPEND` promises --that everything
            // goes to the end-- and the JDK forbids it for exactly that.
            throw new IOException("canal abierto en modo APPEND");
        }
        if (src == null) {
            throw new NullPointerException();
        }
        boolean ok = false;
        this.begin();
        try {
            byte[] b = drain(src);
            int n = this.writeThrough(position, b, 0, b.length);
            ok = true;
            return n;
        } finally {
            this.end(ok);
        }
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        checkRange(srcs, offset, length);
        this.requireWritable();
        long total = 0;
        int i = offset;
        while (i < offset + length) {
            ByteBuffer s = srcs[i];
            if (s.remaining() > 0) {
                total = total + this.write(s);
            }
            i = i + 1;
        }
        return total;
    }

    // ---- position and size -----------------------------------------------------------------------

    public long position() throws IOException {
        this.requireOpen();
        return this.pos;
    }

    public FileChannel position(long newPosition) throws IOException {
        if (newPosition < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        this.requireOpen();
        this.pos = newPosition;
        return this;
    }

    public long size() throws IOException {
        this.requireOpen();
        return this.sizeOf();
    }

    private long sizeOf() throws IOException {
        return Fs.size(this.filePath);
    }

    public FileChannel truncate(long size) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("tamanio negativo");
        }
        this.requireWritable();
        byte[] old = this.contents();
        if (size < old.length) {
            byte[] fresh = new byte[(int) size];
            System.arraycopy(old, 0, fresh, 0, (int) size);
            if (!Fs.writeAllBytes(this.filePath, fresh, false)) {
                throw new IOException("could not truncate " + this.filePath);
            }
        }
        // The position is cut back even if the file did not change size: the contract is that it never
        // be left pointing beyond the end.
        if (this.pos > size) {
            this.pos = size;
        }
        return this;
    }

    public void force(boolean metaData) throws IOException {
        this.requireOpen();
        // Nothing to force; see the note of `FileChannel.force`.
    }

    // ---- transferencias --------------------------------------------------------------------------

    public long transferTo(long position, long count, WritableByteChannel target)
            throws IOException {
        if (position < 0 || count < 0) {
            throw new IllegalArgumentException("negative position or count");
        }
        if (target == null) {
            throw new NullPointerException();
        }
        this.requireReadable();
        if (!target.isOpen()) {
            throw new ClosedChannelException();
        }
        byte[] data = this.contents();
        if (position >= data.length) {
            return 0;
        }
        int n = (int) Math.min(count, (long) data.length - position);
        ByteBuffer bb = ByteBuffer.wrap(data, (int) position, n);
        long written = 0;
        while (bb.hasRemaining()) {
            int w = target.write(bb);
            if (w <= 0) {
                break;
            }
            written = written + w;
        }
        return written;
    }

    public long transferFrom(ReadableByteChannel src, long position, long count)
            throws IOException {
        if (position < 0 || count < 0) {
            throw new IllegalArgumentException("negative position or count");
        }
        if (src == null) {
            throw new NullPointerException();
        }
        this.requireWritable();
        if (!src.isOpen()) {
            throw new ClosedChannelException();
        }
        if (position > this.sizeOf()) {
            // The JDK does not grow the file to get there: if the position goes past the end, nothing is
            // transferred.
            return 0;
        }
        if (count > CAP) {
            throw new IOException("transfer too big for this VM");
        }
        ByteBuffer bb = ByteBuffer.allocate((int) count);
        long readCount = 0;
        while (bb.hasRemaining()) {
            int n = src.read(bb);
            if (n <= 0) {
                break;
            }
            readCount = readCount + n;
        }
        if (readCount == 0) {
            return 0;
        }
        return this.writeThrough(position, bb.array(), 0, (int) readCount);
    }

    // ---- locks -----------------------------------------------------------------------------------

    /**
     * Takes a lock over that region, waiting if it has to.
     *
     * <p>See the note on {@code FileLockRegistry} for why an overlap with another lock of this same
     * VM is an error and not a wait.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return the lock
     * @throws IOException if it cannot be taken
     */
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return acquire(position, size, shared, true);
    }

    /**
     * The same without waiting: returns {@code null} when another process holds the region.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return the lock, or {@code null}
     * @throws IOException if it cannot be taken
     */
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return acquire(position, size, shared, false);
    }

    /**
     * The same acquisition on behalf of an asynchronous channel wrapped around this one.
     *
     * <p>The owner matters for {@link FileLock#acquiredBy}, which has to name the channel the
     * caller asked on: whoever holds an {@link AsynchronousFileChannel} never saw this one and
     * could not recognise it.
     */
    FileLock acquireFor(AsynchronousFileChannel owner, long position, long size, boolean shared,
            boolean wait) throws IOException {
        final KajiFileLock l = reserveFor(owner, position, size, shared);
        return complete(l, wait) ? l : null;
    }

    /**
     * Claims the region for an asynchronous channel without taking the system lock yet.
     *
     * <p>The claim happens on the caller's thread on purpose: an asynchronous `lock` throws
     * {@link OverlappingFileLockException} rather than delivering it through the future, so the
     * check cannot wait for the pool. See {@code KajiFileLock.RESERVED}.
     */
    KajiFileLock reserveFor(AsynchronousFileChannel owner, long position, long size,
            boolean shared) throws IOException {
        final String key = checkRequest(position, size, shared);
        final KajiFileLock l =
                new KajiFileLock(owner, key, position, size, shared, KajiFileLock.RESERVED);
        FileLockRegistry.add(l);
        return l;
    }

    /**
     * Takes the system lock a reservation was standing in for.
     *
     * <p>Returns false when the region was held by another process and the caller asked not to
     * wait; the reservation is dropped either way it fails.
     */
    boolean complete(KajiFileLock l, boolean wait) throws IOException {
        final int t;
        try {
            t = takeSystemLock(l.position(), l.size(), l.isShared(), wait);
        } catch (IOException e) {
            l.release();
            throw e;
        }
        if (t < 0) {
            l.release();
            return false;
        }
        l.confirm(t);
        return true;
    }

    private FileLock acquire(long position, long size, boolean shared, boolean wait)
            throws IOException {
        final String key = checkRequest(position, size, shared);
        final KajiFileLock l =
                new KajiFileLock(this, key, position, size, shared, KajiFileLock.RESERVED);
        FileLockRegistry.add(l);
        return complete(l, wait) ? l : null;
    }

    /**
     * Checks the request and claims nothing yet.
     *
     * @return the canonical path the lock would be taken on
     */
    private String checkRequest(long position, long size, boolean shared) throws IOException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
        if (position < 0 || size < 0) {
            throw new IllegalArgumentException("Negative position or size");
        }
        if (shared && !this.readable) {
            throw new NonReadableChannelException();
        }
        if (!shared && !this.writable && !this.appendMode) {
            throw new NonWritableChannelException();
        }
        final String canonical = Fs.canonical(this.filePath);
        final String key = canonical == null ? this.filePath : canonical;
        FileLockRegistry.check(key, position, size);
        return key;
    }

    /**
     * Asks the system for the lock.
     *
     * @return the token, or -1 when the region was held and the caller asked not to wait
     */
    private int takeSystemLock(long position, long size, boolean shared, boolean wait)
            throws IOException {
        // `Long.MAX_VALUE` is how Java says "everything still to come"; the VM says it with a zero.
        final long count = size == Long.MAX_VALUE ? 0L : size;
        final int t = Fs.lock(this.filePath, position, count, shared, wait);
        if (t == -2) {
            throw new IOException("this system has no file locks");
        }
        if (t < 0 && wait) {
            throw new IOException("could not take the lock on " + this.filePath);
        }
        return t;
    }

    // ---- mapping ---------------------------------------------------------------------------------

    /**
     * Maps a region of the file into memory.
     *
     * <p>The buffer that comes back is not a copy: it reads and writes the file's own pages. That
     * is the whole reason this method waited for a native, and the reason it is worth the wait -- a
     * mapping that took writes and dropped them would be the quietest kind of wrong.
     */
    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        }
        if (mode == null) {
            throw new NullPointerException("Mode is null");
        }
        if (position < 0L) {
            throw new IllegalArgumentException("Negative position");
        }
        if (size < 0L) {
            throw new IllegalArgumentException("Negative size");
        }
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
        // A mode that is not one of the three is refused, and that is the point of refusing it: the
        // two in `jdk.nio.mapmode` ask for non-volatile memory, and treating one of those as a
        // plain READ_WRITE would hand back a mapping that looks like what was asked for and is not.
        // The JDK on this platform answers the same way.
        final int mapMode;
        if (mode == MapMode.READ_ONLY) {
            mapMode = Fs.MAP_READ_ONLY;
        } else if (mode == MapMode.READ_WRITE) {
            mapMode = Fs.MAP_READ_WRITE;
        } else if (mode == MapMode.PRIVATE) {
            mapMode = Fs.MAP_PRIVATE;
        } else {
            throw new UnsupportedOperationException("Unsupported map mode: " + mode);
        }
        final boolean readOnly = mode == MapMode.READ_ONLY;
        if (!this.readable) {
            throw new NonReadableChannelException();
        }
        if (!readOnly && !this.writable) {
            // PRIVATE lands here too, and on purpose: its writes never reach the file, but the
            // system still hands out writable pages, and a channel opened for reading alone has no
            // business asking for those.
            throw new NonWritableChannelException();
        }
        // A zero-length mapping is a buffer with nothing in it, not a call to the system: `mmap`
        // refuses a length of zero, and there would be nothing to look at anyway.
        if (size == 0L) {
            return MappedBuffers.of(-1, 0, readOnly);
        }
        growTo(position + size);
        final int token = Fs.mapOpen(this.filePath, mapMode, position, (int) size);
        if (token == -2) {
            throw new IOException("this system cannot map files into memory");
        }
        if (token < 0) {
            throw new IOException("could not map " + this.filePath);
        }
        return MappedBuffers.of(token, (int) size, readOnly);
    }

    /**
     * Makes sure the file reaches at least this far before it is mapped.
     *
     * <p>Mapping past the end of a file is not a bigger mapping, it is a fault: Unix raises SIGBUS
     * when the page is touched. So the file grows first, with zeros, which is what the JDK does and
     * what {@link #map} documents by not mentioning it.
     */
    private void growTo(long end) throws IOException {
        final long have = Fs.size(this.filePath);
        if (have >= end) {
            return;
        }
        if (!this.writable) {
            throw new IOException("Channel not open for writing - cannot extend file to required"
                    + " size");
        }
        final byte[] zeros = new byte[(int) (end - have)];
        if (!Fs.writeAllBytes(this.filePath, zeros, true)) {
            throw new IOException("could not extend " + this.filePath);
        }
    }

    // ---- closing ----------------------------------------------------------------------------------

    protected void implCloseChannel() throws IOException {
        // Closing the channel invalidates its locks. It goes first: if `DELETE_ON_CLOSE` removed
        // the file while a lock was still held, the descriptor holding it would outlive the file.
        FileLockRegistry.releaseAllOn(this);
        if (this.deleteOnClose) {
            // Without `throws` if it fails: `DELETE_ON_CLOSE` is a cleanup, and making the closing fail
            // because it could not clean up turns an oversight into an error of the program.
            Fs.delete(this.filePath);
        }
    }

    // ---- comun -----------------------------------------------------------------------------------

    private static void checkRange(ByteBuffer[] bufs, int offset, int length) {
        if (bufs == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || length > bufs.length - offset) {
            throw new IndexOutOfBoundsException();
        }
    }
}
