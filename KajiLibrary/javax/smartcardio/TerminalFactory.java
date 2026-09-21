package javax.smartcardio;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.smartcardio.TerminalFactory -- where the readers come from.
 *
 * <p>{@link #getDefault} gives the system's; {@link #terminals} gives the readers. The whole
 * package starts here.
 *
 * <h2>The {@code "None"} type</h2>
 *
 * <p>The default type is {@code "PC/SC"} when the system's card library is available. When it is
 * not, the type is {@code "None"} and {@link #terminals} returns an <b>empty</b> list instead of
 * failing.
 *
 * <p>It is on purpose: a program that queries the readers on a machine with no reader has to find
 * out that there is none, not receive an installation error. What does fail is {@link
 * CardTerminals#waitForChange}, with {@link IllegalStateException}, because waiting for a change in
 * a list that is never going to change would be hanging forever.
 *
 * <p>KajiJDK does not talk to the system library, so the default type is always {@code "None"}. A
 * provider registered with {@link #getInstance} works just as in the JDK.
 */
public final class TerminalFactory {

    /** The type used when there is no card library. */
    private static final String NONE_TYPE = "None";

    /** The default one, built the first time it is asked for. */
    private static TerminalFactory defaultFactory = null;

    /** What gives the readers. */
    private final TerminalFactorySpi spi;

    /** Which provider it came from. */
    private final Provider provider;

    /** What the type is called. */
    private final String type;

    /** It is reached through {@link #getDefault} or {@link #getInstance}. */
    private TerminalFactory(TerminalFactorySpi spi, Provider provider, String type) {
        this.spi = spi;
        this.provider = provider;
        this.type = type;
    }

    /**
     * The default type.
     *
     * <p>It comes from the {@code javax.smartcardio.TerminalFactory.DefaultType} property if it is
     * set. See the class note.
     */
    public static String getDefaultType() {
        String configured = System.getProperty("javax.smartcardio.TerminalFactory.DefaultType");
        if (configured != null && configured.length() > 0) {
            return configured;
        }
        return NONE_TYPE;
    }

    /** The default factory. Always the same one. */
    public static synchronized TerminalFactory getDefault() {
        if (defaultFactory == null) {
            String type = getDefaultType();
            if (!NONE_TYPE.equals(type)) {
                try {
                    defaultFactory = getInstance(type, null);
                    return defaultFactory;
                } catch (NoSuchAlgorithmException e) {
                    // The configured type does not exist: it falls back to the one that is always
                    // there.
                }
            }
            defaultFactory =
                new TerminalFactory(new NoneFactorySpi(), new NoneProvider(), NONE_TYPE);
        }
        return defaultFactory;
    }

    /**
     * The one of that type, from the first provider that offers it.
     *
     * @param params whatever the implementation needs to configure itself, or null
     * @throws NullPointerException if the type is null
     * @throws NoSuchAlgorithmException if no provider offers that type
     */
    public static TerminalFactory getInstance(String type, Object params)
            throws NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        Provider[] providers = Security.getProviders();
        int i = 0;
        while (i < providers.length) {
            Provider.Service service = providers[i].getService("TerminalFactory", type);
            if (service != null) {
                return build(service, providers[i], type, params);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(type + " TerminalFactory not available");
    }

    /**
     * The one of that type, from that provider by name.
     *
     * @throws NullPointerException if the type is null
     * @throws IllegalArgumentException if the provider name is empty
     * @throws NoSuchAlgorithmException if the provider does not offer that type
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static TerminalFactory getInstance(String type, Object params, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("provider must not be null or empty");
        }
        Provider found = Security.getProvider(provider);
        if (found == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, params, found);
    }

    /**
     * The one of that type, from that provider.
     *
     * @throws NullPointerException if the type or the provider is null
     * @throws NoSuchAlgorithmException if the provider does not offer that type
     */
    public static TerminalFactory getInstance(String type, Object params, Provider provider)
            throws NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        if (provider == null) {
            throw new NullPointerException("provider == null");
        }
        Provider.Service service = provider.getService("TerminalFactory", type);
        if (service == null) {
            throw new NoSuchAlgorithmException("no such algorithm: " + type + " for provider "
                + provider.getName());
        }
        return build(service, provider, type, params);
    }

    /** Builds the factory from the provider's service. */
    private static TerminalFactory build(Provider.Service service, Provider provider, String type,
                                         Object params) throws NoSuchAlgorithmException {
        Object instance = service.newInstance(params);
        if (!(instance instanceof TerminalFactorySpi)) {
            throw new NoSuchAlgorithmException("not a TerminalFactorySpi: " + type);
        }
        return new TerminalFactory((TerminalFactorySpi) instance, provider, type);
    }

    /** Which provider it came from. */
    public Provider getProvider() {
        return this.provider;
    }

    /** What the type is called. */
    public String getType() {
        return this.type;
    }

    /** The readers. See the class note. */
    public CardTerminals terminals() {
        return this.spi.engineTerminals();
    }

    /** The type and the provider. */
    @Override
    public String toString() {
        return "TerminalFactory for type " + this.type + " from provider " + this.provider.getName();
    }

    /** The make-believe provider of the {@code "None"} type. See the class note. */
    private static final class NoneProvider extends Provider {

        private static final long serialVersionUID = 2745808869881593918L;

        NoneProvider() {
            super(NONE_TYPE, "1.0", "none");
        }
    }

    /** The implementation of the {@code "None"} type: always the same zero readers. */
    private static final class NoneFactorySpi extends TerminalFactorySpi {

        /** There is no state, so one is enough. */
        private final CardTerminals terminals = new NoTerminals();

        @Override
        protected CardTerminals engineTerminals() {
            return this.terminals;
        }
    }

    /** No reader, and waiting for a change is an error. See the class note. */
    private static final class NoTerminals extends CardTerminals {

        @Override
        public List<CardTerminal> list(State state) throws CardException {
            if (state == null) {
                throw new NullPointerException();
            }
            return Collections.unmodifiableList(new ArrayList<CardTerminal>());
        }

        @Override
        public boolean waitForChange(long timeout) throws CardException {
            // With no readers there is nothing that can change: waiting would be hanging forever.
            throw new IllegalStateException("no terminals");
        }
    }
}
