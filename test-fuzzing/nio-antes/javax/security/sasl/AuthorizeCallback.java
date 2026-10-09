package javax.security.sasl;

import java.io.Serializable;
import javax.security.auth.callback.Callback;

/**
 * KajiLibrary's javax.security.sasl.AuthorizeCallback -- may this one act as that one.
 *
 * <p>The question a SASL server asks after authenticating, and it separates two things that get
 * confused all the time:
 *
 * <ul>
 *   <li>the <b>authentication identifier</b> is who proved to be: the person or process that
 *       presented the password;
 *   <li>the <b>authorization identifier</b> is on whose behalf it wants to act.
 * </ul>
 *
 * <p>Almost always they are the same and nothing happens. When they are not --an administration
 * process that wants to do something as another user, a proxy that forwards-- it is exactly the
 * point where it has to be decided whether that is allowed, and this class is where the handler
 * decides it.
 *
 * <h2>The answer has two parts</h2>
 *
 * <p>{@link #setAuthorized} says whether it is allowed, and {@link #setAuthorizedID} also lets the
 * identifier be <b>rewritten</b>. Rewriting it is useful because the name that arrives through the
 * protocol is almost never the one the system uses inside: {@code "juan@example.com"} arrives and
 * inside the work is done with {@code "uid=juan,ou=people"}.
 *
 * <p>{@link #getAuthorizedID} returns null while it has not been authorized, and the authorization
 * one if it was authorized without rewriting. Going back to null on unauthorizing is the right
 * thing: an authorized identifier that survives the refusal is just the one somebody is going to
 * read without looking at the flag.
 */
public class AuthorizeCallback implements Callback, Serializable {

    private static final long serialVersionUID = -2353344186490470805L;

    /** Who proved to be. */
    private final String authenticationID;

    /** On whose behalf it wants to act. */
    private final String authorizationID;

    /** What the handler answered. */
    private boolean authorized = false;

    /** The rewritten identifier, or null to use the authorization one. */
    private String authorizedID = null;

    /**
     * @param authnID who proved to be
     * @param authzID on whose behalf it wants to act
     */
    public AuthorizeCallback(String authnID, String authzID) {
        this.authenticationID = authnID;
        this.authorizationID = authzID;
    }

    /** Who proved to be. */
    public String getAuthenticationID() {
        return this.authenticationID;
    }

    /** On whose behalf it wants to act. */
    public String getAuthorizationID() {
        return this.authorizationID;
    }

    /** What the handler answered. */
    public boolean isAuthorized() {
        return this.authorized;
    }

    /** Ver {@link #isAuthorized}. */
    public void setAuthorized(boolean ok) {
        this.authorized = ok;
    }

    /**
     * The identifier to use.
     *
     * @return null if it was not authorized; see the class note
     */
    public String getAuthorizedID() {
        if (!this.authorized) {
            return null;
        }
        return (this.authorizedID == null) ? this.authorizationID : this.authorizedID;
    }

    /** Rewrites the identifier. See the class note on why it is needed. */
    public void setAuthorizedID(String id) {
        this.authorizedID = id;
    }
}
