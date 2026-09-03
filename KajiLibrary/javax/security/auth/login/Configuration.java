package javax.security.auth.login;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * KajiLibrary's javax.security.auth.login.Configuration -- que modulos usa cada aplicacion.
 *
 * <p>Traduce un nombre --el que la aplicacion le pasa a {@link LoginContext}-- a la lista de
 * {@link AppConfigurationEntry} con la que se va a autenticar. Toda la gracia de JAAS esta en esa
 * indireccion: el programa dice "autenticame como 'MiApp'" y quien despliega decide si eso es una
 * contrasena local, un Kerberos o los dos.
 *
 * <h2>Una sola por proceso</h2>
 *
 * <p>{@link #getConfiguration} y {@link #setConfiguration} son estaticos, asi que la configuracion
 * es global. Eso es exactamente lo que se quiere aca --que una biblioteca no pueda cambiarle las
 * reglas de autenticacion al resto del programa por su cuenta-- y por eso el que la cambia necesita
 * permiso.
 *
 * <h2>Los tres {@code getInstance}</h2>
 *
 * <p>Son la via alternativa: en vez de la configuracion global, una construida por un proveedor a
 * partir de {@link Parameters}. Sirve para armarse una configuracion propia sin pisarle la de nadie.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca <b>no trae un lector de archivos de configuracion</b>. Falta el parser, no el
 * API: mientras nadie instale una, {@link #getConfiguration} devuelve una configuracion vacia y un
 * {@link LoginContext} sobre cualquier nombre falla con "No LoginModules configured", que es
 * exactamente lo que hace el JDK cuando no encuentra el archivo. Instalar la propia con
 * {@link #setConfiguration} --o pasarla al constructor del contexto-- anda igual que siempre.
 *
 * <p>Los tres {@code getInstance} lanzan {@link NoSuchAlgorithmException} porque no hay ningun
 * proveedor que registre un servicio {@code Configuration}. Es la salida declarada del metodo.
 */
public abstract class Configuration {

    /** La instalada, o null hasta que alguien pregunte. */
    private static Configuration installed;

    /** De donde salio, cuando se obtuvo con {@link #getInstance}; null si no. */
    private Provider provider;

    /** El tipo con el que se pidio, o null. */
    private String type;

    /** Los parametros con los que se pidio, o null. */
    private Parameters parameters;

    /** Para las subclases. */
    protected Configuration() {
    }

    /**
     * La configuracion del proceso.
     *
     * <p>Si nadie instalo una, devuelve una vacia; ver la nota de la clase.
     */
    public static synchronized Configuration getConfiguration() {
        if (installed == null) {
            installed = new EmptyConfiguration();
        }
        return installed;
    }

    /**
     * Cambia la configuracion del proceso.
     *
     * @param configuration la nueva; null vuelve a la vacia
     */
    public static synchronized void setConfiguration(Configuration configuration) {
        installed = configuration;
    }

    /**
     * Una configuracion armada por el primer proveedor que sepa hacerla.
     *
     * @throws NoSuchAlgorithmException si ninguno la sabe hacer; siempre en KajiLibrary
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
     * Idem, de un proveedor con nombre.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
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

    /** Idem, de un proveedor ya en la mano. */
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

    /** El armado comun de los tres {@code getInstance}. */
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

    /** El proveedor que la armo, o null si no salio de {@link #getInstance}. */
    public Provider getProvider() {
        return this.provider;
    }

    /** El tipo con el que se pidio, o null. */
    public String getType() {
        return this.type;
    }

    /** Los parametros con los que se pidio, o null. */
    public Parameters getParameters() {
        return this.parameters;
    }

    /**
     * Los modulos configurados para ese nombre.
     *
     * @return null si ese nombre no tiene nada configurado, que <b>no</b> es un error
     */
    public abstract AppConfigurationEntry[] getAppConfigurationEntry(String name);

    /**
     * Vuelve a leer la configuracion.
     *
     * <p>Por omision no hace nada: una configuracion armada en memoria no tiene de donde releer.
     */
    public void refresh() {
    }

    /**
     * Lo que se le pasa a un proveedor para que arme una configuracion.
     *
     * <p>Marcadora y sin metodos: cada tipo de configuracion define los suyos. Existe solo para que
     * la firma de {@link Configuration#getInstance} diga algo mas util que {@code Object}.
     */
    public interface Parameters {
    }

    /** La que se usa cuando nadie instalo ninguna. Ver la nota de la clase. */
    private static final class EmptyConfiguration extends Configuration {

        /** Siempre null: no hay nada configurado para ningun nombre. */
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return null;
        }
    }

    /** La que devuelven los {@code getInstance}: le pasa todo al SPI del proveedor. */
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
