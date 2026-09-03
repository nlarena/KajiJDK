package javax.security.sasl;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.CallbackHandler;

/**
 * KajiLibrary's javax.security.sasl.Sasl -- por donde se entra a SASL.
 *
 * <p>Metodos estaticos y una lista larga de constantes. Las constantes son los nombres de las
 * propiedades que se le pasan a la negociacion, y se dividen en dos grupos que conviene no mezclar:
 *
 * <ul>
 *   <li>las de <b>configuracion</b> --{@link #QOP}, {@link #STRENGTH}, {@link #MAX_BUFFER}-- dicen
 *       que se quiere;
 *   <li>las de <b>politica</b> --las {@code POLICY_*}-- dicen que <b>no</b> se acepta, y actuan
 *       antes: filtran que mecanismos se ofrecen siquiera.
 * </ul>
 *
 * <p>La distincion importa porque las de politica son las que de verdad protegen.
 * {@link #POLICY_NOPLAINTEXT} saca de la lista a los mecanismos que mandan la contrasena en claro, y
 * eso es mas fuerte que pedir cifrado: un mecanismo que no esta en la lista no se puede elegir ni
 * por error ni porque el servidor lo empuje.
 *
 * <h2>Devolver null no es fallar</h2>
 *
 * <p>{@link #createSaslClient} y {@link #createSaslServer} devuelven <b>null</b> cuando ninguna
 * fabrica registrada puede con lo que se pidio. Es lo correcto y hay que atajarlo: significa "no hay
 * un mecanismo en comun", que es una respuesta normal de una negociacion, no una excepcion.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no registra ninguna fabrica SASL, asi que los dos {@code create} devuelven null
 * y las dos enumeraciones vienen vacias. La busqueda esta implementada de verdad --recorre los
 * proveedores de seguridad buscando servicios {@code SaslClientFactory} y
 * {@code SaslServerFactory}-- asi que registrar una fabrica alcanza para que todo funcione.
 */
public class Sasl {

    /** La calidad de proteccion pedida: {@code auth}, {@code auth-int} o {@code auth-conf}. */
    public static final String QOP = "javax.security.sasl.qop";

    /** La fuerza del cifrado: {@code low}, {@code medium} o {@code high}. */
    public static final String STRENGTH = "javax.security.sasl.strength";

    /** Si el servidor tambien tiene que autenticarse. */
    public static final String SERVER_AUTH = "javax.security.sasl.server.authentication";

    /** El nombre del servidor al que el canal esta atado. */
    public static final String BOUND_SERVER_NAME = "javax.security.sasl.bound.server.name";

    /** El tamano maximo de un bloque recibido. */
    public static final String MAX_BUFFER = "javax.security.sasl.maxbuffer";

    /** El tamano maximo de un bloque enviado. */
    public static final String RAW_SEND_SIZE = "javax.security.sasl.rawsendsize";

    /** Si se puede reusar una sesion ya autenticada. */
    public static final String REUSE = "javax.security.sasl.reuse";

    /** No aceptar mecanismos que manden la contrasena en claro. Ver la nota de la clase. */
    public static final String POLICY_NOPLAINTEXT = "javax.security.sasl.policy.noplaintext";

    /** No aceptar mecanismos vulnerables a un atacante activo. */
    public static final String POLICY_NOACTIVE = "javax.security.sasl.policy.noactive";

    /** No aceptar mecanismos vulnerables a un ataque de diccionario. */
    public static final String POLICY_NODICTIONARY = "javax.security.sasl.policy.nodictionary";

    /** No aceptar autenticacion anonima. */
    public static final String POLICY_NOANONYMOUS = "javax.security.sasl.policy.noanonymous";

    /** Solo mecanismos con secreto hacia adelante. */
    public static final String POLICY_FORWARD_SECRECY = "javax.security.sasl.policy.forward";

    /** Solo mecanismos que pasen credenciales del cliente. */
    public static final String POLICY_PASS_CREDENTIALS = "javax.security.sasl.policy.credentials";

    /** Las credenciales a usar, cuando el mecanismo las toma de afuera. */
    public static final String CREDENTIALS = "javax.security.sasl.credentials";

    /** El tipo de servicio de las fabricas de cliente. */
    private static final String CLIENT_SERVICE = "SaslClientFactory";

    /** El de las fabricas de servidor. */
    private static final String SERVER_SERVICE = "SaslServerFactory";

    /** Privado: la clase es solo metodos estaticos. */
    private Sasl() {
    }

    /**
     * Un cliente para el primero de esos mecanismos que alguna fabrica pueda atender.
     *
     * <p>Recorre las fabricas registradas en orden de proveedor. La primera que devuelva algo gana.
     *
     * @param mechanisms los mecanismos aceptables, en orden de preferencia
     * @return null si ninguna puede; ver la nota de la clase
     * @throws SaslException si una fabrica falla al construir
     */
    public static SaslClient createSaslClient(String[] mechanisms, String authorizationId,
                                              String protocol, String serverName,
                                              Map<String, ?> props, CallbackHandler cbh)
        throws SaslException {
        if (mechanisms == null) {
            throw new NullPointerException("mechanisms cannot be null");
        }
        int i = 0;
        while (i < mechanisms.length) {
            String mech = mechanisms[i];
            List<SaslClientFactory> factories = clientFactoriesFor(mech);
            int j = 0;
            while (j < factories.size()) {
                SaslClient made = factories.get(j).createSaslClient(
                    new String[] {mech}, authorizationId, protocol, serverName, props, cbh);
                if (made != null) {
                    return made;
                }
                j = j + 1;
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * Un servidor para ese mecanismo.
     *
     * @return null si ninguna fabrica puede
     */
    public static SaslServer createSaslServer(String mechanism, String protocol, String serverName,
                                              Map<String, ?> props, CallbackHandler cbh)
        throws SaslException {
        if (mechanism == null) {
            throw new NullPointerException("mechanism cannot be null");
        }
        List<SaslServerFactory> factories = serverFactoriesFor(mechanism);
        int i = 0;
        while (i < factories.size()) {
            SaslServer made = factories.get(i).createSaslServer(
                mechanism, protocol, serverName, props, cbh);
            if (made != null) {
                return made;
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * Todas las fabricas de cliente registradas.
     *
     * <p>Devuelve una {@code Enumeration} y no un {@code Iterator} porque la clase es de 2002, y
     * cambiarlo ahora rompe a quien la use.
     */
    public static Enumeration<SaslClientFactory> getSaslClientFactories() {
        List<SaslClientFactory> found = new ArrayList<SaslClientFactory>();
        for (Object o : allFactories(CLIENT_SERVICE)) {
            if (o instanceof SaslClientFactory) {
                found.add((SaslClientFactory) o);
            }
        }
        return Collections.enumeration(found);
    }

    /** Todas las fabricas de servidor registradas. */
    public static Enumeration<SaslServerFactory> getSaslServerFactories() {
        List<SaslServerFactory> found = new ArrayList<SaslServerFactory>();
        for (Object o : allFactories(SERVER_SERVICE)) {
            if (o instanceof SaslServerFactory) {
                found.add((SaslServerFactory) o);
            }
        }
        return Collections.enumeration(found);
    }

    /** Las fabricas de cliente que atienden ese mecanismo. */
    private static List<SaslClientFactory> clientFactoriesFor(String mech) throws SaslException {
        List<SaslClientFactory> found = new ArrayList<SaslClientFactory>();
        for (Object o : factoriesFor(CLIENT_SERVICE, mech)) {
            if (o instanceof SaslClientFactory) {
                found.add((SaslClientFactory) o);
            }
        }
        return found;
    }

    /** Las de servidor. */
    private static List<SaslServerFactory> serverFactoriesFor(String mech) throws SaslException {
        List<SaslServerFactory> found = new ArrayList<SaslServerFactory>();
        for (Object o : factoriesFor(SERVER_SERVICE, mech)) {
            if (o instanceof SaslServerFactory) {
                found.add((SaslServerFactory) o);
            }
        }
        return found;
    }

    /** Las instancias registradas para ese tipo de servicio y ese mecanismo. */
    private static List<Object> factoriesFor(String type, String mech) throws SaslException {
        List<Object> found = new ArrayList<Object>();
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService(type, mech);
            if (s != null) {
                try {
                    found.add(s.newInstance(null));
                } catch (Exception e) {
                    throw new SaslException(
                        "Cannot instantiate " + type + " for " + mech + " from "
                            + provs[i].getName(), e);
                }
            }
            i = i + 1;
        }
        return found;
    }

    /**
     * Todas las instancias de ese tipo de servicio, sin filtrar por mecanismo.
     *
     * <p>Una fabrica que atiende varios mecanismos aparece una sola vez: se la busca por su nombre
     * de clase, que es lo que la identifica.
     */
    private static List<Object> allFactories(String type) {
        List<Object> found = new ArrayList<Object>();
        List<String> seen = new ArrayList<String>();
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Set<Provider.Service> services = provs[i].getServices();
            for (Provider.Service s : services) {
                if (!type.equals(s.getType())) {
                    continue;
                }
                if (seen.contains(s.getClassName())) {
                    continue;
                }
                seen.add(s.getClassName());
                try {
                    found.add(s.newInstance(null));
                } catch (Exception e) {
                    // Una fabrica que no se puede construir no se enumera. Es lo unico que se puede
                    // hacer: el metodo no declara excepcion.
                }
            }
            i = i + 1;
        }
        return found;
    }
}
