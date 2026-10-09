package javax.accessibility;

import java.util.Locale;

/**
 * The base of the categories with a translatable name: roles, states and relations.
 *
 * <p>The trick of the class is that the constants are **objects**, not strings or integers. Each
 * role or state is a unique instance with a key inside, so they are compared by identity and can be
 * shown translated without the program ever touching the text.
 *
 * <p>That is also what lets an application invent its own categories: it inherits from here,
 * declares its constant, and the rest of the package treats it the same as the built-in ones.
 *
 * <p>Without a catalogue of translations, {@link #toDisplayString()} returns the key. It is what
 * the JDK does when it does not find the language's resource bundle, so it is not filler: it is the
 * default answer, and a readable key is better than an empty string.
 */
public abstract class AccessibleBundle {

    /** The key that identifies this category. */
    protected String key;

    /** For subclasses. */
    public AccessibleBundle() {
    }

    /**
     * The display name, looked up in that catalogue and that language.
     *
     * <p>It returns the key: this library ships no translation catalogues.
     */
    protected String toDisplayString(String resourceBundleName, Locale locale) {
        return this.key;
    }

    /** The display name in that language. */
    public String toDisplayString(Locale locale) {
        return this.toDisplayString("com.sun.accessibility.internal.resources.accessibility",
                locale);
    }

    /** The display name in the default language. */
    public String toDisplayString() {
        return this.toDisplayString(Locale.getDefault());
    }

    public String toString() {
        return this.toDisplayString();
    }
}
