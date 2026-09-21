package javax.accessibility;

import java.util.ListResourceBundle;

/**
 * The catalogue of translatable names of roles, states and relations.
 *
 * <p>It is **empty**, and that is right here: this library ships no translations, so
 * {@link AccessibleBundle#toDisplayString()} returns the key. A catalogue with the English keys
 * mapped to themselves would be the same result with more ceremony and with the false appearance of
 * being translated.
 *
 * @deprecated the JDK stopped using it; translations are looked up another way. It is kept because
 *     it is in the public API.
 */
@Deprecated
public class AccessibleResourceBundle extends ListResourceBundle {

    /** An empty catalogue. */
    public AccessibleResourceBundle() {
    }

    /** No entries. */
    public Object[][] getContents() {
        return new Object[0][];
    }
}
