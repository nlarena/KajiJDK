package java.util;

// The argument is of a type the conversion cannot format.
public class IllegalFormatConversionException extends IllegalFormatException {

    private final char conversion;
    private final Class<?> argumentClass;

    public IllegalFormatConversionException(char c, Class<?> arg) {
        this.conversion = c;
        this.argumentClass = arg;
    }

    public char getConversion() {
        return conversion;
    }

    public Class<?> getArgumentClass() {
        return argumentClass;
    }

    public String getMessage() {
        return conversion + " != " + argumentClass.getName();
    }
}
