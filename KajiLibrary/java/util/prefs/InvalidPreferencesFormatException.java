package java.util.prefs;

// The document handed to {@link Preferences#importPreferences} is not a valid preferences tree.
//
// It is distinct from an `IOException`: here the bytes arrived fine and what is wrong is what they
// say. That is why the cause is usually an error from the XML parser and not from the stream.
public class InvalidPreferencesFormatException extends Exception {

    private static final long serialVersionUID = -791715184232119669L;

    // An invalid document, with the cause that detected it.
    public InvalidPreferencesFormatException(Throwable cause) {
        super(cause);
    }

    // An invalid document, described by `message`.
    public InvalidPreferencesFormatException(String message) {
        super(message);
    }

    // An invalid document, with a message and a cause.
    public InvalidPreferencesFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
