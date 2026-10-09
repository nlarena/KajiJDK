package javax.imageio.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * KajiLibrary's javax.imageio.stream.MemoryCacheImageInputStream -- reads any stream, keeping what
 * goes by in memory.
 *
 * <p>An {@link InputStream} cannot be rewound, and image formats need to go back. This class's
 * solution is to keep everything read in memory.
 *
 * <p>The consequence is the expected one: <b>memory grows with what is read</b>. For a
 * hundred-megabyte image from a socket, that is a hundred megabytes of heap.
 * {@link FileCacheImageInputStream} is the alternative when that does not fit.
 *
 * <p>{@link #flushBefore} is what makes it usable: promising not to go back before a certain point
 * frees everything before it. A reader that works in strips can read a huge file with bounded
 * memory, which is why it is worth calling.
 *
 * <p>The underlying stream is <b>not</b> closed when this one is closed.
 */
public class MemoryCacheImageInputStream extends ImageInputStreamImpl {

    /** How many bytes each cache block has. */
    private static final int BLOCK_SIZE = 8192;

    /** Where it really reads from. */
    private InputStream stream;

    /** The kept blocks; the first one corresponds to {@link #cacheStart}. */
    private final ArrayList<byte[]> cache = new ArrayList<byte[]>();

    /** Which stream position the first kept block corresponds to. */
    private long cacheStart = 0;

    /** How many bytes were read from the underlying stream in total. */
    private long length = 0;

    /** Whether the underlying stream ended. */
    private boolean foundEOF = false;

    /**
     * @param stream where to read from
     * @throws IllegalArgumentException if it is null
     */
    public MemoryCacheImageInputStream(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;
    }

    /** One byte. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (!ensureAvailable(this.streamPos + 1)) {
            return -1;
        }
        int value = byteAt(this.streamPos) & 0xFF;
        this.streamPos = this.streamPos + 1;
        return value;
    }

    /** Up to {@code len} bytes. */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if (b == null) {
            throw new NullPointerException("b == null!");
        }
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        this.bitOffset = 0;
        if (len == 0) {
            return 0;
        }
        if (!ensureAvailable(this.streamPos + 1)) {
            return -1;
        }
        ensureAvailable(this.streamPos + len);
        int available = (int) Math.min((long) len, this.length - this.streamPos);
        int i = 0;
        while (i < available) {
            b[off + i] = byteAt(this.streamPos + i);
            i = i + 1;
        }
        this.streamPos = this.streamPos + available;
        return available;
    }

    /**
     * Frees everything before that position. See the class note.
     *
     * @throws IndexOutOfBoundsException if it is before the current flushed position or after the
     *     position
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        super.flushBefore(pos);
        // Whole blocks are dropped: freeing byte by byte would force moving what is left.
        long firstNeeded = (pos / BLOCK_SIZE) * BLOCK_SIZE;
        while (this.cacheStart + BLOCK_SIZE <= firstNeeded && !this.cache.isEmpty()) {
            this.cache.remove(0);
            this.cacheStart = this.cacheStart + BLOCK_SIZE;
        }
    }

    /** Yes. It keeps things in memory. */
    @Override
    public boolean isCached() {
        return true;
    }

    /** No. */
    @Override
    public boolean isCachedFile() {
        return false;
    }

    /** Yes. */
    @Override
    public boolean isCachedMemory() {
        return true;
    }

    /** Closes and releases the cache. Does not close the underlying stream. */
    @Override
    public void close() throws IOException {
        super.close();
        this.cache.clear();
        this.stream = null;
    }

    /** Closes it if nobody did. */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * Reads from the underlying stream until that many bytes are kept.
     *
     * @return whether it got there; false if the stream ended first
     */
    private boolean ensureAvailable(long needed) throws IOException {
        while (this.length < needed && !this.foundEOF) {
            byte[] block;
            int within = (int) ((this.length - this.cacheStart) % BLOCK_SIZE);
            if (within == 0) {
                block = new byte[BLOCK_SIZE];
                this.cache.add(block);
            } else {
                block = this.cache.get(this.cache.size() - 1);
            }
            int read = this.stream.read(block, within, BLOCK_SIZE - within);
            if (read <= 0) {
                this.foundEOF = true;
                // The block just added was left empty; it is removed so that the block count keeps
                // matching the bytes kept.
                if (within == 0 && !this.cache.isEmpty()) {
                    this.cache.remove(this.cache.size() - 1);
                }
                break;
            }
            this.length = this.length + read;
        }
        return this.length >= needed;
    }

    /** The byte kept at that stream position. */
    private byte byteAt(long pos) {
        long offset = pos - this.cacheStart;
        byte[] block = this.cache.get((int) (offset / BLOCK_SIZE));
        return block[(int) (offset % BLOCK_SIZE)];
    }
}
