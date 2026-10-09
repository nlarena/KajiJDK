package javax.security.sasl;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * KajiLibrary's javax.security.sasl.SaslServerFactory -- where the {@link SaslServer}s come from.
 *
 * <p>The mirror of {@link SaslClientFactory}, with a difference in the signature that says a lot:
 * here <b>one</b> mechanism is asked for and not a list. It makes sense -- the client already chose
 * and announced it, and the server only has to be able to handle it or not.
 *
 * <p>{@link #getMechanismNames} also depends on the policy, just as on the client side, and on the
 * server side that is even more important: the list it returns is the one <b>announced to the
 * client</b>, and announcing a weak mechanism is offering it to whoever wants to choose it.
 */
public interface SaslServerFactory {

    /**
     * A server for that mechanism.
     *
     * @return null if this factory cannot handle it
     */
    SaslServer createSaslServer(String mechanism, String protocol, String serverName,
                                Map<String, ?> props, CallbackHandler cbh) throws SaslException;

    /** The mechanisms it offers with those properties. See the class note. */
    String[] getMechanismNames(Map<String, ?> props);
}
