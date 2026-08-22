package java.io;

import java.io.IOException;

// Signals that a stream ended in the middle of something the reader still needed. Note the
// contrast with the -1 convention of InputStream.read(): a single byte read reports "end"
// as a value, but a multi-byte read (an int, a UTF string) has no way to report a partial
// result, so it throws instead.
public class EOFException extends IOException {

    public EOFException() {
    }

    public EOFException(String message) {
        super(message);
    }
}
