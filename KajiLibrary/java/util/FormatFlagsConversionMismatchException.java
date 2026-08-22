package java.util;

// A flag is valid on its own but not for this conversion.
public class FormatFlagsConversionMismatchException extends IllegalFormatException {

    private final String flags;
    private final char conversion;

    public FormatFlagsConversionMismatchException(String f, char c) {
        this.flags = f;
        this.conversion = c;
    }

    public String getFlags() {
        return flags;
    }

    public char getConversion() {
        return conversion;
    }

    public String getMessage() {
        return "Conversion = " + conversion + ", Flags = " + flags;
    }
}
