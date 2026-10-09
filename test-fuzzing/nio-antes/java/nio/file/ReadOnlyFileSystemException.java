package java.nio.file;

// Writing to a filesystem mounted read-only was attempted.
//
// **It inherits from `UnsupportedOperationException`, not from `IOException`**, and the distinction
// is useful: it is not that the write failed, it is that on that system the operation **does not
// exist**. Retrying makes no sense.
public class ReadOnlyFileSystemException extends UnsupportedOperationException {

    private static final long serialVersionUID = -6822409595617487197L;

    /** With no message. */
    public ReadOnlyFileSystemException() {
    }
}
