package javax.annotation.processing;

// The factory of {@link Completion} (JSR 269). It is the whole class: two static `of` methods and a
// private constructor so that nobody instantiates it. The concrete implementation goes in a private
// nested class, just as in the real JDK: `Completion` exposes no constructor, and this is the only
// road to one.
public class Completions {

    // A utility class: it is not instantiated.
    private Completions() {
    }

    /**
     * A completion with a value and a message.
     *
     * @param value the text to insert
     * @param message the explanation that goes with it
     */
    public static Completion of(String value, String message) {
        return new SimpleCompletion(value, message);
    }

    /**
     * A completion with no message: the message is left as the empty string, not `null`.
     *
     * <p>It is what the real JDK does, and it is the difference that matters: a `null` would force
     * every consumer to check, when "I have nothing to explain" is already said by "".
     */
    public static Completion of(String value) {
        return new SimpleCompletion(value, "");
    }

    // The only implementor. Immutable and unvalidated: the contract does not forbid a null value, and
    // inventing an exception the JDK does not throw would be lying about the behaviour.
    private static class SimpleCompletion implements Completion {

        private final String value;
        private final String message;

        SimpleCompletion(String value, String message) {
            this.value = value;
            this.message = message;
        }

        public String getValue() {
            return this.value;
        }

        public String getMessage() {
            return this.message;
        }

        public String toString() {
            return "[\"" + this.value + "\", \"" + this.message + "\"]";
        }
    }
}
