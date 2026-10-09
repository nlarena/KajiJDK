package javax.print.attribute;

import java.io.Serializable;
import java.util.Locale;

// The syntax class of the attributes whose value is text with an associated locale.
//
// The locale matters: the same text in a different language is two different values, and it goes
// into `equals` and `hashCode`. A null locale is replaced with the machine's; a null text is an
// error.
public abstract class TextSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -8130648736378144102L;

    private String value;
    private Locale locale;

    protected TextSyntax(String value, Locale locale) {
        this.value = verify(value);
        this.locale = verify(locale);
    }

    private static String verify(String value) {
        if (value == null) {
            throw new NullPointerException(" value is null");
        }
        return value;
    }

    // A null locale is not an error: it means "the local one".
    private static Locale verify(Locale locale) {
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }

    public String getValue() {
        return this.value;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public int hashCode() {
        return this.value.hashCode() ^ this.locale.hashCode();
    }

    public boolean equals(Object object) {
        if (!(object instanceof TextSyntax)) {
            return false;
        }
        TextSyntax other = (TextSyntax) object;
        return this.value.equals(other.value) && this.locale.equals(other.locale);
    }

    // Only the text: the locale is not shown.
    public String toString() {
        return this.value;
    }
}
