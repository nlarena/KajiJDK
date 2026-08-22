package java.util;

// The precision is negative, or the conversion does not take one.
public class IllegalFormatPrecisionException extends IllegalFormatException {

    private final int precision;

    public IllegalFormatPrecisionException(int p) {
        this.precision = p;
    }

    public int getPrecision() {
        return precision;
    }

    public String getMessage() {
        return Integer.toString(precision);
    }
}
