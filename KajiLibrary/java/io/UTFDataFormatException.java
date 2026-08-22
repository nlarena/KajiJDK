package java.io;

import java.io.IOException;

// Thrown when bytes that claimed to be "modified UTF-8" (the encoding DataInput.readUTF and
// the class-file format use) are malformed. Modified UTF-8 is not standard UTF-8: it encodes
// U+0000 as two bytes so that no embedded NUL ever appears, and it writes a supplementary
// character as a surrogate pair of three-byte forms rather than one four-byte form.
public class UTFDataFormatException extends IOException {

    public UTFDataFormatException() {
    }

    public UTFDataFormatException(String message) {
        super(message);
    }
}
