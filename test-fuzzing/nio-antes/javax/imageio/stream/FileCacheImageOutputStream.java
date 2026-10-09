package javax.imageio.stream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileCacheImageOutputStream -- writes to any stream, collecting
 * in a temporary file.
 *
 * <p>The combination of {@link FileCacheImageInputStream} and {@link MemoryCacheImageOutputStream}:
 * it can seek and fix things up freely because everything goes through a file on disk first, and
 * {@link #flushBefore} releases what will not be touched again.
 *
 * <p>As in the memory version, <b>nothing reaches the underlying stream</b> until
 * {@code flushBefore} or {@link #close} is called.
 *
 * <p>The temporary file is deleted on close; the underlying stream is not closed.
 */
public class FileCacheImageOutputStream extends ImageOutputStreamImpl {

    /** Where what is released goes. */
    private OutputStream stream;

    /** Where it is collected. */
    private File cacheFile;

    /** The temporary file, open. */
    private RandomAccessFile cache;

    /** How far it was written. */
    private long length = 0;

    /**
     * @param stream where to write
     * @param cacheDir where to put the temporary file, or null for the system's
     * @throws IllegalArgumentException if the stream is null, or if the directory is not one
     * @throws IOException if the temporary file could not be created
     */
    public FileCacheImageOutputStream(OutputStream stream, File cacheDir) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        if (cacheDir != null && !cacheDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory!");
        }
        this.stream = stream;
        this.cacheFile = File.createTempFile("imageio", ".tmp", cacheDir);
        this.cache = new RandomAccessFile(this.cacheFile, "rw");
    }

    /** One byte of what was already written. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (this.streamPos >= this.length) {
            return -1;
        }
        this.cache.seek(this.streamPos);
        int value = this.cache.read();
        if (value != -1) {
            this.streamPos = this.streamPos + 1;
        }
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
        this.cache.seek(this.streamPos);
        int toRead = (int) Math.min((long) len, this.length - this.streamPos);
        int nbytes = this.cache.read(b, off, toRead);
        if (nbytes > 0) {
            this.streamPos = this.streamPos + nbytes;
        }
        return nbytes;
    }

    /** One byte. */
    @Override
    public void write(int b) throws IOException {
        flushBits();
        this.cache.seek(this.streamPos);
        this.cache.write(b);
        this.streamPos = this.streamPos + 1;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** That part of the array. */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        this.cache.seek(this.streamPos);
        this.cache.write(b, off, len);
        this.streamPos = this.streamPos + len;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** How much was written. */
    @Override
    public long length() {
        return this.length;
    }

    /** Seeks; it may go past the end. */
    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.streamPos = pos;
    }

    /** Yes. */
    @Override
    public boolean isCached() {
        return true;
    }

    /** Yes. */
    @Override
    public boolean isCachedFile() {
        return true;
    }

    /** No. */
    @Override
    public boolean isCachedMemory() {
        return false;
    }

    /**
     * Releases to the underlying stream everything before that position.
     *
     * @throws IndexOutOfBoundsException if it is before the current flushed position or after the
     *     position
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        long oldFlushed = getFlushedPosition();
        super.flushBefore(pos);
        if (pos <= oldFlushed) {
            return;
        }
        byte[] buf = new byte[8192];
        long at = oldFlushed;
        this.cache.seek(at);
        while (at < pos) {
            int toRead = (int) Math.min((long) buf.length, pos - at);
            int read = this.cache.read(buf, 0, toRead);
            if (read <= 0) {
                break;
            }
            this.stream.write(buf, 0, read);
            at = at + read;
        }
        this.stream.flush();
    }

    /** Releases what is pending, closes and deletes the temporary file. */
    @Override
    public void close() throws IOException {
        try {
            flushBits();
        } catch (IOException e) {
            // It is already closing.
        }
        long pos = this.length;
        seek(pos);
        flushBefore(pos);
        super.close();
        this.cache.close();
        this.cache = null;
        this.cacheFile.delete();
        this.cacheFile = null;
        this.stream = null;
    }
}
