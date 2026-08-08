package java.lang;

// KajiLibrary's java.lang.AutoCloseable — a resource that is closed automatically at the
// end of a try-with-resources block. `close()` may throw a checked exception.
public interface AutoCloseable {

    void close() throws Exception;
}
