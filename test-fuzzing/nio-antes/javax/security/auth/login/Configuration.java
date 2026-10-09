package javax.security.auth.login;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * KajiLibrary's javax.security.auth.login.Configuration -- which modules each application uses.
 *
 * <p>It translates a name --the one the application passes to {@link LoginContext}-- to the list of
 * {@link AppConfigurationEntry} it is going to authenticate with. All of JAAS's point is in that
 * indirection: the program says "authenticate me as 'MyApp'" and whoever deploys decides whether
 * that is a local password, Kerberos or both.
 *
 * <h2>Only one per process</h2>
 *
 * <p>{@link #getConfiguration} and {@link #setConfiguration} are static, so the configuration is
 * global. That is exactly what is wanted here --that a library cannot change the authentication
 * rules of the rest of the program on its own-- and that is why whoever changes it needs
 * permission.
 *
 * <h2>The three {@code getInstance}s</h2>
 *
 * <p>They are the alternative route: instead of the global configuration, one built by a provider
 * from {@link Parameters}. It serves to set up a configuration of one's own without overwriting
 * anybody else's.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library <b>does not come with a configuration file reader</b>. What is missing is the
 * parser, not the API: while nobody installs one, {@link #getConfiguration} returns an empty
 * configuration and a {@link LoginContext} on any name fails with "No LoginModules configured",
 * which is exactly what the JDK does when it does not find the file. Installing one's own with
 * {@link #setConfiguration} --or passing it to the context's constructor-- works as always.
 *
 * <p>The three {@code getInstance}s throw {@link NoSuchAlgorithmException} because there is no
 * provider that registers a {@code Configuration} service. It is the method's declared way out.
 */
public abstract class Configuration {

    /** The installed one, or null until somebody asks. */
    private static Configuration installed;

    /** Where it came from, when obtained with {@link #getInstance}; null otherwise. */
    private Provider provider;

    /** The type it was asked for with, or null. */
    private String type;

    /** The parameters it was asked for with, or null. */
    private Parameters parameters;

    /** For the subclasses. */
    protected Configuration() {
    }

    /**
     * The process's configuration.
     *
     * <p>If nobody installed one, it returns an empty one; see the class note.
     */
    public static synchronized Configuration getConfiguration() {
        if (installed == null) {
            installed = new EmptyConfiguration();
        }
        return installed;
    }

    /**
     * Changes the process's configuration.
     *
     * @param configuration the new one; null goes back to the empty one
     */
    public static synchronized void setConfiguration(Configuration configuration) {
        installed = configuration;
    }

    /**
     * A configuration built by the first provider that knows how to build it.
     *
     * @throws NoSuchAlgorithmException if none knows how; always in KajiLibrary
     */
    public static Configuration getInstance(String type, Parameters params)
        throws NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("invalid null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("Configuration", type);
            if (s != null) {
                return build(s, type, params);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(type + " Configuration not available");
    }

    /**
     * Likewise, from a named provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static Configuration getInstance(String type, Parameters params, String provider)
        throws NoSuchProviderException, NoSuchAlgorithmException {
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, params, p);
    }

    /** Likewise, from a provider already at hand. */
    public static Configuration getInstance(String type, Parameters params, Provider provider)
        throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("invalid null type name");
        }
        Provider.Service s = provider.getService("Configuration", type);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return build(s, type, params);
    }

    /** What the three {@code getInstance}s build in common. */
    private static Configuration build(Provider.Service s, String type, Parameters params)
        throws NoSuchAlgorithmException {
        Object o = s.newInstance(params);
        if (!(o instanceof ConfigurationSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for Configuration is not a ConfigurationSpi: " + s.getClassName());
        }
        ConfigurationDelegate made = new ConfigurationDelegate((ConfigurationSpi) o);
        made.provider = s.getProvider();
        made.type = type;
        made.parameters = params;
        return made;
    }

    /** The provider that built it, or null if it did not come from {@link #getInstance}. */
    public Provider getProvider() {
        return this.provider;
    }

    /** The type it was asked for with, or null. */
    public String getType() {
        return this.type;
    }

    /** The parameters it was asked for with, or null. */
    public Parameters getParameters() {
        return this.parameters;
    }

    /**
     * The modules configured for that name.
     *
     * @return null if that name has nothing configured, which is <b>not</b> an error
     */
    public abstract AppConfigurationEntry[] getAppConfigurationEntry(String name);

    /**
     * Reads the configuration again.
     *
     * <p>By default it does nothing: a configuration built in memory has nowhere to reread from.
     */
    public void refresh() {
    }

    /**
     * What is passed to a provider so that it builds a configuration.
     *
     * <p>A marker without methods: each type of configuration defines its own. It exists only so
     * that the signature of {@link Configuration#getInstance} says something more useful than
     * {@code Object}.
     */
    public interface Parameters {
    }

    /** The one used when nobody installed any. See the class note. */
    private static final class EmptyConfiguration extends Configuration {

        /** Always null: there is nothing configured for any name. */
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return null;
        }
    }

    /** The one the {@code getInstance}s return: it hands everything to the provider's SPI. */
    private static final class ConfigurationDelegate extends Configuration {

        private final ConfigurationSpi spi;

        ConfigurationDelegate(ConfigurationSpi spi) {
            this.spi = spi;
        }

        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return this.spi.engineGetAppConfigurationEntry(name);
        }

        public void refresh() {
            this.spi.engineRefresh();
        }
    }
}
