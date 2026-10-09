package java.security;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

/**
 * A {@link Provider} whose keys have to be unlocked before being used.
 *
 * <h2>Why a provider would need authentication</h2>
 *
 * <p>Because not all of them keep their keys in a file. A provider backed by a smart card, a USB
 * token or an HSM has the keys inside the device, and the device asks for a PIN before letting one
 * sign with them. That state —connected or not— does not exist in an ordinary {@link Provider},
 * which is supposed to be available from the moment it is registered.
 *
 * <p>Hence the three methods: {@link #login} opens the session, {@link #logout} closes it, and
 * {@link #setCallbackHandler} says <strong>how</strong> the PIN is asked of the user — because the
 * provider does not know whether there is a terminal, a window or a service on the other side. It
 * is the same mechanism of {@code javax.security.auth.callback} that JAAS uses, and that is why it
 * reuses it.
 *
 * <p>The whole class is declarative: whoever extends it is the concrete provider, which is the only
 * one that knows how to talk to its device.
 */
public abstract class AuthProvider extends Provider {

    private static final long serialVersionUID = 4197859053084546461L;

    /**
     * @deprecated use the constructor that takes the version as a {@link String}: a {@code double}
     *     cannot represent a three-part version, and {@code 1.10} is smaller than {@code 1.9}
     */
    @Deprecated(since = "9")
    protected AuthProvider(String name, double version, String info) {
        super(name, version, info);
    }

    /** With the name, the version and a description. */
    protected AuthProvider(String name, String versionStr, String info) {
        super(name, versionStr, info);
    }

    /**
     * Opens the session with the device.
     *
     * @param subject where to leave the principals that result, or {@code null}
     * @param handler how to ask the user for the credentials; {@code null} uses the one that was
     *     set with {@link #setCallbackHandler}
     * @throws LoginException if it could not be done
     */
    public abstract void login(Subject subject, CallbackHandler handler) throws LoginException;

    /**
     * Closes the session.
     *
     * <p>After this the keys of the device go back to being unavailable, which is the point: an
     * application that has finished signing should not leave the token open.
     */
    public abstract void logout() throws LoginException;

    /**
     * Sets how the credentials are asked of the user.
     *
     * <p>It can be called before {@link #login} so that the latter does not have to receive one.
     */
    public abstract void setCallbackHandler(CallbackHandler handler);
}
