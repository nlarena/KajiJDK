package java.sql;

/**
 * KajiLibrary's java.sql.Blob -- a large binary value, **by reference**.
 *
 * <p>The reason it exists instead of a `byte[]` is in {@link #getBinaryStream}: a BLOB can weigh
 * gigabytes, and fetching it whole to read the first thousand bytes would be absurd. This object is
 * a client-side pointer to a value that keeps living in the database, and only what is asked for is
 * materialized.
 *
 * <p>That explains {@link #free}: the pointer ties up resources on the other side, and waiting for
 * the collector to release them may be too late.
 *
 * <p>Positions are counted **from one**, like everything in JDBC.
 */
public interface Blob {

    /** How many bytes it has. */
    long length() throws SQLException;

    /** `length` bytes starting at `pos`. */
    byte[] getBytes(long pos, int length) throws SQLException;

    /** The whole content, as a stream. */
    java.io.InputStream getBinaryStream() throws SQLException;

    /** `length` bytes from `pos`, as a stream. */
    java.io.InputStream getBinaryStream(long pos, long length) throws SQLException;

    /** Where `pattern` starts from `start`, or -1. */
    long position(byte[] pattern, long start) throws SQLException;

    /** The same, searching for another BLOB's content. */
    long position(Blob pattern, long start) throws SQLException;

    /** Writes those bytes at `pos`; returns how many it wrote. */
    int setBytes(long pos, byte[] bytes) throws SQLException;

    /** The same, taking a slice of the array. */
    int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException;

    /** A stream to write from `pos`. */
    java.io.OutputStream setBinaryStream(long pos) throws SQLException;

    /** Truncates it to `len` bytes. */
    void truncate(long len) throws SQLException;

    /** Releases the pointer's resources. */
    void free() throws SQLException;
}
