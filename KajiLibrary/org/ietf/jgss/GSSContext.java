package org.ietf.jgss;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * KajiLibrary's org.ietf.jgss.GSSContext -- a secure conversation between two parties.
 *
 * <p>It is the central interface of the package and it has two well separated stages: first the
 * context is <b>established</b> by exchanging tokens, and then it is used to protect messages.
 *
 * <h2>The establishment is a dance of tokens</h2>
 *
 * <p>The initiator calls {@link #initSecContext} and gets a token; it sends it to the other by
 * whatever means --GSS-API transports nothing--, the other passes it to {@link #acceptSecContext}
 * and gets another one, and so on until {@link #isEstablished} gives true on both sides. How many
 * rounds are needed depends on the mechanism: that is why the loop is always written the same and a
 * single round is never assumed.
 *
 * <h2>What is asked for before, and what is obtained after</h2>
 *
 * <p>The {@code request*} are only valid <b>before</b> starting, and all of them are requests and
 * not orders: the other end or the mechanism may not grant them. That is why each one has a
 * matching {@code get*} that says what was obtained, and comparing the two is the duty of whoever
 * uses the API. Asking for {@link #requestConf} and not checking {@link #getConfState} is sending
 * in clear believing one encrypted -- the most expensive mistake of this package.
 *
 * <h2>Protecting messages: two levels</h2>
 *
 * <p>{@link #wrap} protects the whole message --integrity, and encryption if it was asked for-- and
 * returns a token that replaces the message. {@link #getMIC} leaves the message as it is and
 * produces a label <b>apart</b>, which is verified with {@link #verifyMIC}. The second one serves
 * when the message has to stay readable for intermediaries that take no part in the security.
 *
 * <p>{@link #getWrapSizeLimit} answers how much can be put into a {@code wrap} without going past a
 * given token size. It exists because protection makes things bigger, and by how much depends on
 * the mechanism and on whether there is encryption: guessing it is how one ends up with tokens the
 * other side cannot receive.
 *
 * <p>Each method comes in two forms, with arrays and with streams. The one with streams avoids
 * having the whole message in memory twice, which for a large message is the difference.
 */
public interface GSSContext {

    /** The default expiry of the mechanism. */
    public static final int DEFAULT_LIFETIME = 0;

    /** It does not expire. */
    public static final int INDEFINITE_LIFETIME = Integer.MAX_VALUE;

    /**
     * One round of the establishment, on the initiating side.
     *
     * @return the token to send to the other, or null if there is nothing more to send
     */
    byte[] initSecContext(byte[] inputBuf, int offset, int len) throws GSSException;

    /**
     * The same, with streams.
     *
     * @return how many bytes were written
     */
    int initSecContext(InputStream inStream, OutputStream outStream) throws GSSException;

    /** One round of the establishment, on the accepting side. */
    byte[] acceptSecContext(byte[] inTok, int offset, int len) throws GSSException;

    /** The same, with streams. */
    void acceptSecContext(InputStream inStream, OutputStream outStream) throws GSSException;

    /** Whether it can already be used to protect messages. */
    boolean isEstablished();

    /** It releases whatever the context holds. After this it is good for nothing. */
    void dispose() throws GSSException;

    /**
     * How much message fits in a token of that size.
     *
     * <p>See the note of the class on why it is not guessed.
     */
    int getWrapSizeLimit(int qop, boolean confReq, int maxTokenSize) throws GSSException;

    /** It protects the message and returns the token that replaces it. */
    byte[] wrap(byte[] inBuf, int offset, int len, MessageProp msgProp) throws GSSException;

    /** The same, with streams. */
    void wrap(InputStream inStream, OutputStream outStream, MessageProp msgProp)
        throws GSSException;

    /**
     * It undoes a {@link #wrap}.
     *
     * <p>The {@code msgProp} comes back <b>filled in</b> with what really happened, including the
     * warnings of duplicate and disorder; see {@link MessageProp}.
     */
    byte[] unwrap(byte[] inBuf, int offset, int len, MessageProp msgProp) throws GSSException;

    /** The same, with streams. */
    void unwrap(InputStream inStream, OutputStream outStream, MessageProp msgProp)
        throws GSSException;

    /** The integrity label of a message that travels apart. See the note of the class. */
    byte[] getMIC(byte[] inMsg, int offset, int len, MessageProp msgProp) throws GSSException;

    /** The same, with streams. */
    void getMIC(InputStream inStream, OutputStream outStream, MessageProp msgProp)
        throws GSSException;

    /**
     * It checks a label against its message.
     *
     * @throws GSSException with {@link GSSException#BAD_MIC} if it does not match
     */
    void verifyMIC(byte[] inTok, int tokOffset, int tokLen, byte[] inMsg, int msgOffset, int msgLen,
                   MessageProp msgProp) throws GSSException;

    /** The same, with streams. */
    void verifyMIC(InputStream tokStream, InputStream msgStream, MessageProp msgProp)
        throws GSSException;

    /** It serialises the context in order to pass it to another process. */
    byte[] export() throws GSSException;

    /** It asks for mutual authentication. Before starting; see the note of the class. */
    void requestMutualAuth(boolean state) throws GSSException;

    /** It asks for detection of replay. */
    void requestReplayDet(boolean state) throws GSSException;

    /** It asks for detection of disorder. */
    void requestSequenceDet(boolean state) throws GSSException;

    /**
     * It asks for the credential to be delegated to the other end.
     *
     * <p>It is the most expensive request of all: the other is left able to act <b>on one's
     * behalf</b> against third parties.
     */
    void requestCredDeleg(boolean state) throws GSSException;

    /** It asks to initiate without saying who one is. */
    void requestAnonymity(boolean state) throws GSSException;

    /** It asks for encryption besides integrity. */
    void requestConf(boolean state) throws GSSException;

    /** It asks for integrity. */
    void requestInteg(boolean state) throws GSSException;

    /** It asks for an expiry in seconds. */
    void requestLifetime(int lifetime) throws GSSException;

    /** It ties the context to the channel; see {@link ChannelBinding}. */
    void setChannelBinding(ChannelBinding cb) throws GSSException;

    /** Whether the credential really was delegated. */
    boolean getCredDelegState();

    /** Whether the authentication really is mutual. */
    boolean getMutualAuthState();

    /** Whether there really is detection of replay. */
    boolean getReplayDetState();

    /** Whether there really is detection of disorder. */
    boolean getSequenceDetState();

    /** Whether the initiator really was left anonymous. */
    boolean getAnonymityState();

    /** Whether the context can be exported. */
    boolean isTransferable() throws GSSException;

    /**
     * Whether messages can already be protected.
     *
     * <p>It may give true <b>before</b> {@link #isEstablished}: some mechanisms enable the
     * protection before finishing the last exchange.
     */
    boolean isProtReady();

    /** Whether there really is encryption. See the note of the class. */
    boolean getConfState();

    /** Whether there really is integrity. */
    boolean getIntegState();

    /** How many seconds it has left. */
    int getLifetime();

    /** Who initiated. */
    GSSName getSrcName() throws GSSException;

    /** Against whom it was initiated. */
    GSSName getTargName() throws GSSException;

    /** Which mechanism was left in use. */
    Oid getMech() throws GSSException;

    /** The credential the other delegated, or null if it did not delegate. */
    GSSCredential getDelegCred() throws GSSException;

    /** Whether this side is the one that initiated. */
    boolean isInitiator() throws GSSException;
}
