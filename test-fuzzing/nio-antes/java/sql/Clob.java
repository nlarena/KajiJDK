package java.sql;

/**
 * KajiLibrary's java.sql.Clob -- a large text, by reference.
 *
 * <p>The same as {@link Blob} for characters, and the difference matters: here positions and
 * lengths are counted in **characters**, not in bytes, so in a multi-byte encoding they do not
 * match the byte counts. Confusing the two units is the classic mistake with this interface. (This
 * note said they depend on the database's encoding; counting characters is what keeps them from
 * depending on it.)
 */
public interface Clob {

    /** How many characters it has. */
    long length() throws SQLException;

    /** `length` characters starting at `pos`. */
    String getSubString(long pos, int length) throws SQLException;

    /** The whole content, as a reader. */
    java.io.Reader getCharacterStream() throws SQLException;

    /** `length` characters from `pos`. */
    java.io.Reader getCharacterStream(long pos, long length) throws SQLException;

    /** The content as an ASCII stream. */
    java.io.InputStream getAsciiStream() throws SQLException;

    /** Where `searchstr` starts from `start`, or -1. */
    long position(String searchstr, long start) throws SQLException;

    /** The same, searching for another CLOB's content. */
    long position(Clob searchstr, long start) throws SQLException;

    /** Writes that text at `pos`; returns how many characters it wrote. */
    int setString(long pos, String str) throws SQLException;

    /** The same, taking a slice. */
    int setString(long pos, String str, int offset, int len) throws SQLException;

    /** An ASCII stream to write from `pos`. */
    java.io.OutputStream setAsciiStream(long pos) throws SQLException;

    /** A writer to write from `pos`. */
    java.io.Writer setCharacterStream(long pos) throws SQLException;

    /** Truncates it to `len` characters. */
    void truncate(long len) throws SQLException;

    /** Releases the pointer's resources. */
    void free() throws SQLException;
}
