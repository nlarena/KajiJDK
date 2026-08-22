package java.util.zip;

import java.io.IOException;

// A malformed archive. It extends `IOException` rather than standing alone because, from a
// caller's point of view, a corrupt zip and an unreadable file fail the same read.
public class ZipException extends IOException {

    public ZipException() {
        super();
    }

    public ZipException(String message) {
        super(message);
    }
}
