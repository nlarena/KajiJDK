package java.util;

// The conversion character is not one this formatter knows.
public class UnknownFormatConversionException extends IllegalFormatException {

    private final String conversion;

    public UnknownFormatConversionException(String s) {
        this.conversion = s;
    }

    public String getConversion() {
        return conversion;
    }

    public String getMessage() {
        return "Conversion = '" + conversion + "'";
    }
}
