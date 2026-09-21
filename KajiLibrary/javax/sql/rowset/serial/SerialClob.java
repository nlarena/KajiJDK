package javax.sql.rowset.serial;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialClob -- an in-memory copy of a CLOB.
 *
 * <p>The same as {@link SerialBlob} but with characters. The same two warnings hold: all the
 * content stays in memory, and positions start at 1.
 *
 * <p>The difference that matters: it keeps {@code char[]} and not bytes, so the encoding was
 * already resolved when copying it. That is why {@link #getAsciiStream} has to encode again, and it
 * only works if the content really is ASCII -- with an accent inside, what comes out is not what
 * went in.
 */
public class SerialClob implements Clob, Serializable, Cloneable {

    private static final long serialVersionUID = -1662519690087375313L;

    /** The copy. */
    private char[] buf;

    /** How many characters count. */
    private long len;

    /** Whether it was already freed. */
    private boolean freed = false;

    /** Copies those characters. */
    public SerialClob(char[] ch) throws SerialException, SQLException {
        if (ch == null) {
            throw new SQLException("Invalid Clob object. The char array is null");
        }
        this.buf = new char[ch.length];
        System.arraycopy(ch, 0, this.buf, 0, ch.length);
        this.len = ch.length;
    }

    /** Copies the content of a server CLOB. */
    public SerialClob(Clob clob) throws SerialException, SQLException {
        if (clob == null) {
            throw new SQLException("Cannot instantiate a SerialClob object with a null Clob object");
        }
        long size = clob.length();
        String text = clob.getSubString(1L, (int) size);
        this.buf = text.toCharArray();
        this.len = size;
    }

    /** How many characters it has. */
    public long length() throws SerialException {
        check();
        return this.len;
    }

    /** A reader over the copy. */
    public Reader getCharacterStream() throws SerialException {
        check();
        return new CharArrayReader(this.buf, 0, (int) this.len);
    }

    /**
     * A byte stream, taking each character as a byte.
     *
     * <p>See the class note: it only works if the content is ASCII. JDK 25 does not do this: unless
     * it was built from a {@link Clob}, whose {@code getAsciiStream} it delegates to, it throws
     * {@link SerialException}.
     */
    public InputStream getAsciiStream() throws SerialException, SQLException {
        check();
        byte[] bytes = new byte[(int) this.len];
        int i = 0;
        while (i < this.len) {
            bytes[i] = (byte) this.buf[i];
            i = i + 1;
        }
        return new java.io.ByteArrayInputStream(bytes);
    }

    /**
     * A slice, as a string.
     *
     * @param pos the first position, starting at 1
     */
    public String getSubString(long pos, int length) throws SerialException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in SerialClob object set");
        }
        if (length < 0 || (pos - 1) + length > this.len) {
            throw new SerialException("Invalid position and substring length");
        }
        return new String(this.buf, (int) (pos - 1), length);
    }

    /**
     * Looks for that text from that position.
     *
     * @return the position where it starts, starting at 1, or -1
     */
    public long position(String searchStr, long start) throws SerialException, SQLException {
        check();
        if (start < 1 || start > this.len || searchStr == null) {
            return -1;
        }
        String whole = new String(this.buf, 0, (int) this.len);
        int found = whole.indexOf(searchStr, (int) (start - 1));
        return (found < 0) ? -1 : found + 1;
    }

    /** Likewise, with the text in another CLOB. */
    public long position(Clob searchStr, long start) throws SerialException, SQLException {
        check();
        if (searchStr == null) {
            return -1;
        }
        return position(searchStr.getSubString(1L, (int) searchStr.length()), start);
    }

    /** Writes over it, from that position. */
    public int setString(long pos, String str) throws SerialException {
        return setString(pos, str, 0, str == null ? 0 : str.length());
    }

    /**
     * Writes a slice of the text over it.
     *
     * @throws SerialException if it does not fit in the current content: this copy does not grow
     */
    public int setString(long pos, String str, int offset, int length) throws SerialException {
        check();
        if (str == null) {
            throw new SerialException("Invalid null value for string");
        }
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in Clob object set");
        }
        if (offset < 0 || offset + length > str.length()) {
            throw new SerialException("Invalid offset in string set");
        }
        if ((pos - 1) + length > this.len) {
            throw new SerialException(
                "Buffer is not sufficient to hold the value");
        }
        str.getChars(offset, offset + length, this.buf, (int) (pos - 1));
        return length;
    }

    /**
     * Cannot be written through a stream.
     *
     * @throws SerialException always; see {@link SerialBlob#setBinaryStream}
     */
    public OutputStream setAsciiStream(long pos) throws SerialException, SQLException {
        throw new SerialException("Unsupported operation. SerialClob cannot return a writable "
            + "ascii stream, unless instantiated with a Clob object.");
    }

    /**
     * Cannot be written through a writer.
     *
     * @throws SerialException always; see {@link SerialBlob#setBinaryStream}
     */
    public Writer setCharacterStream(long pos) throws SerialException, SQLException {
        throw new SerialException("Unsupported operation. SerialClob cannot return a writable "
            + "character stream, unless instantiated with a Clob object.");
    }

    /** Truncates to that many characters. */
    public void truncate(long length) throws SerialException {
        check();
        if (length > this.len) {
            throw new SerialException("Length more than what can be truncated");
        }
        if (length == 0) {
            this.buf = new char[0];
            this.len = 0;
            return;
        }
        char[] smaller = new char[(int) length];
        System.arraycopy(this.buf, 0, smaller, 0, (int) length);
        this.buf = smaller;
        this.len = length;
    }

    /** A reader over a slice. */
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in SerialClob object set");
        }
        if (length < 1 || length > this.len - pos + 1) {
            throw new SerialException("Invalid length or pos + length > total number of characters");
        }
        return new CharArrayReader(this.buf, (int) (pos - 1), (int) length);
    }

    /** Lets go of the copy; see {@link SerialBlob#free}. */
    public void free() throws SQLException {
        this.buf = null;
        this.len = 0;
        this.freed = true;
    }

    /** Equal if they have the same characters. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialClob)) {
            return false;
        }
        SerialClob that = (SerialClob) obj;
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

    /** A copy with its own characters. */
    public Object clone() {
        try {
            SerialClob copy = new SerialClob(new char[0]);
            if (this.buf != null) {
                copy.buf = new char[this.buf.length];
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
            throw new SerialException("Error: You cannot call a method on a SerialClob instance "
                + "once free() has been called.");
        }
    }
}
