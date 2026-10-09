package javax.sql.rowset.serial;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialBlob -- an in-memory copy of a BLOB.
 *
 * <p>A real {@link Blob} is a <b>pointer</b> to data that lives on the server and is only valid
 * while the transaction that produced it stays open. This class copies the bytes into memory, and
 * with that the datum survives the connection, can be serialized and can be sent over the network.
 *
 * <p>The price is obvious and it has to be said: <b>all</b> the content stays in memory. A BLOB of
 * several gigabytes does not fit, and there one has to keep the pointer and read it in parts.
 *
 * <h2>Positions start at 1</h2>
 *
 * <p>It is SQL's convention and not Java's, and it is the classic source of mistakes in this
 * package: {@code getBytes(1, 10)} returns the first ten bytes, not the second to the eleventh. A
 * position 0 or negative is an error.
 */
public class SerialBlob implements Blob, Serializable, Cloneable {

    private static final long serialVersionUID = -8144641928112860441L;

    /** The copy. */
    private byte[] buf;

    /** How many bytes count; it can be fewer than {@code buf.length} after a truncation. */
    private long len;

    /** Null once it was freed with {@link #free}. */
    private boolean freed = false;

    /** Copies those bytes. */
    public SerialBlob(byte[] b) throws SerialException, SQLException {
        if (b == null) {
            throw new SQLException("Invalid Blob object. The byte array is null");
        }
        this.buf = new byte[b.length];
        System.arraycopy(b, 0, this.buf, 0, b.length);
        this.len = b.length;
    }

    /** Copies the content of a server BLOB. */
    public SerialBlob(Blob blob) throws SerialException, SQLException {
        if (blob == null) {
            throw new SQLException("Cannot instantiate a SerialBlob object with a null Blob object");
        }
        long size = blob.length();
        byte[] bytes = blob.getBytes(1L, (int) size);
        this.buf = bytes;
        this.len = size;
    }

    /**
     * A slice.
     *
     * @param pos the first position, starting at 1
     * @throws SerialException if the slice goes outside the content
     */
    public byte[] getBytes(long pos, int length) throws SerialException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        int available = (int) (this.len - (pos - 1));
        int take = (length > available) ? available : length;
        if (take < 0) {
            throw new SerialException("Invalid length in BLOB object set");
        }
        byte[] out = new byte[take];
        System.arraycopy(this.buf, (int) (pos - 1), out, 0, take);
        return out;
    }

    /** How many bytes it has. */
    public long length() throws SerialException {
        check();
        return this.len;
    }

    /** A stream over the copy. */
    public InputStream getBinaryStream() throws SerialException {
        check();
        return new ByteArrayInputStream(this.buf, 0, (int) this.len);
    }

    /**
     * Looks for that pattern from that position.
     *
     * @return the position where it starts, starting at 1, or -1
     */
    public long position(byte[] pattern, long start) throws SerialException, SQLException {
        check();
        if (start < 1 || start > this.len || pattern == null) {
            return -1;
        }
        int from = (int) (start - 1);
        int limit = (int) this.len - pattern.length;
        int i = from;
        while (i <= limit) {
            int j = 0;
            while (j < pattern.length && this.buf[i + j] == pattern[j]) {
                j = j + 1;
            }
            if (j == pattern.length) {
                return i + 1;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Likewise, with the pattern in another BLOB. */
    public long position(Blob pattern, long start) throws SerialException, SQLException {
        check();
        if (pattern == null) {
            return -1;
        }
        return position(pattern.getBytes(1L, (int) pattern.length()), start);
    }

    /** Writes over it, from that position. */
    public int setBytes(long pos, byte[] bytes) throws SerialException, SQLException {
        return setBytes(pos, bytes, 0, bytes == null ? 0 : bytes.length);
    }

    /**
     * Writes a slice of the array over it.
     *
     * @throws SerialException if it does not fit in the current content: this copy does not grow
     */
    public int setBytes(long pos, byte[] bytes, int offset, int length)
        throws SerialException, SQLException {
        check();
        if (bytes == null) {
            throw new SerialException("Invalid null value for bytes");
        }
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        if (offset < 0 || offset + length > bytes.length) {
            throw new SerialException("Invalid offset in byte array set");
        }
        if ((pos - 1) + length > this.len) {
            throw new SerialException("Buffer is not sufficient to hold the value");
        }
        System.arraycopy(bytes, offset, this.buf, (int) (pos - 1), length);
        return length;
    }

    /**
     * Cannot be written through a stream.
     *
     * @throws SerialException always: an output stream could grow, and this copy has a fixed size
     *     since it was built. The JDK throws only when it was built from a byte array; built from a
     *     {@link Blob} it delegates to that one's {@code setBinaryStream}. This copy does not keep
     *     the original, so it has nothing to delegate to
     */
    public OutputStream setBinaryStream(long pos) throws SerialException, SQLException {
        throw new SerialException("Unsupported operation. SerialBlob cannot return a writable "
            + "binary stream, unless instantiated with a Blob object.");
    }

    /** Truncates to that many bytes. */
    public void truncate(long length) throws SerialException {
        check();
        if (length > this.len) {
            throw new SerialException("Length more than what can be truncated");
        }
        if (length == 0) {
            this.buf = new byte[0];
            this.len = 0;
            return;
        }
        byte[] smaller = new byte[(int) length];
        System.arraycopy(this.buf, 0, smaller, 0, (int) length);
        this.buf = smaller;
        this.len = length;
    }

    /** A stream over a slice. */
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        if (length < 1 || length > this.len - pos + 1) {
            throw new SerialException(
                "length is < 1 or pos + length > total number of bytes");
        }
        return new ByteArrayInputStream(this.buf, (int) (pos - 1), (int) length);
    }

    /**
     * Lets go of the copy.
     *
     * <p>After this any other method throws. It is what the {@link Blob} contract does and it makes
     * sense here even though there are no server resources to release: it frees the memory, which
     * in a large BLOB is precisely the resource.
     */
    public void free() throws SQLException {
        this.buf = null;
        this.len = 0;
        this.freed = true;
    }

    /** Equal if they have the same bytes. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialBlob)) {
            return false;
        }
        SerialBlob that = (SerialBlob) obj;
        if (this.len != that.len) {
            return false;
        }
        if (this.buf == null || that.buf == null) {
            return this.buf == that.buf;
        }
        int i = 0;
        while (i < this.len) {
            if (this.buf[i] != that.buf[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Consistent with {@link #equals}. */
    public int hashCode() {
        int hash = 31;
        int i = 0;
        while (i < this.len) {
            hash = hash * 31 + this.buf[i];
            i = i + 1;
        }
        return hash;
    }

    /** A copy with its own bytes. */
    public Object clone() {
        try {
            SerialBlob copy = new SerialBlob(new byte[0]);
            if (this.buf != null) {
                copy.buf = new byte[this.buf.length];
                System.arraycopy(this.buf, 0, copy.buf, 0, this.buf.length);
            } else {
                copy.buf = null;
            }
            copy.len = this.len;
            copy.freed = this.freed;
            return copy;
        } catch (SQLException e) {
            return null;
        }
    }

    /** That it has not been freed. */
    private void check() throws SerialException {
        if (this.freed || this.buf == null) {
            throw new SerialException("Error: You cannot call a method on a SerialBlob instance "
                + "once free() has been called.");
        }
    }
}
