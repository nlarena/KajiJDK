package javax.imageio.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * KajiLibrary's javax.imageio.stream.MemoryCacheImageOutputStream -- writes to any stream,
 * collecting in memory.
 *
 * <p>The mirror of {@link MemoryCacheImageInputStream}. An {@link OutputStream} cannot seek, and
 * writing an image format almost always needs to go back and fix the header; the solution is to
 * collect everything in memory and release it when possible.
 *
 * <h2>{@link #flushBefore} is what really writes</h2>
 *
 * <p>It is the part that gets misread. Until it is called, <b>nothing</b> reaches the underlying
 * stream: everything stays on the heap. {@link #close} flushes, and with that everything goes
 * out.
 *
 * <p>And once a stretch went out, it cannot be revisited: {@link #seek} to an earlier position
 * throws {@link IndexOutOfBoundsException}. It is the price of having released it.
 *
 * <p>The underlying stream is not closed when this one is closed.
 */
public class MemoryCacheImageOutputStream extends ImageOutputStreamImpl {

    /** How many bytes each block has. */
    private static final int BLOCK_SIZE = 8192;

    /** Where what is released goes. */
    private OutputStream stream;

    /** The collected blocks; the first one corresponds to {@link #cacheStart}. */
    private final ArrayList<byte[]> cache = new ArrayList<byte[]>();

    /** Which position the first block corresponds to. */
    private long cacheStart = 0;

    /** How far it was written. */
    private long length = 0;

    /**
     * @param stream where to write
     * @throws IllegalArgumentException if it is null
     */
    public MemoryCacheImageOutputStream(OutputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;
    }

    /** One byte of what was already written, or -1 past the end. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (this.streamPos >= this.length) {
            return -1;
        }
        int value = byteAt(this.streamPos) & 0xFF;
        this.streamPos = this.streamPos + 1;
        return value;
    }

    /** Up to {@code len} bytes of what was already written. */
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
        if (this.streamPos >= this.length) {
            return -1;
        }
        int available = (int) Math.min((long) len, this.length - this.streamPos);
        int i = 0;
        while (i < available) {
            b[off + i] = byteAt(this.streamPos + i);
            i = i + 1;
        }
        this.streamPos = this.streamPos + available;
        return available;
    }

    /** One byte. */
    @Override
    public void write(int b) throws IOException {
        flushBits();
        ensureCapacity(this.streamPos + 1);
        setByteAt(this.streamPos, (byte) b);
        this.streamPos = this.streamPos + 1;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** That part of the array. */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        if (b == null) {
            throw new NullPointerException("b == null!");
        }
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(this.streamPos + len);
        int i = 0;
        while (i < len) {
            setByteAt(this.streamPos + i, b[off + i]);
            i = i + 1;
        }
        this.streamPos = this.streamPos + len;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** How much was written so far. */
    @Override
    public long length() {
        return this.length;
    }

    /** Yes. */
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

    /**
     * Releases to the underlying stream everything before that position. See the class note.
     *
     * @throws IndexOutOfBoundsException if it is before the current flushed position or after the
     *     position
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        long oldFlushed = getFlushedPosition();
        super.flushBefore(pos);
        long i = oldFlushed;
        while (i < pos) {
            this.stream.write(byteAt(i) & 0xFF);
            i = i + 1;
        }
        this.stream.flush();
        // The blocks left whole behind are no longer needed.
        long firstNeeded = (pos / BLOCK_SIZE) * BLOCK_SIZE;
        while (this.cacheStart + BLOCK_SIZE <= firstNeeded && !this.cache.isEmpty()) {
            this.cache.remove(0);
            this.cacheStart = this.cacheStart + BLOCK_SIZE;
        }
    }

    /** Releases everything pending and closes. Does not close the underlying stream. */
    @Override
    public void close() throws IOException {
        try {
            flushBits();
        } catch (IOException e) {
            // It is already closing.
        }
        // Without this, what was written after the last flushBefore would be silently lost.
        long pos = this.length;
        seek(pos);
        flushBefore(pos);
        super.close();
        this.cache.clear();
        this.stream = null;
    }

    /** Grows the cache until that position can be written. */
    private void ensureCapacity(long pos) {
        while (this.cacheStart + (long) this.cache.size() * BLOCK_SIZE < pos) {
            this.cache.add(new byte[BLOCK_SIZE]);
        }
    }

    /** The byte kept at that position. */
    private byte byteAt(long pos) {
        long offset = pos - this.cacheStart;
        byte[] block = this.cache.get((int) (offset / BLOCK_SIZE));
        return block[(int) (offset % BLOCK_SIZE)];
    }

    /** Writes it. */
    private void setByteAt(long pos, byte value) {
        long offset = pos - this.cacheStart;
        byte[] block = this.cache.get((int) (offset / BLOCK_SIZE));
        block[(int) (offset % BLOCK_SIZE)] = value;
    }
}
