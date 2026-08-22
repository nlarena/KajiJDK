package java.io;

import java.io.IOException;

// Thrown when a blocking I/O operation was interrupted before finishing. The public
// `bytesTransferred` field is the unusual part of the design: since the transfer was cut
// short rather than failed outright, the exception has to carry the partial progress, or
// the caller could not resume without losing or duplicating data.
public class InterruptedIOException extends IOException {

    public int bytesTransferred;

    public InterruptedIOException() {
        this.bytesTransferred = 0;
    }

    public InterruptedIOException(String message) {
        super(message);
        this.bytesTransferred = 0;
    }
}
