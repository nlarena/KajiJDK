package javax.security.sasl;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * KajiLibrary's javax.security.sasl.SaslClientFactory -- where the {@link SaslClient}s come from.
 *
 * <p>It is registered as a service of a security provider, with the type {@code
 * "SaslClientFactory"} and the mechanism's name as the algorithm. One factory can handle several
 * mechanisms, and that is why {@link #getMechanismNames} returns an array.
 *
 * <p>That method receives the properties, and there lies the interesting detail: the list of
 * mechanisms a factory offers <b>depends on the policy</b>. With {@link Sasl#POLICY_NOPLAINTEXT}
 * set, a factory that knows PLAIN must not list it. It is what lets the choice of mechanism respect
 * the policy without the caller having to know them one by one.
 *
 * <p>{@link #createSaslClient} can return null: it means that, with those properties and that
 * handler, this factory cannot handle any of the requested mechanisms. It is not an error, and that
 * is why {@link Sasl#createSaslClient} goes on with the next one.
 */
public interface SaslClientFactory {

    /**
     * A client for the first of those mechanisms this factory can handle.
     *
     * @param mechanisms the acceptable mechanisms, in order of preference
     * @param authorizationId on whose behalf to act, or null for the authenticated one
     * @param protocol the protocol above, for example {@code "ldap"}
     * @param serverName the server's name
     * @param props the configuration; see the constants of {@link Sasl}
     * @return null if it cannot handle any
     */
    SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol,
                                String serverName, Map<String, ?> props, CallbackHandler cbh)
        throws SaslException;

    /** The mechanisms it offers with those properties. See the class note. */
    String[] getMechanismNames(Map<String, ?> props);
}
