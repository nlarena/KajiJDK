package java.util;

// A flag in the specifier is not one this formatter knows.
public class UnknownFormatFlagsException extends IllegalFormatException {

    private final String flags;

    public UnknownFormatFlagsException(String f) {
        this.flags = f;
    }

    public String getFlags() {
        return flags;
    }

    public String getMessage() {
        return "Flags = " + flags;
    }
}
