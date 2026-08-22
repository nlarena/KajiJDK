package java.io;

import java.io.IOException;

// Thrown when a request to flush a buffer all the way down to the physical device could not
// be honoured. Distinct from a plain write failure: the bytes may well have reached the OS,
// they just are not guaranteed to have reached the disk.
public class SyncFailedException extends IOException {

    public SyncFailedException(String message) {
        super(message);
    }
}
