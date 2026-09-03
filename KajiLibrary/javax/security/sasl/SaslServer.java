package javax.security.sasl;

/**
 * KajiLibrary's javax.security.sasl.SaslServer -- el lado que autentica.
 *
 * <p>El espejo de {@link SaslClient}, con dos diferencias que valen:
 *
 * <ul>
 *   <li>no tiene {@code hasInitialResponse}: es el cliente el que sabe si su mecanismo empieza
 *       hablando, y el servidor se entera de lo que llega;
 *   <li>tiene {@link #getAuthorizationID}, que es <b>el resultado</b> de todo esto. Terminada la
 *       negociacion, ese es el identificador en cuyo nombre hay que actuar -- ya pasado por el
 *       {@link AuthorizeCallback}, con el reescrito si el manejador lo reescribio.
 * </ul>
 *
 * <p>Leer {@code getAuthorizationID} antes de que {@link #isComplete} de true no tiene sentido: la
 * negociacion todavia puede fallar.
 */
public interface SaslServer {

    /** El nombre del mecanismo. */
    String getMechanismName();

    /**
     * Procesa una respuesta del cliente y produce el proximo desafio.
     *
     * @return el desafio a mandar, o null si no hay mas
     * @throws SaslException si la respuesta no sirve; la autenticacion fallo
     */
    byte[] evaluateResponse(byte[] response) throws SaslException;

    /** Si la negociacion termino. */
    boolean isComplete();

    /** En nombre de quien actuar. Ver la nota de la clase: recien vale al terminar. */
    String getAuthorizationID();

    /** Deshace la proteccion de un mensaje recibido. */
    byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException;

    /** Protege un mensaje a enviar. */
    byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException;

    /** Que se negocio de verdad; ver {@link SaslClient#getNegotiatedProperty}. */
    Object getNegotiatedProperty(String propName);

    /** Libera lo que tenga guardado. */
    void dispose() throws SaslException;
}
