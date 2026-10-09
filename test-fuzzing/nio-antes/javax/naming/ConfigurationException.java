package javax.naming;

/**
 * Thrown when the client's or the provider's configuration is wrong: a property with an impossible
 * value, a factory class that cannot be loaded, an unusable `PROVIDER_URL`. Retrying never helps;
 * the configuration has to change.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class ConfigurationException extends NamingException {

    private static final long serialVersionUID = -2535156726228855704L;

    public ConfigurationException(String explanation) {
        super(explanation);
    }

    public ConfigurationException() {
        super();
    }
}
