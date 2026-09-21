package javax.security.sasl;

/**
 * KajiLibrary's javax.security.sasl.SaslServer -- the side that authenticates.
 *
 * <p>The mirror of {@link SaslClient}, with two differences worth noting:
 *
 * <ul>
 *   <li>it has no {@code hasInitialResponse}: it is the client that knows whether its mechanism
 *       starts by speaking, and the server learns from what arrives;
 *   <li>it has {@link #getAuthorizationID}, which is <b>the result</b> of all this. Once the
 *       negotiation is over, that is the identifier on whose behalf to act -- already passed
 *       through the {@link AuthorizeCallback}, with the rewritten one if the handler rewrote it.
 * </ul>
 *
 * <p>Reading {@code getAuthorizationID} before {@link #isComplete} gives true makes no sense: the
 * negotiation can still fail.
 */
public interface SaslServer {

    /** The mechanism's name. */
    String getMechanismName();

    /**
     * Processes a response from the client and produces the next challenge.
     *
     * @return the challenge to send, or null if there are no more
     * @throws SaslException if the response is no good; the authentication failed
     */
    byte[] evaluateResponse(byte[] response) throws SaslException;

    /** Whether the negotiation ended. */
    boolean isComplete();

    /** On whose behalf to act. See the class note: it only holds at the end. */
    String getAuthorizationID();

    /** Undoes the protection of a received message. */
    byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException;

    /** Protects a message to be sent. */
    byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException;

    /** What was really negotiated; see {@link SaslClient#getNegotiatedProperty}. */
    Object getNegotiatedProperty(String propName);

    /** Releases whatever it has kept. */
    void dispose() throws SaslException;
}
