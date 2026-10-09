package java.lang;

// KajiLibrary's java.lang.NumberFormatException — a string could not be parsed as a number.
// It extends IllegalArgumentException, which is the honest classification: the argument was
// wrong, and the caller could have checked it.
public class NumberFormatException extends IllegalArgumentException {

    public NumberFormatException() {
    }

    public NumberFormatException(String message) {
        super(message);
    }
}
