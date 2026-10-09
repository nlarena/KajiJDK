package javax.crypto;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Encrypts --or decrypts-- whatever is written into another stream.
 *
 * <h2>Closing is part of the message</h2>
 *
 * <p>{@link #close} is what calls {@code doFinal}, and {@code doFinal} is what writes the last block
 * with its padding. A program that writes everything and does not close produces a truncated file:
 * not a shorter one, one that cannot be decrypted.
 *
 * <p>{@link #flush} is not enough. It empties whatever the stream underneath has pending, but it
 * cannot force the cipher to hand over an incomplete block --if it could, it would not be a block
 * cipher.
 *
 * @since 1.4
 */
public class CipherOutputStream extends FilterOutputStream {

    private final Cipher cipher;
    private final byte[] one = new byte[1];
    private boolean closed;

    /**
     * One that puts what is written through that cipher.
     *
     * @param os where to write
     * @param c the cipher, already configured
     */
    public CipherOutputStream(OutputStream os, Cipher c) {
        super(os);
        this.cipher = c;
    }

    /**
     * One that encrypts nothing.
     *
     * @param os where to write
     */
    protected CipherOutputStream(OutputStream os) {
        this(os, new NullCipher());
    }

    /**
     * Writes one byte.
     *
     * @param b the byte
     * @throws IOException if the write fails
     */
    @Override
    public void write(int b) throws IOException {
        this.one[0] = (byte) b;
        write(this.one, 0, 1);
    }

    /**
     * Writes an array.
     *
     * @param b the data
     * @throws IOException if the write fails
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes part of an array.
     *
     * @param b the data
     * @param off from where
     * @param len how many
     * @throws IOException if the write fails
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        final byte[] came = this.cipher.update(b, off, len);
        if (came != null && came.length > 0) {
            this.out.write(came);
        }
    }

    /**
     * Empties what the stream underneath has pending.
     *
     * <p>It does not force the cipher to hand over an incomplete block: see the class note.
     *
     * @throws IOException if it fails
     */
    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * Finishes the cipher, writes the last of it, and closes the stream underneath.
     *
     * @throws IOException if the write or the close fails, or if the cipher throws
     */
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            final byte[] came = this.cipher.doFinal();
            if (came != null && came.length > 0) {
                this.out.write(came);
            }
        } catch (BadPaddingException e) {
            // Encrypting it does not happen; decrypting it does, and it means the message is
            // altered.
        } catch (IllegalBlockSizeException e) {
            // Likewise.
        }
        this.out.flush();
        this.out.close();
    }
}
