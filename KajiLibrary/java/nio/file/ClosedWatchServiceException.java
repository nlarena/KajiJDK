package java.nio.file;

// A `WatchService` that was already closed was used. KajiJDK has no watch service, so it never
// throws it.
public class ClosedWatchServiceException extends IllegalStateException {

    private static final long serialVersionUID = 1917780725909606046L;

    /** With no message. */
    public ClosedWatchServiceException() {
    }
}
