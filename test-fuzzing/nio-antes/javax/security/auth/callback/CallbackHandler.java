package javax.security.auth.callback;

import java.io.IOException;

/**
 * KajiLibrary's javax.security.auth.callback.CallbackHandler -- whoever answers the requests.
 *
 * <p>The <b>application</b> writes it, not the login module: it is the half that knows whether
 * there is a terminal, a window or a configuration file. See {@link Callback} for why the division
 * is where it is.
 *
 * <h2>The two ways of saying no</h2>
 *
 * <p>The only method declares two exceptions and the difference between them decides what the
 * module that called it does:
 *
 * <ul>
 *   <li>{@link UnsupportedCallbackException} -- "I do not know how to answer <b>this</b> callback".
 *       The module may try another one, or go on without that datum.
 *   <li>{@link IOException} -- the medium failed. There is nothing to retry.
 * </ul>
 */
public interface CallbackHandler {

    /**
     * Answers each callback of the array, writing the answer <b>into the callback itself</b>.
     *
     * <p>It returns nothing: each one has its own setter, and that is where whoever asked is going
     * to look.
     *
     * @throws IOException if the input or output medium failed
     * @throws UnsupportedCallbackException if one of the callbacks cannot be answered
     */
    void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException;
}
