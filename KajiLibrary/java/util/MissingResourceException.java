package java.util;

// KajiLibrary's java.util.MissingResourceException -- thrown when a resource bundle or one of its
// keys cannot be found. KajiJDK ships no bundle files, so ResourceBundle.getBundle always raises
// this; it also carries the class name that was searched for and the missing key, as the JDK does.
public class MissingResourceException extends RuntimeException {

    private final String className;
    private final String key;

    public MissingResourceException(String s, String className, String key) {
        super(s);
        this.className = className;
        this.key = key;
    }

    /** The name of the class that was being looked for. */
    public String getClassName() {
        return this.className;
    }

    /** The key that was being looked for, or null. */
    public String getKey() {
        return this.key;
    }
}
