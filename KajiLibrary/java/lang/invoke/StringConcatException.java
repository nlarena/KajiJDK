package java.lang.invoke;

// The string-concatenation factory could not build the call site — a malformed recipe, or a
// shape it cannot express. Same reasoning as `LambdaConversionException`: raised at link time.
public class StringConcatException extends Exception {

    public StringConcatException(String message) {
        super(message);
    }
}
