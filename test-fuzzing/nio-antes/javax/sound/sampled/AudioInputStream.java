package javax.sound.sampled;

import java.io.IOException;
import java.io.InputStream;

/**
 * KajiLibrary's javax.sound.sampled.AudioInputStream -- a stream of bytes that knows its format.
 *
 * <p>An {@link InputStream} with an {@link AudioFormat} attached and a length in frames. That is
 * all, and it is enough: anything that produces or consumes audio in this package speaks this type.
 *
 * <h2>It reads whole frames</h2>
 *
 * <p>It is the part to keep in mind. {@link #read(byte[], int, int)} <b>rounds down</b> to the
 * frame: asking for 5 bytes of a format of 4 bytes per frame returns 4, not 5. And {@link #read()}
 * throws {@link IOException} if the frame takes more than one byte.
 *
 * <p>It is not a whim: half a frame means nothing, and returning it would leave the stream
 * misaligned and everything that followed would sound like noise.
 *
 * <h2>{@link #skip} too</h2>
 *
 * <p>It rounds the same way. Skipping an amount that is not a multiple of the frame skips less,
 * never more.
 */
public class AudioInputStream extends InputStream {

    /** Where the bytes come from. */
    private final InputStream stream;

    /** Which format they have. */
    protected AudioFormat format;

    /** How many frames there are, or {@link AudioSystem#NOT_SPECIFIED}. */
    protected long frameLength;

    /** How many bytes a frame takes. */
    protected int frameSize;

    /** At which frame the reading is. */
    protected long framePos;

    /** Where it was when marked. */
    private long markpos;

    /** What was left over from an incomplete frame between two reads. */
    private byte[] pushBackBuffer = null;

    /** How many bytes are kept there. */
    private int pushBackLen = 0;

    /**
     * @param stream where to read from
     * @param format which format the bytes have
     * @param length how many frames, or {@link AudioSystem#NOT_SPECIFIED}
     */
    public AudioInputStream(InputStream stream, AudioFormat format, long length) {
        this.stream = stream;
        this.format = format;
        this.frameLength = length;
        this.frameSize = format.getFrameSize();
        if (this.frameSize == AudioSystem.NOT_SPECIFIED || this.frameSize <= 0) {
            this.frameSize = 1;
        }
        this.framePos = 0;
        this.markpos = 0;
    }

    /**
     * A stream over whatever that input line captures.
     *
     * <p>The length is {@link AudioSystem#NOT_SPECIFIED}: a capture line has no end.
     */
    public AudioInputStream(TargetDataLine line) {
        this(new TargetDataLineInputStream(line), line.getFormat(),
             AudioSystem.NOT_SPECIFIED);
    }

    /** Which format the bytes have. */
    public AudioFormat getFormat() {
        return this.format;
    }

    /** How many frames, or {@link AudioSystem#NOT_SPECIFIED}. */
    public long getFrameLength() {
        return this.frameLength;
    }

    /**
     * One byte.
     *
     * @throws IOException if a frame takes more than one byte; see the class note
     */
    @Override
    public int read() throws IOException {
        if (this.frameSize != 1) {
            throw new IOException("cannot read a single byte if frame size > 1");
        }
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        if (n <= 0) {
            return -1;
        }
        return one[0] & 0xFF;
    }

    /** Everything that fits in the array, rounded to the frame. */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Up to {@code len} bytes, rounded down to the frame.
     *
     * <p>See the class note on why.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.frameSize != 1) {
            len = len - (len % this.frameSize);
        }
        if (len == 0) {
            return 0;
        }
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            long left = (this.frameLength - this.framePos) * this.frameSize;
            if (left <= 0) {
                return -1;
            }
            if (len > left) {
                len = (int) left;
            }
        }
        int read = this.stream.read(b, off, len);
        if (read < 0) {
            return -1;
        }
        // If the underlying stream stopped halfway through a frame, it is completed before
        // returning: the contract of this class is that half a frame never comes out.
        if (this.frameSize != 1 && read % this.frameSize != 0) {
            read = completeFrame(b, off, read);
        }
        this.framePos = this.framePos + read / this.frameSize;
        return read;
    }

    /**
     * Reads whatever is missing to complete the last frame; if it does not arrive, discards the
     * rest.
     */
    private int completeFrame(byte[] b, int off, int read) throws IOException {
        int missing = this.frameSize - (read % this.frameSize);
        int got = 0;
        while (got < missing) {
            int n = this.stream.read(b, off + read + got, missing - got);
            if (n < 0) {
                return read - (read % this.frameSize);
            }
            got = got + n;
        }
        return read + missing;
    }

    /** Skips, rounded down to the frame. */
    @Override
    public long skip(long n) throws IOException {
        if (this.frameSize != 1) {
            n = n - (n % this.frameSize);
        }
        if (n <= 0) {
            return 0;
        }
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            long left = (this.frameLength - this.framePos) * this.frameSize;
            if (n > left) {
                n = left;
            }
        }
        long skipped = this.stream.skip(n);
        if (skipped % this.frameSize != 0) {
            skipped = skipped - (skipped % this.frameSize);
        }
        this.framePos = this.framePos + skipped / this.frameSize;
        return skipped;
    }

    /** How many bytes can be read without blocking, rounded to the frame. */
    @Override
    public int available() throws IOException {
        int n = this.stream.available();
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            long left = (this.frameLength - this.framePos) * this.frameSize;
            if (n > left) {
                n = (int) left;
            }
        }
        if (this.frameSize != 1) {
            n = n - (n % this.frameSize);
        }
        return n;
    }

    /** Closes the underlying stream. */
    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    /** Marks, if the underlying stream knows how. */
    @Override
    public void mark(int readlimit) {
        this.stream.mark(readlimit);
        if (markSupported()) {
            this.markpos = this.framePos;
        }
    }

    /**
     * Goes back to the mark.
     *
     * @throws IOException if the underlying stream does not support marks
     */
    @Override
    public void reset() throws IOException {
        this.stream.reset();
        this.framePos = this.markpos;
    }

    /** Whether the underlying stream supports marks. */
    @Override
    public boolean markSupported() {
        return this.stream.markSupported();
    }

    /**
     * The bridge between a capture line and an {@link InputStream}.
     *
     * <p>Package access: it exists only for the constructor that takes a {@link TargetDataLine}. It
     * starts the line on the first read, not when constructed, because a started line is capturing
     * and filling its buffer even if nobody reads.
     */
    private static final class TargetDataLineInputStream extends InputStream {

        /** Where it captures from. */
        private final TargetDataLine line;

        TargetDataLineInputStream(TargetDataLine line) {
            this.line = line;
        }

        @Override
        public int available() throws IOException {
            return this.line.available();
        }

        @Override
        public void close() throws IOException {
            this.line.close();
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!this.line.isActive()) {
                this.line.start();
            }
            return this.line.read(b, off, len);
        }
    }
}
