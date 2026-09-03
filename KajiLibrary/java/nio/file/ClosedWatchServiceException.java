package java.nio.file;

// Se uso un `WatchService` que ya estaba cerrado. KajiJDK no tiene servicio de vigilancia, asi que
// nunca la levanta.
public class ClosedWatchServiceException extends IllegalStateException {

    private static final long serialVersionUID = 1917780725909606046L;

    /** Sin mensaje. */
    public ClosedWatchServiceException() {
    }
}
