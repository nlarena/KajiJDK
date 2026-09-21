package javax.security.sasl;

import javax.security.auth.callback.TextInputCallback;

/**
 * KajiLibrary's javax.security.sasl.RealmCallback -- asking in which realm.
 *
 * <p>A {@link TextInputCallback} that adds nothing but its <b>type</b>. That is all it needs: a
 * handler receives a list of callbacks and decides what to do with each one by looking at its
 * class, so "ask for a realm" and "ask for any text" have to be different types to be answered
 * differently.
 *
 * <p>The realm is the namespace where the user is valid. The same name can be two different people
 * in two realms, and that is why the mechanism asks for it separately instead of expecting it to
 * come attached to the user.
 */
public class RealmCallback extends TextInputCallback {

    private static final long serialVersionUID = -4342673378785456908L;

    /** Without a suggestion. */
    public RealmCallback(String prompt) {
        super(prompt);
    }

    /**
     * With a suggestion.
     *
     * @param defaultRealmInfo the realm the server proposes
     */
    public RealmCallback(String prompt, String defaultRealmInfo) {
        super(prompt, defaultRealmInfo);
    }
}
