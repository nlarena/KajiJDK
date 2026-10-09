package javax.security.auth.callback;

import java.util.Locale;

/**
 * KajiLibrary's javax.security.auth.callback.LanguageCallback -- which language to speak to the
 * user in.
 *
 * <p>It is the only callback <b>without a prompt</b>, and that makes sense: asking which language
 * to ask in would be circular. The application answers it with what it already knows -- the
 * system's configuration, an HTTP request's header, the user's saved preference.
 */
public class LanguageCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 2019050433478903213L;

    private Locale locale;

    public LanguageCallback() {
    }

    /** Sets the language. Whoever answers calls it. */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** The language, or null if nobody answered yet. */
    public Locale getLocale() {
        return this.locale;
    }
}
