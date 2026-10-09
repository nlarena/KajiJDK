package java.util.zip;

// A compressed stream that does not decode. Note it is NOT an `IOException`: the bytes arrived
// fine, it is their content that is wrong, and the JDK keeps that distinction by extending
// `Exception` directly.
public class DataFormatException extends Exception {

    public DataFormatException() {
        super();
    }

    public DataFormatException(String message) {
        super(message);
    }
}
