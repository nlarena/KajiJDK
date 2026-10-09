package javax.management.remote;

import javax.security.auth.Subject;

/**
 * KajiLibrary's javax.management.remote.JMXAuthenticator -- decides whether a client gets in.
 *
 * <p>It is given to the server in the environment map under the
 * {@link JMXConnectorServer#AUTHENTICATOR} key, and it is called once per connection.
 *
 * <p>The argument is {@link Object} and not something more precise because it depends on the
 * protocol: the RMI connector passes a two-element {@code String[]} --user and password--,
 * another protocol could pass a certificate. An authenticator has to check the type before using
 * it.
 *
 * <p>It returns the {@link Subject} that client's operations will run as. Rejecting is done by
 * throwing {@link SecurityException}: returning null means "no identity", which is not the same
 * as "does not get in".
 */
public interface JMXAuthenticator {

    /**
     * @param credentials what the client sent; its type depends on the protocol
     * @return what identity it runs as, or null for none
     * @throws SecurityException if it does not get in
     */
    Subject authenticate(Object credentials);
}
