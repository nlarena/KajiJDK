package java.util;

// Thrown when a Formatter is used after being closed. Not an IllegalFormatException: the
// format string is fine, it is the formatter's state that is wrong.
public class FormatterClosedException extends IllegalStateException {

    public FormatterClosedException() {
        super();
    }
}
