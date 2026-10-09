package javax.security.auth.login;

import java.security.GeneralSecurityException;

/**
 * KajiLibrary's javax.security.auth.login.LoginException -- the authentication failed.
 *
 * <p>It is the root of a hierarchy that is deliberately <b>flat in what it reports</b>: its
 * subclasses tell the account from the credential --{@code AccountExpiredException} against {@code
 * CredentialExpiredException}-- but none says "that account does not exist" when the module chose
 * not to say it.
 *
 * <p>That is not a shortcoming of the API: a service that answers "wrong user" and "wrong password"
 * separately hands whoever is probing a list of valid users. That is why a prudent login module
 * throws a plain {@code FailedLoginException} in both cases, and the API lets it be as precise or
 * as reserved as it wants.
 *
 * <p>It extends {@code GeneralSecurityException}, which is the root of {@code java.security}: an
 * authentication failure is a security failure and can be caught together with the others.
 */
public class LoginException extends GeneralSecurityException {

    private static final long serialVersionUID = -4679091624035232488L;

    public LoginException() {
        super();
    }

    public LoginException(String msg) {
        super(msg);
    }
}
