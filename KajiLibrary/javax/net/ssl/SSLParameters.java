package javax.net.ssl;

import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Toda la configuracion de una conexion TLS, en un objeto.
 *
 * <h2>Por que existe si {@link SSLSocket} ya tiene setters</h2>
 *
 * <p>Porque los setters sueltos aplican <strong>de a uno</strong>, y varias de estas opciones solo
 * tienen sentido juntas: cambiar las suites sin cambiar los protocolos puede dejar una combinacion
 * que no negocia nada. Este objeto se arma entero y se aplica de una, y ademas se puede guardar,
 * pasar y reusar entre conexiones — un socket no.
 *
 * <p>Es <strong>mutable y no se comparte</strong>: los getters de {@link SSLSocket} y
 * {@link SSLEngine} devuelven una copia, y modificar lo que devolvieron no toca la conexion hasta
 * que se lo pase al setter. Es deliberado, y es lo contrario de lo que uno supone.
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

    /** Todo por omision: sin suites ni protocolos fijados. */
    public SSLParameters() {
    }

    /** Fijando las suites de cifrado. */
    public SSLParameters(String[] cipherSuites) {
        setCipherSuites(cipherSuites);
    }

    /** Fijando las suites y los protocolos. */
    public SSLParameters(String[] cipherSuites, String[] protocols) {
        setCipherSuites(cipherSuites);
        setProtocols(protocols);
    }

    private static String[] copia(String[] v) {
        return v == null ? null : v.clone();
    }

    /** Las suites habilitadas, o {@code null}. Una copia: mutarla no cambia nada. */
    public String[] getCipherSuites() {
        return copia(this.cipherSuites);
    }

    /** Fija las suites habilitadas. */
    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = copia(cipherSuites);
    }

    /** Los protocolos habilitados, o {@code null}. */
    public String[] getProtocols() {
        return copia(this.protocols);
    }

    /** Fija los protocolos habilitados. */
    public void setProtocols(String[] protocols) {
        this.protocols = copia(protocols);
    }

    /**
     * Si se pide autenticacion de cliente sin exigirla.
     *
     * <p>La diferencia con {@link #getNeedClientAuth} es lo que pasa cuando el cliente no tiene
     * certificado: con {@code want} la conexion sigue sin autenticar, con {@code need} se corta. Son
     * excluyentes — fijar uno apaga el otro, y por eso los setters lo hacen explicitamente.
     */
    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    /** Pide autenticacion de cliente sin exigirla; apaga {@code needClientAuth}. */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
        this.needClientAuth = false;
    }

    /** Si se exige autenticacion de cliente. */
    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /** Exige autenticacion de cliente; apaga {@code wantClientAuth}. */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
        this.wantClientAuth = false;
    }

    /** Las restricciones sobre algoritmos, o {@code null}. */
    public AlgorithmConstraints getAlgorithmConstraints() {
        return this.algorithmConstraints;
    }

    /** Fija las restricciones sobre algoritmos. */
    public void setAlgorithmConstraints(AlgorithmConstraints constraints) {
        this.algorithmConstraints = constraints;
    }

    /**
     * El algoritmo con el que se verifica que el certificado corresponda al destino, o {@code null}.
     *
     * <p>{@code null} —el valor por omision— significa <strong>que no se verifica</strong>, y es una
     * de las trampas mas caras de esta API: un {@link SSLSocket} recien creado cifra pero no
     * comprueba que del otro lado este quien se pidio. Ponerle {@code "HTTPS"} es lo que activa esa
     * comprobacion.
     */
    public String getEndpointIdentificationAlgorithm() {
        return this.identificationAlgorithm;
    }

    /** Fija el algoritmo de identificacion del extremo; {@code "HTTPS"} es el habitual. */
    public void setEndpointIdentificationAlgorithm(String algorithm) {
        this.identificationAlgorithm = algorithm;
    }

    /**
     * Los nombres SNI a mandar.
     *
     * @throws NullPointerException si la lista es {@code null}
     * @throws IllegalArgumentException si hay dos del mismo tipo — el protocolo admite uno por tipo,
     *     y mandar dos seria ambiguo
     */
    public final void setServerNames(List<SNIServerName> serverNames) {
        if (serverNames == null) {
            throw new NullPointerException("serverNames");
        }
        Map<Integer, SNIServerName> m = new HashMap<Integer, SNIServerName>();
        for (int i = 0; i < serverNames.size(); i++) {
            SNIServerName n = serverNames.get(i);
            if (m.put(Integer.valueOf(n.getType()), n) != null) {
                throw new IllegalArgumentException("dos nombres del mismo tipo: "
                        + String.valueOf(n.getType()));
            }
        }
        this.sniNames = m;
    }

    /** Los nombres SNI, o {@code null} si no se fijaron. */
    public final List<SNIServerName> getServerNames() {
        if (this.sniNames == null) {
            return null;
        }
        return Collections.unmodifiableList(
                new ArrayList<SNIServerName>(this.sniNames.values()));
    }

    /**
     * Los criterios con los que un servidor acepta nombres SNI.
     *
     * @throws IllegalArgumentException si hay dos del mismo tipo
     */
    public final void setSNIMatchers(Collection<SNIMatcher> matchers) {
        if (matchers == null) {
            throw new NullPointerException("matchers");
        }
        Map<Integer, SNIMatcher> m = new HashMap<Integer, SNIMatcher>();
        for (SNIMatcher x : matchers) {
            if (m.put(Integer.valueOf(x.getType()), x) != null) {
                throw new IllegalArgumentException("dos matchers del mismo tipo: "
                        + String.valueOf(x.getType()));
            }
        }
        this.sniMatchers = m;
    }

    /** Los matchers SNI, o {@code null}. */
    public final Collection<SNIMatcher> getSNIMatchers() {
        if (this.sniMatchers == null) {
            return null;
        }
        return Collections.unmodifiableCollection(
                new ArrayList<SNIMatcher>(this.sniMatchers.values()));
    }

    /**
     * Si manda el orden de suites del servidor y no el del cliente.
     *
     * <p>Importa: quien elige el orden elige, en la practica, la suite. Dejar decidir al cliente
     * significa aceptar su idea de que es seguro.
     */
    public final void setUseCipherSuitesOrder(boolean honorOrder) {
        this.preferLocalCipherSuites = honorOrder;
    }

    /** Si manda el orden local de suites. */
    public final boolean getUseCipherSuitesOrder() {
        return this.preferLocalCipherSuites;
    }

    /**
     * Si se retransmiten los mensajes de handshake perdidos. Solo aplica a DTLS.
     *
     * <p>Sobre TCP no hace falta porque el transporte ya retransmite; sobre datagramas, si no lo
     * hace el protocolo no lo hace nadie.
     */
    public void setEnableRetransmissions(boolean enableRetransmissions) {
        this.enableRetransmissions = enableRetransmissions;
    }

    /** Si las retransmisiones estan habilitadas. */
    public boolean getEnableRetransmissions() {
        return this.enableRetransmissions;
    }

    /**
     * El paquete mas grande que se puede producir; {@code 0} deja decidir a la implementacion.
     *
     * @throws IllegalArgumentException si es negativo
     */
    public void setMaximumPacketSize(int maximumPacketSize) {
        if (maximumPacketSize < 0) {
            throw new IllegalArgumentException("el tamano maximo no puede ser negativo");
        }
        this.maximumPacketSize = maximumPacketSize;
    }

    /** El tamano maximo de paquete. */
    public int getMaximumPacketSize() {
        return this.maximumPacketSize;
    }

    /**
     * Los protocolos de aplicacion a negociar por ALPN, en orden de preferencia.
     *
     * <p>Es como un cliente y un servidor acuerdan hablar HTTP/2 en vez de HTTP/1.1
     * <strong>dentro</strong> del mismo handshake, sin un viaje extra.
     */
    public String[] getApplicationProtocols() {
        return this.applicationProtocols.clone();
    }

    /**
     * Fija los protocolos de aplicacion.
     *
     * @throws IllegalArgumentException si alguno es {@code null} o vacio
     */
    public void setApplicationProtocols(String[] protocols) {
        if (protocols == null) {
            throw new IllegalArgumentException("protocols");
        }
        String[] copia = protocols.clone();
        for (int i = 0; i < copia.length; i++) {
            if (copia[i] == null || copia[i].isEmpty()) {
                throw new IllegalArgumentException(
                        "un protocolo de aplicacion no puede ser nulo ni vacio");
            }
        }
        this.applicationProtocols = copia;
    }

    /** Los esquemas de firma habilitados, o {@code null}. */
    public String[] getSignatureSchemes() {
        return copia(this.signatureSchemes);
    }

    /** Fija los esquemas de firma. */
    public void setSignatureSchemes(String[] signatureSchemes) {
        this.signatureSchemes = copia(signatureSchemes);
    }

    /** Los grupos con nombre para el intercambio de claves, o {@code null}. */
    public String[] getNamedGroups() {
        return copia(this.namedGroups);
    }

    /** Fija los grupos con nombre. */
    public void setNamedGroups(String[] namedGroups) {
        this.namedGroups = copia(namedGroups);
    }
}
