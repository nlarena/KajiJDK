package javax.imageio.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileImageInputStream -- reads a file, with random access.
 *
 * <p>The simplest and most efficient implementation: over a {@link RandomAccessFile} nothing needs
 * to be kept to be able to go back, because the file can already seek.
 *
 * <p>That is why {@link #isCached} returns false. It is not a shortcoming: there is nothing to
 * cache.
 *
 * <h2>Who closes what</h2>
 *
 * <p>The two constructors behave differently and it is not written anywhere obvious:
 *
 * <ul>
 *   <li>the one that takes a {@link File} opens the file and {@link #close} closes it;
 *   <li>the one that takes a {@link RandomAccessFile} closes it <b>too</b>, even though it did not
 *       open it. It is what the JDK does, and it has to be kept in mind if the file is shared.
 * </ul>
 */
public class FileImageInputStream extends ImageInputStreamImpl {

    /** The file. */
    private RandomAccessFile raf;

    /**
     * Opens that file for reading.
     *
     * @throws IllegalArgumentException if it is null
     * @throws FileNotFoundException if it does not exist or cannot be read
     * @throws IOException if opening it failed
     */
    public FileImageInputStream(File f) throws FileNotFoundException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("f == null!");
        }
        this.raf = new RandomAccessFile(f, "r");
    }

    /**
     * Uses that already open file. See the class note about who closes it.
     *
     * @throws IllegalArgumentException if it is null
     */
    public FileImageInputStream(RandomAccessFile raf) {
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

    /** The size of the file, or -1 if it cannot be known. */
    @Override
    public long length() {
        try {
            checkClosed();
            return this.raf.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /** Seeks; the file too. */
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

    /** Closes, and closes the file. See the class note. */
    @Override
    public void close() throws IOException {
        super.close();
        this.raf.close();
        this.raf = null;
    }

    /** Closes it if nobody did; see {@link ImageInputStreamImpl#finalize}. */
    @Override
    protected void finalize() throws Throwable {
        // The base class already closes its own part; there is nothing to do here but let it work.
        super.finalize();
    }
}
