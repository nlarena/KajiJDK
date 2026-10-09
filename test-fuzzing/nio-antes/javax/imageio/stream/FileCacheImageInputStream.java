package javax.imageio.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileCacheImageInputStream -- reads any stream, keeping what it
 * reads in a temporary file.
 *
 * <p>The alternative to {@link MemoryCacheImageInputStream} when what is read does not fit in
 * memory: instead of collecting on the heap, it collects on disk.
 *
 * <p>The temporary file is created in the directory passed in, or in the system's if null is
 * passed. It is deleted on close.
 *
 * <p>The trade-off is the usual one: slower, bounded memory. A reader that processes images of
 * hundreds of megabytes from the network wants this one; one that reads thumbnails wants the
 * other.
 *
 * <p>The underlying stream is not closed when this one is closed.
 */
public class FileCacheImageInputStream extends ImageInputStreamImpl {

    /** Where it really reads from. */
    private InputStream stream;

    /** Where what was read is kept. */
    private File cacheFile;

    /** The temporary file, open. */
    private RandomAccessFile cache;

    /** How many bytes were collected. */
    private long length = 0;

    /** Whether the underlying stream ended. */
    private boolean foundEOF = false;

    /**
     * @param stream where to read from
     * @param cacheDir where to put the temporary file, or null for the system's
     * @throws IllegalArgumentException if the stream is null, or if the directory is not one
     * @throws IOException if the temporary file could not be created
     */
    public FileCacheImageInputStream(InputStream stream, File cacheDir) throws IOException {
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

    /** One byte. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (!ensureAvailable(this.streamPos + 1)) {
            return -1;
        }
        this.cache.seek(this.streamPos);
        int value = this.cache.read();
        if (value != -1) {
            this.streamPos = this.streamPos + 1;
        }
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
        this.cache.seek(this.streamPos);
        int nbytes = this.cache.read(b, off, len);
        if (nbytes > 0) {
            this.streamPos = this.streamPos + nbytes;
        }
        return nbytes;
    }

    /** Yes. It keeps things in a file. */
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

    /** Closes and deletes the temporary file. Does not close the underlying stream. */
    @Override
    public void close() throws IOException {
        super.close();
        this.cache.close();
        this.cache = null;
        // Deleting the temporary file is part of the contract: otherwise a process that reads many
        // images keeps filling the temporary directory without anything warning about it.
        this.cacheFile.delete();
        this.cacheFile = null;
        this.stream = null;
    }

    /** Closes it if nobody did. */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * Copies from the underlying stream into the temporary file until it has that amount.
     *
     * @return whether it got there
     */
    private boolean ensureAvailable(long needed) throws IOException {
        if (this.length >= needed || this.foundEOF) {
            return this.length >= needed;
        }
        byte[] buf = new byte[8192];
        this.cache.seek(this.length);
        while (this.length < needed) {
            int toRead = (int) Math.min((long) buf.length, needed - this.length);
            int read = this.stream.read(buf, 0, toRead);
            if (read <= 0) {
                this.foundEOF = true;
                break;
            }
            this.cache.write(buf, 0, read);
            this.length = this.length + read;
        }
        return this.length >= needed;
    }
}
