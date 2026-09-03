package javax.security.sasl;

/**
 * KajiLibrary's javax.security.sasl.SaslClient -- el lado que se autentica.
 *
 * <p>SASL es un marco y no un mecanismo: define como <b>se intercambian</b> los desafios y las
 * respuestas, y deja el contenido a cada mecanismo. Esta interfaz es ese intercambio visto desde el
 * cliente, y por eso todos sus metodos hablan de bytes opacos.
 *
 * <p>El uso es un bucle: mientras {@link #isComplete} de false, se le pasa lo que llego del servidor
 * a {@link #evaluateChallenge} y se manda lo que devuelve. {@link #hasInitialResponse} decide como
 * arranca el bucle -- si es true, el cliente habla primero y hay que llamar con un arreglo vacio.
 * Adivinarlo mal cuelga la negociacion: los dos esperando al otro.
 *
 * <p>Terminada la autenticacion, {@link #wrap} y {@link #unwrap} protegen los mensajes que siguen
 * <b>si</b> se negocio una capa de seguridad. Si no se negocio, llamarlos es un error, y la forma de
 * saberlo es {@link #getNegotiatedProperty} con {@link Sasl#QOP}: la negociacion puede terminar en
 * solo autenticacion aunque se haya pedido cifrado.
 *
 * <p>{@link #dispose} borra el material secreto; ver la nota equivalente en
 * {@code org.ietf.jgss.GSSCredential}.
 */
public interface SaslClient {

    /** El nombre del mecanismo, por ejemplo {@code "DIGEST-MD5"}. */
    String getMechanismName();

    /** Si el cliente habla primero. Ver la nota de la clase. */
    boolean hasInitialResponse();

    /**
     * Procesa un desafio y produce la respuesta.
     *
     * @param challenge lo que mando el servidor; vacio en la primera vuelta si el cliente empieza
     * @return lo que hay que mandarle, o null si no hay que mandar nada
     * @throws SaslException si el desafio no se pudo procesar
     */
    byte[] evaluateChallenge(byte[] challenge) throws SaslException;

    /** Si la negociacion termino. */
    boolean isComplete();

    /** Deshace la proteccion de un mensaje recibido. */
    byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException;

    /** Protege un mensaje a enviar. Ver la nota de la clase sobre cuando se puede. */
    byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException;

    /**
     * Que se negocio de verdad.
     *
     * @param propName una de las constantes de {@link Sasl}
     * @return null si esa propiedad no se negocio
     */
    Object getNegotiatedProperty(String propName);

    /** Libera lo que tenga guardado. */
    void dispose() throws SaslException;
}
