package java.util;

// The flags in a specifier are mutually incompatible.
public class IllegalFormatFlagsException extends IllegalFormatException {

    private final String flags;

    public IllegalFormatFlagsException(String f) {
        this.flags = f;
    }

    public String getFlags() {
        return flags;
    }

    public String getMessage() {
        return flags;
    }
}
