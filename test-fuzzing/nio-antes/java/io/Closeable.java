package java.io;

// KajiLibrary's java.io.Closeable — a source or destination of data that can be closed to
// release the underlying resource. Refines AutoCloseable for the I/O world (in the JDK its
// close() is idempotent and declared to throw IOException; we don't model IOException yet).
public interface Closeable extends AutoCloseable {

    void close() throws IOException;
}
