package java.io;

import java.io.IOException;

// Base class for the failures a byte<->char converter can hit: a byte sequence that is not
// valid in the source charset, or a character with no representation in the target one.
public class CharConversionException extends IOException {

    public CharConversionException() {
    }

    public CharConversionException(String message) {
        super(message);
    }
}
