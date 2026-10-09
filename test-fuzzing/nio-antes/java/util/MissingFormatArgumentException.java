package java.util;

// The format string asks for an argument that was not supplied.
public class MissingFormatArgumentException extends IllegalFormatException {

    private final String specifier;

    public MissingFormatArgumentException(String s) {
        this.specifier = s;
    }

    public String getFormatSpecifier() {
        return specifier;
    }

    public String getMessage() {
        return "Format specifier '" + specifier + "'";
    }
}
