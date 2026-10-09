package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.ConfigurationSpi -- what a provider writes.
 *
 * <p>The lower half of the pair: {@link Configuration} is what the application sees and this is
 * what whoever provides implements. The separation serves so that the public methods --{@code
 * getProvider}, {@code getType}, {@code getParameters}-- cannot be implemented wrong: the upper
 * class answers them with what it knows about the request, and the provider cannot lie about where
 * it came from.
 */
public abstract class ConfigurationSpi {

    /** Public because of how it is instantiated by reflection from the provider. */
    public ConfigurationSpi() {
    }

    /**
     * The modules configured for that name.
     *
     * @return null if there is nothing for that name
     */
    protected abstract AppConfigurationEntry[] engineGetAppConfigurationEntry(String name);

    /**
     * Reads the configuration again.
     *
     * <p>By default it does nothing, so that a provider without a reloadable source does not have
     * to write an empty body.
     */
    protected void engineRefresh() {
    }
}
