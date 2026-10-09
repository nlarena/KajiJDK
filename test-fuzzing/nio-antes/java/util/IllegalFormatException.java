package java.util;

// The root of the format-string exception family. Every way a format string can be wrong —
// an unknown conversion, a flag that clashes with it, a missing width — arrives as one of its
// subclasses, so a caller can catch them all at once.
//
// Like the JDK's, it has no public constructor: it is a category, never thrown directly.
public class IllegalFormatException extends IllegalArgumentException {

    IllegalFormatException() {
        super();
    }

    IllegalFormatException(String message) {
        super(message);
    }
}
