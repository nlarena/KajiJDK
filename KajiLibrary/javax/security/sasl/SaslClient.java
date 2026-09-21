package javax.security.sasl;

/**
 * KajiLibrary's javax.security.sasl.SaslClient -- the side that authenticates itself.
 *
 * <p>SASL is a framework and not a mechanism: it defines how the challenges and responses <b>are
 * exchanged</b>, and leaves the content to each mechanism. This interface is that exchange seen
 * from the client, and that is why all its methods talk about opaque bytes.
 *
 * <p>The use is a loop: while {@link #isComplete} gives false, what arrived from the server is
 * passed to {@link #evaluateChallenge} and what it returns is sent. {@link #hasInitialResponse}
 * decides how the loop starts -- if it is true, the client speaks first and it has to be called
 * with an empty array. Guessing it wrong hangs the negotiation: both waiting for the other.
 *
 * <p>Once the authentication is over, {@link #wrap} and {@link #unwrap} protect the messages that
 * follow <b>if</b> a security layer was negotiated. If it was not, calling them is an error, and
 * the way to know is {@link #getNegotiatedProperty} with {@link Sasl#QOP}: the negotiation can end
 * in authentication only even though encryption was asked for.
 *
 * <p>{@link #dispose} erases the secret material; see the equivalent note in
 * {@code org.ietf.jgss.GSSCredential}.
 */
public interface SaslClient {

    /** The mechanism's name, for example {@code "DIGEST-MD5"}. */
    String getMechanismName();

    /** Whether the client speaks first. See the class note. */
    boolean hasInitialResponse();

    /**
     * Processes a challenge and produces the response.
     *
     * @param challenge what the server sent; empty on the first round if the client starts
     * @return what has to be sent to it, or null if nothing has to be sent
     * @throws SaslException if the challenge could not be processed
     */
    byte[] evaluateChallenge(byte[] challenge) throws SaslException;

    /** Whether the negotiation ended. */
    boolean isComplete();

    /** Undoes the protection of a received message. */
    byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException;

    /** Protects a message to be sent. See the class note on when it can be done. */
    byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException;

    /**
     * What was really negotiated.
     *
     * @param propName one of the constants of {@link Sasl}
     * @return null if that property was not negotiated
     */
    Object getNegotiatedProperty(String propName);

    /** Releases whatever it has kept. */
    void dispose() throws SaslException;
}
