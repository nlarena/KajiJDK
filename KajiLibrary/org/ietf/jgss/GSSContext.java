package org.ietf.jgss;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * KajiLibrary's org.ietf.jgss.GSSContext -- una conversacion segura entre dos partes.
 *
 * <p>Es la interfaz central del paquete y tiene dos etapas bien separadas: primero se <b>establece</b>
 * el contexto intercambiando tokens, y despues se lo usa para proteger mensajes.
 *
 * <h2>El establecimiento es un baile de tokens</h2>
 *
 * <p>Quien inicia llama a {@link #initSecContext} y obtiene un token; se lo manda al otro por donde
 * sea --GSS-API no transporta nada--, el otro se lo pasa a {@link #acceptSecContext} y obtiene otro,
 * y asi hasta que {@link #isEstablished} da true de los dos lados. Cuantas vueltas hacen falta
 * depende del mecanismo: por eso el bucle se escribe siempre igual y nunca se asume una sola vuelta.
 *
 * <h2>Lo que se pide antes, y lo que se obtiene despues</h2>
 *
 * <p>Los {@code request*} solo valen <b>antes</b> de empezar, y todos son pedidos y no ordenes: el
 * otro extremo o el mecanismo pueden no darlos. Por eso a cada uno le corresponde un {@code get*}
 * que dice que se consiguio, y comparar los dos es obligacion de quien usa el API. Pedir
 * {@link #requestConf} y no chequear {@link #getConfState} es mandar en claro creyendo que se
 * cifro -- el error mas caro de este paquete.
 *
 * <h2>Proteger mensajes: dos niveles</h2>
 *
 * <p>{@link #wrap} protege el mensaje entero --integridad, y cifrado si se pidio-- y devuelve un
 * token que reemplaza al mensaje. {@link #getMIC} deja el mensaje como esta y produce una etiqueta
 * <b>aparte</b>, que se verifica con {@link #verifyMIC}. La segunda sirve cuando el mensaje tiene
 * que seguir siendo legible para intermediarios que no participan de la seguridad.
 *
 * <p>{@link #getWrapSizeLimit} contesta cuanto se puede meter en un {@code wrap} sin pasarse de un
 * tamano de token dado. Existe porque la proteccion agranda, y cuanto depende del mecanismo y de si
 * hay cifrado: calcularlo a ojo es como se llega a tokens que el otro lado no puede recibir.
 *
 * <p>Cada metodo viene en dos formas, con arreglos y con flujos. La de flujos evita tener el mensaje
 * entero en memoria dos veces, que para un mensaje grande es la diferencia.
 */
public interface GSSContext {

    /** El vencimiento por omision del mecanismo. */
    public static final int DEFAULT_LIFETIME = 0;

    /** No vence. */
    public static final int INDEFINITE_LIFETIME = Integer.MAX_VALUE;

    /**
     * Una vuelta del establecimiento, del lado que inicia.
     *
     * @return el token a mandarle al otro, o null si no hay mas nada que mandar
     */
    byte[] initSecContext(byte[] inputBuf, int offset, int len) throws GSSException;

    /**
     * Idem, con flujos.
     *
     * @return cuantos bytes se escribieron
     */
    int initSecContext(InputStream inStream, OutputStream outStream) throws GSSException;

    /** Una vuelta del establecimiento, del lado que acepta. */
    byte[] acceptSecContext(byte[] inTok, int offset, int len) throws GSSException;

    /** Idem, con flujos. */
    void acceptSecContext(InputStream inStream, OutputStream outStream) throws GSSException;

    /** Si ya se puede usar para proteger mensajes. */
    boolean isEstablished();

    /** Libera lo que el contexto tenga. Despues de esto no sirve para nada. */
    void dispose() throws GSSException;

    /**
     * Cuanto mensaje entra en un token de ese tamano.
     *
     * <p>Ver la nota de la clase sobre por que no se calcula a ojo.
     */
    int getWrapSizeLimit(int qop, boolean confReq, int maxTokenSize) throws GSSException;

    /** Protege el mensaje y devuelve el token que lo reemplaza. */
    byte[] wrap(byte[] inBuf, int offset, int len, MessageProp msgProp) throws GSSException;

    /** Idem, con flujos. */
    void wrap(InputStream inStream, OutputStream outStream, MessageProp msgProp)
        throws GSSException;

    /**
     * Deshace un {@link #wrap}.
     *
     * <p>El {@code msgProp} vuelve <b>lleno</b> con lo que de verdad paso, incluidos los avisos de
     * duplicado y desorden; ver {@link MessageProp}.
     */
    byte[] unwrap(byte[] inBuf, int offset, int len, MessageProp msgProp) throws GSSException;

    /** Idem, con flujos. */
    void unwrap(InputStream inStream, OutputStream outStream, MessageProp msgProp)
        throws GSSException;

    /** La etiqueta de integridad de un mensaje que viaja aparte. Ver la nota de la clase. */
    byte[] getMIC(byte[] inMsg, int offset, int len, MessageProp msgProp) throws GSSException;

    /** Idem, con flujos. */
    void getMIC(InputStream inStream, OutputStream outStream, MessageProp msgProp)
        throws GSSException;

    /**
     * Comprueba una etiqueta contra su mensaje.
     *
     * @throws GSSException con {@link GSSException#BAD_MIC} si no corresponde
     */
    void verifyMIC(byte[] inTok, int tokOffset, int tokLen, byte[] inMsg, int msgOffset, int msgLen,
                   MessageProp msgProp) throws GSSException;

    /** Idem, con flujos. */
    void verifyMIC(InputStream tokStream, InputStream msgStream, MessageProp msgProp)
        throws GSSException;

    /** Serializa el contexto para pasarlo a otro proceso. */
    byte[] export() throws GSSException;

    /** Pide autenticacion mutua. Antes de empezar; ver la nota de la clase. */
    void requestMutualAuth(boolean state) throws GSSException;

    /** Pide deteccion de repeticion. */
    void requestReplayDet(boolean state) throws GSSException;

    /** Pide deteccion de desorden. */
    void requestSequenceDet(boolean state) throws GSSException;

    /**
     * Pide delegar la credencial al otro extremo.
     *
     * <p>Es el pedido mas caro de todos: el otro queda pudiendo actuar <b>en nombre de uno</b>
     * contra terceros.
     */
    void requestCredDeleg(boolean state) throws GSSException;

    /** Pide iniciar sin decir quien es. */
    void requestAnonymity(boolean state) throws GSSException;

    /** Pide cifrado ademas de integridad. */
    void requestConf(boolean state) throws GSSException;

    /** Pide integridad. */
    void requestInteg(boolean state) throws GSSException;

    /** Pide un vencimiento en segundos. */
    void requestLifetime(int lifetime) throws GSSException;

    /** Ata el contexto al canal; ver {@link ChannelBinding}. */
    void setChannelBinding(ChannelBinding cb) throws GSSException;

    /** Si la credencial se delego de verdad. */
    boolean getCredDelegState();

    /** Si la autenticacion es mutua de verdad. */
    boolean getMutualAuthState();

    /** Si hay deteccion de repeticion de verdad. */
    boolean getReplayDetState();

    /** Si hay deteccion de desorden de verdad. */
    boolean getSequenceDetState();

    /** Si el iniciador quedo anonimo de verdad. */
    boolean getAnonymityState();

    /** Si el contexto se puede exportar. */
    boolean isTransferable() throws GSSException;

    /**
     * Si ya se pueden proteger mensajes.
     *
     * <p>Puede dar true <b>antes</b> de {@link #isEstablished}: algunos mecanismos habilitan la
     * proteccion antes de terminar el ultimo intercambio.
     */
    boolean isProtReady();

    /** Si hay cifrado de verdad. Ver la nota de la clase. */
    boolean getConfState();

    /** Si hay integridad de verdad. */
    boolean getIntegState();

    /** Cuantos segundos le quedan. */
    int getLifetime();

    /** Quien inicio. */
    GSSName getSrcName() throws GSSException;

    /** Contra quien se inicio. */
    GSSName getTargName() throws GSSException;

    /** Que mecanismo quedo en uso. */
    Oid getMech() throws GSSException;

    /** La credencial que el otro delego, o null si no delego. */
    GSSCredential getDelegCred() throws GSSException;

    /** Si este lado es el que inicio. */
    boolean isInitiator() throws GSSException;
}
