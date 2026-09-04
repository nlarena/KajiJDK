package javax.net.ssl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiFunction;

/**
 * TLS sin transporte: una maquina de estados que traduce entre bytes de aplicacion y bytes de red.
 *
 * <h2>Por que existe si ya hay {@link SSLSocket}</h2>
 *
 * <p>Porque un {@code SSLSocket} decide por vos como se lee y se escribe — bloqueando, un hilo por
 * conexion. Un servidor con muchas conexiones no puede pagar eso, y quiere multiplexar con un
 * selector; otro puede querer TLS sobre algo que ni siquiera es TCP.
 *
 * <p>Este motor separa las dos cosas: <strong>no toca la red</strong>. Se le dan buffers y devuelve
 * buffers; quien lo usa decide como viajan. Es potencia a cambio de responsabilidad, y por eso su
 * uso correcto es notoriamente delicado.
 *
 * <h2>El bucle, que es lo unico que hay que entender</h2>
 *
 * <p>Cada llamada devuelve un {@link SSLEngineResult} con dos estados, y quien llama tiene que
 * <strong>obedecerlos</strong>:
 *
 * <ul>
 * <li>{@code NEED_WRAP} — el motor tiene algo que mandar; llamar a {@link #wrap};</li>
 * <li>{@code NEED_UNWRAP} — necesita datos; leer de la red y llamar a {@link #unwrap};</li>
 * <li>{@code NEED_TASK} — hay trabajo pesado pendiente. Sacarlo con {@link #getDelegatedTask} y
 *     <strong>correrlo</strong>. Ignorar esto es el error mas comun: el handshake se queda quieto
 *     para siempre y no hay ninguna excepcion que lo diga;</li>
 * <li>{@code BUFFER_OVERFLOW} / {@code BUFFER_UNDERFLOW} — no son fallas: son pedidos de mas lugar
 *     o mas datos. Dimensionar con {@link SSLSession#getPacketBufferSize} y
 *     {@link SSLSession#getApplicationBufferSize} y reintentar.</li>
 * </ul>
 *
 * <h2>Los dos sentidos se cierran por separado</h2>
 *
 * <p>{@link #closeOutbound} y {@link #closeInbound} son distintos a proposito: TLS cierra cada
 * direccion con su propio aviso, y cerrar la salida sin haber recibido la del par deja la entrada
 * viva. Es lo que permite detectar un truncamiento — que alguien haya cortado la conexion para que
 * parezca que el mensaje termino ahi.
 */
public abstract class SSLEngine {

    private final String peerHost;
    private final int peerPort;

    /** Sin datos del par: no se puede reanudar sesion ni mandar SNI. */
    protected SSLEngine() {
        this(null, -1);
    }

    /**
     * Con el par sugerido.
     *
     * <p>Es una <em>pista</em>, no un destino: el motor no se conecta a nada. Sirve para dos cosas
     * concretas — reanudar una sesion con ese par, y mandar su nombre por SNI.
     */
    protected SSLEngine(String peerHost, int peerPort) {
        this.peerHost = peerHost;
        this.peerPort = peerPort;
    }

    /** El nombre del par que se sugirio, o {@code null}. */
    public String getPeerHost() {
        return this.peerHost;
    }

    /** El puerto del par que se sugirio, o {@code -1}. */
    public int getPeerPort() {
        return this.peerPort;
    }

    /** Cifra los datos de {@code src} hacia {@code dst}. */
    public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return wrap(new ByteBuffer[] { src }, 0, 1, dst);
    }

    /** Igual, tomando de varios buffers. */
    public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException {
        if (srcs == null) {
            throw new IllegalArgumentException("srcs");
        }
        return wrap(srcs, 0, srcs.length, dst);
    }

    /**
     * La forma general, sobre un tramo de {@code srcs}.
     *
     * <p>Es la unica abstracta de las tres: las otras dos delegan aca. Una implementacion escribe
     * una sola.
     */
    public abstract SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst)
            throws SSLException;

    /** Descifra los datos de {@code src} hacia {@code dst}. */
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return unwrap(src, new ByteBuffer[] { dst }, 0, 1);
    }

    /** Igual, repartiendo en varios buffers. */
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
        if (dsts == null) {
            throw new IllegalArgumentException("dsts");
        }
        return unwrap(src, dsts, 0, dsts.length);
    }

    /** La forma general, sobre un tramo de {@code dsts}. */
    public abstract SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset,
            int length) throws SSLException;

    /**
     * Una tarea pendiente, o {@code null} si no hay.
     *
     * <p>Se sacan y se corren hasta que devuelva {@code null}. Pueden correrse en otro hilo — es
     * justamente para eso que existen — pero <strong>tienen que correrse</strong>.
     */
    public abstract Runnable getDelegatedTask();

    /**
     * Cierra la entrada.
     *
     * @throws SSLException si el par no habia mandado su aviso de cierre, lo que puede significar un
     *     truncamiento y no un cierre
     */
    public abstract void closeInbound() throws SSLException;

    /** Si ya no se va a aceptar mas entrada. */
    public abstract boolean isInboundDone();

    /** Cierra la salida. Todavia hay que hacer un {@link #wrap} para emitir el aviso. */
    public abstract void closeOutbound();

    /** Si ya se emitio el aviso de cierre de salida. */
    public abstract boolean isOutboundDone();

    /** Todas las suites que el motor conoce. */
    public abstract String[] getSupportedCipherSuites();

    /** Las suites habilitadas ahora. */
    public abstract String[] getEnabledCipherSuites();

    /** Fija las suites habilitadas. */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** Todos los protocolos que el motor conoce. */
    public abstract String[] getSupportedProtocols();

    /** Los protocolos habilitados ahora. */
    public abstract String[] getEnabledProtocols();

    /** Fija los protocolos habilitados. */
    public abstract void setEnabledProtocols(String[] protocols);

    /**
     * La sesion vigente.
     *
     * <p>Antes del primer handshake devuelve una sesion vacia, con suite {@code SSL_NULL_WITH_NULL_NULL}
     * — no {@code null}. Es incomodo pero deliberado: obliga a mirar la suite en vez de suponer que
     * tener sesion significa estar autenticado.
     */
    public abstract SSLSession getSession();

    /**
     * La sesion que se esta negociando, o {@code null} si no hay handshake en curso.
     *
     * <p>Existe para poder decidir <em>durante</em> el handshake — elegir un certificado mirando el
     * SNI que acaba de llegar, por ejemplo— cuando {@link #getSession} todavia devuelve la vieja.
     */
    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException("este motor no expone la sesion en negociacion");
    }

    /** Arranca o renegocia el handshake. */
    public abstract void beginHandshake() throws SSLException;

    /** Que hace falta hacer ahora; ver el bucle en la descripcion de la clase. */
    public abstract SSLEngineResult.HandshakeStatus getHandshakeStatus();

    /**
     * Si este motor es el cliente.
     *
     * @throws IllegalArgumentException si ya empezo el handshake — el rol define todo el protocolo y
     *     cambiarlo a mitad de camino no significa nada
     */
    public abstract void setUseClientMode(boolean mode);

    /** Si es el cliente. */
    public abstract boolean getUseClientMode();

    /** Exige autenticacion de cliente; solo tiene sentido del lado servidor. */
    public abstract void setNeedClientAuth(boolean need);

    /** Si se exige autenticacion de cliente. */
    public abstract boolean getNeedClientAuth();

    /** Pide autenticacion de cliente sin exigirla. */
    public abstract void setWantClientAuth(boolean want);

    /** Si se pide autenticacion de cliente. */
    public abstract boolean getWantClientAuth();

    /** Si se pueden crear sesiones nuevas, o solo reanudar las que ya hay. */
    public abstract void setEnableSessionCreation(boolean flag);

    /** Si se pueden crear sesiones nuevas. */
    public abstract boolean getEnableSessionCreation();

    /** Toda la configuracion junta; ver {@link SSLParameters}. */
    public SSLParameters getSSLParameters() {
        SSLParameters p = new SSLParameters();
        p.setCipherSuites(getEnabledCipherSuites());
        p.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            p.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            p.setWantClientAuth(true);
        }
        return p;
    }

    /**
     * Aplica la configuracion.
     *
     * <p>Solo lo que no sea {@code null}: un {@link SSLParameters} recien creado tiene casi todo sin
     * fijar, y aplicarlo entero borraria lo que el motor ya tenia.
     */
    public void setSSLParameters(SSLParameters params) {
        String[] s = params.getCipherSuites();
        if (s != null) {
            setEnabledCipherSuites(s);
        }
        s = params.getProtocols();
        if (s != null) {
            setEnabledProtocols(s);
        }
        if (params.getNeedClientAuth()) {
            setNeedClientAuth(true);
        } else if (params.getWantClientAuth()) {
            setWantClientAuth(true);
        } else {
            setWantClientAuth(false);
        }
    }

    /** El protocolo de aplicacion acordado por ALPN, {@code ""} si ninguno, {@code null} si no se negocio. */
    public String getApplicationProtocol() {
        throw new UnsupportedOperationException("este motor no soporta ALPN");
    }

    /** El que se va acordando mientras dura el handshake. */
    public String getHandshakeApplicationProtocol() {
        throw new UnsupportedOperationException("este motor no soporta ALPN");
    }

    /**
     * Elige el protocolo de aplicacion con una funcion propia en vez de la lista.
     *
     * <p>Sirve del lado servidor, donde la eleccion puede depender de algo que solo se sabe mirando
     * al cliente concreto.
     */
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLEngine, List<String>, String> selector) {
        throw new UnsupportedOperationException("este motor no soporta ALPN");
    }

    /** El selector puesto, o {@code null}. */
    public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
        throw new UnsupportedOperationException("este motor no soporta ALPN");
    }
}
