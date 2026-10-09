package javax.crypto;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decrypts --or encrypts-- whatever is read from another stream.
 *
 * <h2>Why the lag</h2>
 *
 * <p>A block cipher hands nothing over until it has a whole block, so this stream has to read ahead
 * in order to hand anything over. It reads 512 bytes at a time, gives them to the cipher, and keeps
 * whatever comes out until somebody asks for it. It is the reason {@link #available} hardly ever
 * matches what is left in the stream underneath.
 *
 * <h2>Closing is not optional</h2>
 *
 * <p>{@link #close} is what calls {@code doFinal}, and {@code doFinal} is where the padding --and,
 * in an authenticated cipher, the tag-- is checked. A program that reads to the end and does not
 * close never learns that the message was altered.
 *
 * <p>And there is something worse, particular to this class: if {@code doFinal} fails,
 * {@link #close} swallows the exception. It is what the JDK does and it cannot be changed without
 * breaking whoever depends on it, but it means this class is no good for authenticated data: one has
 * to use {@link Cipher} directly and look at what it throws.
 *
 * <h2>No marks</h2>
 *
 * <p>{@link #markSupported} always answers false. Going back would mean rewinding the cipher's
 * state, which in a chained mode depends on everything that happened before.
 *
 * @since 1.4
 */
public class CipherInputStream extends FilterInputStream {

    private static final int SIZE = 512;

    private final Cipher cipher;
    private final byte[] input = new byte[SIZE];

    private byte[] output;
    private int from;
    private int to;
    private boolean finished;
    private boolean closed;

    /**
     * One that puts what is read through that cipher.
     *
     * @param is where to read from
     * @param c the cipher, already configured
     */
    public CipherInputStream(InputStream is, Cipher c) {
        super(is);
        this.cipher = c;
    }

    /**
     * One that encrypts nothing.
     *
     * <p>It is protected because it only makes sense for a subclass that wants the stream behaviour
     * without the transformation.
     *
     * @param is where to read from
     */
    protected CipherInputStream(InputStream is) {
        this(is, new NullCipher());
    }

    /**
     * The next byte.
     *
     * @return the byte, between 0 and 255, or -1 if it ended
     * @throws IOException if the read fails
     */
    @Override
    public int read() throws IOException {
        if (this.from >= this.to && !fill()) {
            return -1;
        }
        final int b = this.output[this.from] & 0xff;
        this.from++;
        return b;
    }

    /**
     * Fills the array.
     *
     * @param b where to write
     * @return how many bytes were read, or -1 if it ended
     * @throws IOException if the read fails
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Fills part of the array.
     *
     * @param b where to write
     * @param off from where
     * @param len how many at most
     * @return how many bytes were read, or -1 if it ended
     * @throws IOException if the read fails
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.from >= this.to && !fill()) {
            return -1;
        }
        if (len <= 0) {
            return 0;
        }
        final int howMany = Math.min(len, this.to - this.from);
        System.arraycopy(this.output, this.from, b, off, howMany);
        this.from += howMany;
        return howMany;
    }

    /**
     * Discards bytes.
     *
     * <p>It only skips what is already decrypted and waiting: there is no sense in reading and
     * decrypting more just to throw it away.
     *
     * @param n how many
     * @return how many were skipped
     * @throws IOException if it fails
     */
    @Override
    public long skip(long n) throws IOException {
        final long available = this.to - this.from;
        final long howMany = n > available ? available : n;
        if (howMany <= 0) {
            return 0;
        }
        this.from += (int) howMany;
        return howMany;
    }

    /**
     * How much can be read without blocking.
     *
     * @return what is already decrypted and waiting
     * @throws IOException if it fails
     */
    @Override
    public int available() throws IOException {
        return this.to - this.from;
    }

    /**
     * Closes the stream underneath and finishes the cipher.
     *
     * <p>Whatever {@code doFinal} throws is discarded: see the class note.
     *
     * @throws IOException if closing the stream underneath fails
     */
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.in.close();
        try {
            this.cipher.doFinal();
        } catch (BadPaddingException e) {
            // The JDK swallows it, and changing that would break whoever depends on it.
        } catch (IllegalBlockSizeException e) {
            // Likewise.
        }
        this.from = 0;
        this.to = 0;
    }

    /**
     * Whether one can go back.
     *
     * @return false: rewinding the cipher is not possible
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /** Reads from the stream underneath and puts it through the cipher; false when nothing is left. */
    private boolean fill() throws IOException {
        while (true) {
            if (this.finished) {
                return false;
            }
            final int read = this.in.read(this.input, 0, SIZE);
            byte[] came;
            if (read == -1) {
                this.finished = true;
                try {
                    came = this.cipher.doFinal();
                } catch (BadPaddingException e) {
                    came = null;
                } catch (IllegalBlockSizeException e) {
                    came = null;
                }
            } else {
                came = this.cipher.update(this.input, 0, read);
            }
            if (came != null && came.length > 0) {
                this.output = came;
                this.from = 0;
                this.to = came.length;
                return true;
            }
            if (this.finished) {
                return false;
            }
        }
    }
}
