package java.util;

// A flag appears more than once in the same specifier.
public class DuplicateFormatFlagsException extends IllegalFormatException {

    private final String flags;

    public DuplicateFormatFlagsException(String f) {
        this.flags = f;
    }

    public String getFlags() {
        return flags;
    }

    public String getMessage() {
        return flags;
    }
}
