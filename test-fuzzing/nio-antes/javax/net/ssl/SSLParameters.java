package javax.net.ssl;

import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All the configuration of a TLS connection, in one object.
 *
 * <h2>Why it exists if {@link SSLSocket} already has setters</h2>
 *
 * <p>Because the loose setters apply <strong>one at a time</strong>, and several of these options
 * only make sense together: changing the suites without changing the protocols can leave a
 * combination that negotiates nothing. This object is put together whole and applied in one go, and
 * besides it can be kept, passed around and reused between connections — a socket cannot.
 *
 * <p>It is <strong>mutable and not shared</strong>: the getters of {@link SSLSocket} and
 * {@link SSLEngine} return a copy, and modifying what they returned does not touch the connection
 * until it is passed to the setter. It is deliberate, and it is the opposite of what one assumes.
 */
public class SSLParameters {

    private String[] cipherSuites;
    private String[] protocols;
    private boolean wantClientAuth;
    private boolean needClientAuth;
    private AlgorithmConstraints algorithmConstraints;
    private String identificationAlgorithm;
    private Map<Integer, SNIServerName> sniNames;
    private Map<Integer, SNIMatcher> sniMatchers;
    private boolean preferLocalCipherSuites;
    private boolean enableRetransmissions = true;
    private int maximumPacketSize;
    private String[] applicationProtocols = new String[0];
    private String[] signatureSchemes;
    private String[] namedGroups;

    /** Everything default: no suites nor protocols set. */
    public SSLParameters() {
    }

    /** Setting the cipher suites. */
    public SSLParameters(String[] cipherSuites) {
        setCipherSuites(cipherSuites);
    }

    /** Setting the suites and the protocols. */
    public SSLParameters(String[] cipherSuites, String[] protocols) {
        setCipherSuites(cipherSuites);
        setProtocols(protocols);
    }

    private static String[] copy(String[] v) {
        return v == null ? null : v.clone();
    }

    /** The enabled suites, or {@code null}. A copy: mutating it changes nothing. */
    public String[] getCipherSuites() {
        return copy(this.cipherSuites);
    }

    /** Sets the enabled suites. */
    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = copy(cipherSuites);
    }

    /** The enabled protocols, or {@code null}. */
    public String[] getProtocols() {
        return copy(this.protocols);
    }

    /** Sets the enabled protocols. */
    public void setProtocols(String[] protocols) {
        this.protocols = copy(protocols);
    }

    /**
     * Whether client authentication is requested without requiring it.
     *
     * <p>The difference from {@link #getNeedClientAuth} is what happens when the client has no
     * certificate: with {@code want} the connection goes on unauthenticated, with {@code need} it
     * is cut. They are mutually exclusive — setting one turns the other off, and that is why the
     * setters do it explicitly.
     */
    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    /** Requests client authentication without requiring it; turns {@code needClientAuth} off. */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
        this.needClientAuth = false;
    }

    /** Whether client authentication is required. */
    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /** Requires client authentication; turns {@code wantClientAuth} off. */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
        this.wantClientAuth = false;
    }

    /** The constraints on algorithms, or {@code null}. */
    public AlgorithmConstraints getAlgorithmConstraints() {
        return this.algorithmConstraints;
    }

    /** Sets the constraints on algorithms. */
    public void setAlgorithmConstraints(AlgorithmConstraints constraints) {
        this.algorithmConstraints = constraints;
    }

    /**
     * The algorithm with which it is checked that the certificate belongs to the destination, or
     * {@code null}.
     *
     * <p>{@code null} --the default-- means <strong>that it is not checked</strong>, and it is one
     * of the most expensive traps of this API: a freshly created {@link SSLSocket} encrypts but
     * does not check that whoever was asked for is on the other side. Setting it to {@code "HTTPS"}
     * is what turns that check on.
     */
    public String getEndpointIdentificationAlgorithm() {
        return this.identificationAlgorithm;
    }

    /** Sets the endpoint identification algorithm; {@code "HTTPS"} is the usual one. */
    public void setEndpointIdentificationAlgorithm(String algorithm) {
        this.identificationAlgorithm = algorithm;
    }

    /**
     * The SNI names to send.
     *
     * @throws NullPointerException if the list is {@code null}
     * @throws IllegalArgumentException if there are two of the same type — the protocol admits one
     *     per type, and sending two would be ambiguous
     */
    public final void setServerNames(List<SNIServerName> serverNames) {
        if (serverNames == null) {
            throw new NullPointerException("serverNames");
        }
        Map<Integer, SNIServerName> m = new HashMap<Integer, SNIServerName>();
        for (int i = 0; i < serverNames.size(); i++) {
            SNIServerName n = serverNames.get(i);
            if (m.put(Integer.valueOf(n.getType()), n) != null) {
                throw new IllegalArgumentException("two names of the same type: "
                        + String.valueOf(n.getType()));
            }
        }
        this.sniNames = m;
    }

    /** The SNI names, or {@code null} if they were not set. */
    public final List<SNIServerName> getServerNames() {
        if (this.sniNames == null) {
            return null;
        }
        return Collections.unmodifiableList(
                new ArrayList<SNIServerName>(this.sniNames.values()));
    }

    /**
     * The criteria with which a server accepts SNI names.
     *
     * @throws IllegalArgumentException if there are two of the same type
     */
    public final void setSNIMatchers(Collection<SNIMatcher> matchers) {
        if (matchers == null) {
            throw new NullPointerException("matchers");
        }
        Map<Integer, SNIMatcher> m = new HashMap<Integer, SNIMatcher>();
        for (SNIMatcher x : matchers) {
            if (m.put(Integer.valueOf(x.getType()), x) != null) {
                throw new IllegalArgumentException("two matchers of the same type: "
                        + String.valueOf(x.getType()));
            }
        }
        this.sniMatchers = m;
    }

    /** The SNI matchers, or {@code null}. */
    public final Collection<SNIMatcher> getSNIMatchers() {
        if (this.sniMatchers == null) {
            return null;
        }
        return Collections.unmodifiableCollection(
                new ArrayList<SNIMatcher>(this.sniMatchers.values()));
    }

    /**
     * Whether the server's suite order rules and not the client's.
     *
     * <p>It matters: whoever chooses the order chooses, in practice, the suite. Letting the client
     * decide means accepting its idea of what is secure.
     */
    public final void setUseCipherSuitesOrder(boolean honorOrder) {
        this.preferLocalCipherSuites = honorOrder;
    }

    /** Whether the local suite order rules. */
    public final boolean getUseCipherSuitesOrder() {
        return this.preferLocalCipherSuites;
    }

    /**
     * Whether lost handshake messages are retransmitted. It only applies to DTLS.
     *
     * <p>Over TCP it is not needed because the transport already retransmits; over datagrams, if
     * the protocol does not do it nobody does.
     */
    public void setEnableRetransmissions(boolean enableRetransmissions) {
        this.enableRetransmissions = enableRetransmissions;
    }

    /** Whether retransmissions are enabled. */
    public boolean getEnableRetransmissions() {
        return this.enableRetransmissions;
    }

    /**
     * The largest packet that can be produced; {@code 0} lets the implementation decide.
     *
     * @throws IllegalArgumentException if it is negative
     */
    public void setMaximumPacketSize(int maximumPacketSize) {
        if (maximumPacketSize < 0) {
            throw new IllegalArgumentException("the maximum size cannot be negative");
        }
        this.maximumPacketSize = maximumPacketSize;
    }

    /** The maximum packet size. */
    public int getMaximumPacketSize() {
        return this.maximumPacketSize;
    }

    /**
     * The application protocols to negotiate through ALPN, in order of preference.
     *
     * <p>It is how a client and a server agree to speak HTTP/2 instead of HTTP/1.1
     * <strong>within</strong> the same handshake, without an extra round trip.
     */
    public String[] getApplicationProtocols() {
        return this.applicationProtocols.clone();
    }

    /**
     * Sets the application protocols.
     *
     * @throws IllegalArgumentException if any is {@code null} or empty
     */
    public void setApplicationProtocols(String[] protocols) {
        if (protocols == null) {
            throw new IllegalArgumentException("protocols");
        }
        String[] copy = protocols.clone();
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] == null || copy[i].isEmpty()) {
                throw new IllegalArgumentException(
                        "an application protocol cannot be null or empty");
            }
        }
        this.applicationProtocols = copy;
    }

    /** The enabled signature schemes, or {@code null}. */
    public String[] getSignatureSchemes() {
        return copy(this.signatureSchemes);
    }

    /** Sets the signature schemes. */
    public void setSignatureSchemes(String[] signatureSchemes) {
        this.signatureSchemes = copy(signatureSchemes);
    }

    /** The named groups for the key exchange, or {@code null}. */
    public String[] getNamedGroups() {
        return copy(this.namedGroups);
    }

    /** Sets the named groups. */
    public void setNamedGroups(String[] namedGroups) {
        this.namedGroups = copy(namedGroups);
    }
}
