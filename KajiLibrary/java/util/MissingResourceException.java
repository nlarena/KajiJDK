package java.util;

// Thrown when a resource bundle, or a key inside one, cannot be found.
//
// It carries two things the message alone would bury: the class name that was looked for and the
// key that was missing. A caller that catches this usually wants to branch on one of them — fall
// back to a default string, or report which bundle is absent — and digging either back out of a
// formatted message would be guesswork.
public class MissingResourceException extends RuntimeException {

    // The name of the class or bundle that was being looked for.
    private String className;

    // The key that was missing, or "" when the whole bundle was the thing missing.
    private String key;

    // Signals that `key` was not found in `className`, with `s` as the human-readable detail.
    public MissingResourceException(String s, String className, String key) {
        super(s);
        this.className = className;
        this.key = key;
    }

    // The name of the class or bundle that was being looked for.
    public String getClassName() {
        return this.className;
    }

    // The key that was missing.
    public String getKey() {
        return this.key;
    }
}
