package java.lang.invoke;

// The string-concatenation factory could not build the call site — a malformed recipe, or a
// shape it cannot express. Same reasoning as `LambdaConversionException`: raised at link time.
public class StringConcatException extends Exception {

    public StringConcatException(String message) {
        super(message);
    }

    // The cause-carrying form. Unlike `LambdaConversionException` there is no no-arg constructor
    // and no cause-only one: the JDK gives this class exactly these two, because a concatenation
    // failure always has something to say about the recipe that failed.
    public StringConcatException(String message, Throwable cause) {
        super(message, cause);
    }
}
