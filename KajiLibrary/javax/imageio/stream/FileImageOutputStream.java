package javax.imageio.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileImageOutputStream -- writes a file, with random access.
 *
 * <p>The mirror of {@link FileImageInputStream}, over a {@link RandomAccessFile} open for reading
 * and writing. It is the implementation that makes the pattern "write the header with a length I
 * do not know yet, write the image, go back and fix it" easy.
 *
 * <p>The file is opened in {@code "rw"} mode: if it does not exist it is created, and if it exists
 * it <b>is not truncated</b>. Writing a file shorter than the previous one leaves the old one's
 * tail stuck at the end.
 *
 * <p>See {@link FileImageInputStream} about who closes what.
 */
public class FileImageOutputStream extends ImageOutputStreamImpl {

    /** The file. */
    private RandomAccessFile raf;

    /**
     * Opens that file for reading and writing.
     *
     * @throws IllegalArgumentException if it is null
     * @throws FileNotFoundException if it cannot be opened
     * @throws IOException if it failed
     */
    public FileImageOutputStream(File f) throws FileNotFoundException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("f == null!");
        }
        this.raf = new RandomAccessFile(f, "rw");
    }

    /**
     * Uses that already open file.
     *
     * @throws IllegalArgumentException if it is null
     */
    public FileImageOutputStream(RandomAccessFile raf) {
        if (raf == null) {
            throw new IllegalArgumentException("raf == null!");
        }
        this.raf = raf;
    }

    /** One byte. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        int val = this.raf.read();
        if (val != -1) {
            this.streamPos = this.streamPos + 1;
        }
        return val;
    }

    /** Up to {@code len} bytes. */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        this.bitOffset = 0;
        int nbytes = this.raf.read(b, off, len);
        if (nbytes != -1) {
            this.streamPos = this.streamPos + nbytes;
        }
        return nbytes;
    }

    /** One byte; first closes the pending bit byte. */
    @Override
    public void write(int b) throws IOException {
        flushBits();
        this.raf.write(b);
        this.streamPos = this.streamPos + 1;
    }

    /** That part of the array. */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        this.raf.write(b, off, len);
        this.streamPos = this.streamPos + len;
    }

    /** The size of the file, or -1. */
    @Override
    public long length() {
        try {
            checkClosed();
            return this.raf.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Seeks.
     *
     * <p>It may go past the end: the file grows with zeros when written there. It is what allows
     * reserving space for a header and filling it in later.
     */
    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.raf.seek(pos);
        this.streamPos = this.raf.getFilePointer();
    }

    /** Closes the pending byte, closes the stream and the file. */
    @Override
    public void close() throws IOException {
        try {
            flushBits();
        } catch (IOException e) {
            // It is already closing; there is nothing better to do than keep closing.
        }
        super.close();
        this.raf.close();
        this.raf = null;
    }

    /** Closes it if nobody did. */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
