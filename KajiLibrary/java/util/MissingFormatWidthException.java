package java.util;

// The conversion requires a width and the specifier gives none.
public class MissingFormatWidthException extends IllegalFormatException {

    private final String specifier;

    public MissingFormatWidthException(String s) {
        this.specifier = s;
    }

    public String getFormatSpecifier() {
        return specifier;
    }

    public String getMessage() {
        return specifier;
    }
}
