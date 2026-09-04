package javax.smartcardio;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.smartcardio.TerminalFactory -- de donde salen los lectores.
 *
 * <p>{@link #getDefault} da la del sistema; {@link #terminals} da los lectores. Todo el paquete
 * empieza por aca.
 *
 * <h2>El tipo {@code "None"}</h2>
 *
 * <p>El tipo por omision es {@code "PC/SC"} cuando la biblioteca de tarjetas del sistema esta
 * disponible. Cuando no, el tipo es {@code "None"} y {@link #terminals} devuelve una lista
 * <b>vacia</b> en vez de fallar.
 *
 * <p>Es a proposito: un programa que consulte los lectores en una maquina sin lector tiene que
 * enterarse de que no hay ninguno, no recibir un error de instalacion. Lo que si falla es
 * {@link CardTerminals#waitForChange}, con {@link IllegalStateException}, porque esperar un cambio en
 * una lista que nunca va a cambiar seria colgarse para siempre.
 *
 * <p>KajiJDK no habla con la biblioteca del sistema, asi que el tipo por omision es siempre
 * {@code "None"}. Un proveedor registrado con {@link #getInstance} funciona igual que en el JDK.
 */
public final class TerminalFactory {

    /** El tipo que se usa cuando no hay biblioteca de tarjetas. */
    private static final String NONE_TYPE = "None";

    /** La de por omision, armada la primera vez que se pide. */
    private static TerminalFactory defaultFactory = null;

    /** Quien da los lectores. */
    private final TerminalFactorySpi spi;

    /** De que proveedor salio. */
    private final Provider provider;

    /** Como se llama el tipo. */
    private final String type;

    /** Se llega por {@link #getDefault} o {@link #getInstance}. */
    private TerminalFactory(TerminalFactorySpi spi, Provider provider, String type) {
        this.spi = spi;
        this.provider = provider;
        this.type = type;
    }

    /**
     * El tipo por omision.
     *
     * <p>Sale de la propiedad {@code javax.smartcardio.TerminalFactory.DefaultType} si esta puesta.
     * Ver la nota de la clase.
     */
    public static String getDefaultType() {
        String configured = System.getProperty("javax.smartcardio.TerminalFactory.DefaultType");
        if (configured != null && configured.length() > 0) {
            return configured;
        }
        return NONE_TYPE;
    }

    /** La fabrica por omision. Siempre la misma. */
    public static synchronized TerminalFactory getDefault() {
        if (defaultFactory == null) {
            String type = getDefaultType();
            if (!NONE_TYPE.equals(type)) {
                try {
                    defaultFactory = getInstance(type, null);
                    return defaultFactory;
                } catch (NoSuchAlgorithmException e) {
                    // El tipo configurado no existe: se cae al que siempre esta.
                }
            }
            defaultFactory =
                new TerminalFactory(new NoneFactorySpi(), new NoneProvider(), NONE_TYPE);
        }
        return defaultFactory;
    }

    /**
     * La de ese tipo, del primer proveedor que lo ofrezca.
     *
     * @param params lo que la implementacion necesite para configurarse, o null
     * @throws NullPointerException si el tipo es null
     * @throws NoSuchAlgorithmException si ningun proveedor ofrece ese tipo
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
     * La de ese tipo, de ese proveedor por nombre.
     *
     * @throws NullPointerException si el tipo es null
     * @throws IllegalArgumentException si el nombre del proveedor esta vacio
     * @throws NoSuchAlgorithmException si el proveedor no ofrece ese tipo
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
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
     * La de ese tipo, de ese proveedor.
     *
     * @throws NullPointerException si el tipo o el proveedor son null
     * @throws NoSuchAlgorithmException si el proveedor no ofrece ese tipo
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

    /** Arma la fabrica desde el servicio del proveedor. */
    private static TerminalFactory build(Provider.Service service, Provider provider, String type,
                                         Object params) throws NoSuchAlgorithmException {
        Object instance = service.newInstance(params);
        if (!(instance instanceof TerminalFactorySpi)) {
            throw new NoSuchAlgorithmException("not a TerminalFactorySpi: " + type);
        }
        return new TerminalFactory((TerminalFactorySpi) instance, provider, type);
    }

    /** De que proveedor salio. */
    public Provider getProvider() {
        return this.provider;
    }

    /** Como se llama el tipo. */
    public String getType() {
        return this.type;
    }

    /** Los lectores. Ver la nota de la clase. */
    public CardTerminals terminals() {
        return this.spi.engineTerminals();
    }

    /** El tipo y el proveedor. */
    @Override
    public String toString() {
        return "TerminalFactory for type " + this.type + " from provider " + this.provider.getName();
    }

    /** El proveedor de mentira del tipo {@code "None"}. Ver la nota de la clase. */
    private static final class NoneProvider extends Provider {

        private static final long serialVersionUID = 2745808869881593918L;

        NoneProvider() {
            super(NONE_TYPE, "1.0", "none");
        }
    }

    /** La implementacion del tipo {@code "None"}: siempre los mismos cero lectores. */
    private static final class NoneFactorySpi extends TerminalFactorySpi {

        /** No hay estado, asi que alcanza con una. */
        private final CardTerminals terminals = new NoTerminals();

        @Override
        protected CardTerminals engineTerminals() {
            return this.terminals;
        }
    }

    /** Ningun lector, y esperar un cambio es un error. Ver la nota de la clase. */
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
            // Sin lectores no hay nada que pueda cambiar: esperar seria colgarse para siempre.
            throw new IllegalStateException("no terminals");
        }
    }
}
