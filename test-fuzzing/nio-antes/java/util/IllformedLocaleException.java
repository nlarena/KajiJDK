package java.util;

// An argument of `Locale.Builder` does not satisfy BCP 47's syntax.
//
// Besides the message it carries the **index** where it broke, which is what tells it from any old
// IllegalArgumentException: when validating a language tag the error is almost always in a concrete
// subtag of a long string, and saying "at character 12" is the difference between a useful message
// and one that forces guesswork.
public class IllformedLocaleException extends RuntimeException {

    // Where it broke, or -1 if it is not known.
    private int errorIndex = -1;

    // With neither message nor index.
    public IllformedLocaleException() {
        super();
    }

    // With the given message and no index.
    public IllformedLocaleException(String message) {
        super(message);
    }

    // With the given message and the index where the error was detected.
    public IllformedLocaleException(String message, int errorIndex) {
        super(message + (errorIndex < 0 ? "" : " [at index " + errorIndex + "]"));
        this.errorIndex = errorIndex;
    }

    // The index where it broke, or -1.
    public int getErrorIndex() {
        return this.errorIndex;
    }
}
