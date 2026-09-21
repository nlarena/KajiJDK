package javax.security.auth.spi;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

/**
 * KajiLibrary's javax.security.auth.spi.LoginModule -- a pluggable authentication mechanism.
 *
 * <h2>The two phases, which are the only hard thing about this interface</h2>
 *
 * <p>Authenticating is not one method but <b>two</b>: {@link #login()} and {@link #commit()}. The
 * reason is that a configuration can stack several modules --password, certificate, second factor--
 * and require all of them to pass. If the first wrote the principals into the Subject as soon as it
 * finished, and the third failed, the Subject would be left with half an identity: authenticated by
 * one and rejected by another.
 *
 * <p>That is why {@code login()} only verifies and keeps the result <b>inside the module</b>, and
 * only {@code commit()} --which runs when <b>all</b> passed-- writes it into the Subject. If any
 * failed, {@link #abort()} is called and each module throws away its own.
 *
 * <h2>What each method returns</h2>
 *
 * <p>{@code true} means "this module did something", {@code false} means "it was not my turn". A
 * module that does not apply --for example a smart card one on a machine without a reader-- returns
 * false instead of throwing, and the configuration goes on with the next one.
 *
 * <p><b>This library comes with no module</b>: the interface is there so that one that gets written
 * fits, just as {@code X509Certificate} is there without there being any certificate parser.
 */
public interface LoginModule {

    /**
     * Gives the module what it needs before starting.
     *
     * @param subject         where the identities are going to be written, in {@link #commit()}
     * @param callbackHandler how the user is asked. See
     *     {@link javax.security.auth.callback.Callback} for why the module does not ask on its own
     * @param sharedState     what the modules of the same stack pass each other -- typically the
     *     password, so that the second does not ask the user for it again
     * @param options         this module's configuration in this stack
     */
    void initialize(Subject subject, CallbackHandler callbackHandler,
        java.util.Map<String, ?> sharedState, java.util.Map<String, ?> options);

    /**
     * Verifies. It does <b>not</b> write to the Subject; see the class note.
     *
     * @return whether this module did something
     * @throws LoginException if the authentication failed
     */
    boolean login() throws LoginException;

    /**
     * Writes into the Subject what {@link #login()} verified. It runs only if the <b>whole</b>
     * stack passed.
     *
     * @return whether this module did something
     * @throws LoginException if it could not be written
     */
    boolean commit() throws LoginException;

    /**
     * Throws away what {@link #login()} had verified. It runs when some other module of the stack
     * failed.
     *
     * @return whether this module did something
     * @throws LoginException if it could not be undone
     */
    boolean abort() throws LoginException;

    /**
     * Removes from the Subject what this module put there.
     *
     * @return whether this module did something
     * @throws LoginException if it could not
     */
    boolean logout() throws LoginException;
}
