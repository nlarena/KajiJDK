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
 * KajiLibrary's javax.security.sasl.Sasl -- the way into SASL.
 *
 * <p>Static methods and a long list of constants. The constants are the names of the properties
 * passed to the negotiation, and they split into two groups that are best not mixed:
 *
 * <ul>
 *   <li>the <b>configuration</b> ones --{@link #QOP}, {@link #STRENGTH}, {@link #MAX_BUFFER}-- say
 *       what is wanted;
 *   <li>the <b>policy</b> ones --the {@code POLICY_*}-- say what is <b>not</b> accepted, and act
 *       earlier: they filter which mechanisms are even offered.
 * </ul>
 *
 * <p>The distinction matters because the policy ones are the ones that really protect.
 * {@link #POLICY_NOPLAINTEXT} takes the mechanisms that send the password in the clear off the
 * list, and that is stronger than asking for encryption: a mechanism that is not on the list cannot
 * be chosen either by mistake or because the server pushes it.
 *
 * <h2>Returning null is not failing</h2>
 *
 * <p>{@link #createSaslClient} and {@link #createSaslServer} return <b>null</b> when no registered
 * factory can handle what was asked. It is the right thing and it has to be caught: it means "there
 * is no mechanism in common", which is a normal answer of a negotiation, not an exception.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library registers no SASL factory, so both {@code create}s return null and both
 * enumerations come empty. The lookup is really implemented --it walks the security providers
 * looking for {@code SaslClientFactory} and {@code SaslServerFactory} services-- so registering a
 * factory is enough for everything to work.
 */
public class Sasl {

    /** The requested quality of protection: {@code auth}, {@code auth-int} or {@code auth-conf}. */
    public static final String QOP = "javax.security.sasl.qop";

    /** The strength of the encryption: {@code low}, {@code medium} or {@code high}. */
    public static final String STRENGTH = "javax.security.sasl.strength";

    /** Whether the server also has to authenticate. */
    public static final String SERVER_AUTH = "javax.security.sasl.server.authentication";

    /** The name of the server the channel is bound to. */
    public static final String BOUND_SERVER_NAME = "javax.security.sasl.bound.server.name";

    /** The maximum size of a received block. */
    public static final String MAX_BUFFER = "javax.security.sasl.maxbuffer";

    /** The maximum size of a sent block. */
    public static final String RAW_SEND_SIZE = "javax.security.sasl.rawsendsize";

    /** Whether an already authenticated session can be reused. */
    public static final String REUSE = "javax.security.sasl.reuse";

    /** Do not accept mechanisms that send the password in the clear. See the class note. */
    public static final String POLICY_NOPLAINTEXT = "javax.security.sasl.policy.noplaintext";

    /** Do not accept mechanisms vulnerable to an active attacker. */
    public static final String POLICY_NOACTIVE = "javax.security.sasl.policy.noactive";

    /** Do not accept mechanisms vulnerable to a dictionary attack. */
    public static final String POLICY_NODICTIONARY = "javax.security.sasl.policy.nodictionary";

    /** Do not accept anonymous authentication. */
    public static final String POLICY_NOANONYMOUS = "javax.security.sasl.policy.noanonymous";

    /** Only mechanisms with forward secrecy. */
    public static final String POLICY_FORWARD_SECRECY = "javax.security.sasl.policy.forward";

    /** Only mechanisms that pass on client credentials. */
    public static final String POLICY_PASS_CREDENTIALS = "javax.security.sasl.policy.credentials";

    /** The credentials to use, when the mechanism takes them from outside. */
    public static final String CREDENTIALS = "javax.security.sasl.credentials";

    /** The service type of the client factories. */
    private static final String CLIENT_SERVICE = "SaslClientFactory";

    /** That of the server factories. */
    private static final String SERVER_SERVICE = "SaslServerFactory";

    /** Private: the class is static methods only. */
    private Sasl() {
    }

    /**
     * A client for the first of those mechanisms that some factory can handle.
     *
     * <p>It walks the registered factories in provider order. The first that returns something
     * wins.
     *
     * @param mechanisms the acceptable mechanisms, in order of preference
     * @return null if none can; see the class note
     * @throws SaslException if a factory fails while building
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
     * A server for that mechanism.
     *
     * @return null if no factory can
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
     * All the registered client factories.
     *
     * <p>It returns an {@code Enumeration} and not an {@code Iterator} because that is how the JDK
     * declares it, and changing it now would break whoever uses it. (The note said the class is
     * from 2002; the JDK marks it {@code @since 1.5}, and its file header starts in 1999.)
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

    /** All the registered server factories. */
    public static Enumeration<SaslServerFactory> getSaslServerFactories() {
        List<SaslServerFactory> found = new ArrayList<SaslServerFactory>();
        for (Object o : allFactories(SERVER_SERVICE)) {
            if (o instanceof SaslServerFactory) {
                found.add((SaslServerFactory) o);
            }
        }
        return Collections.enumeration(found);
    }

    /** The client factories that handle that mechanism. */
    private static List<SaslClientFactory> clientFactoriesFor(String mech) throws SaslException {
        List<SaslClientFactory> found = new ArrayList<SaslClientFactory>();
        for (Object o : factoriesFor(CLIENT_SERVICE, mech)) {
            if (o instanceof SaslClientFactory) {
                found.add((SaslClientFactory) o);
            }
        }
        return found;
    }

    /** The server ones. */
    private static List<SaslServerFactory> serverFactoriesFor(String mech) throws SaslException {
        List<SaslServerFactory> found = new ArrayList<SaslServerFactory>();
        for (Object o : factoriesFor(SERVER_SERVICE, mech)) {
            if (o instanceof SaslServerFactory) {
                found.add((SaslServerFactory) o);
            }
        }
        return found;
    }

    /** The registered instances for that service type and that mechanism. */
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
     * All the instances of that service type, without filtering by mechanism.
     *
     * <p>A factory that handles several mechanisms appears only once: it is looked up by its class
     * name, which is what identifies it.
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
                    // A factory that cannot be built is not enumerated. It is the only thing that
                    // can be done: the method declares no exception.
                }
            }
            i = i + 1;
        }
        return found;
    }
}
