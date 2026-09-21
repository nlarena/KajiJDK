package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.GSSCredential -- what an identity is proven with.
 *
 * <p>A credential joins a {@link GSSName} with the material that allows proving it --a Kerberos
 * ticket, a key-- and with an expiry. One of these objects can hold elements of <b>several</b>
 * mechanisms at a time, which is what {@link #add} is for: the same credential answers for the same
 * identity in Kerberos and in whatever comes next.
 *
 * <h2>Initiating and accepting are different things</h2>
 *
 * <p>{@link #INITIATE_ONLY} serves for connecting to others, {@link #ACCEPT_ONLY} for receiving
 * connections, and the two have separate expiries --hence {@link #getRemainingInitLifetime} and
 * {@link #getRemainingAcceptLifetime}--. A server normally wants only the second: asking for the
 * first as well gives the process the ability to <b>pass itself off</b> as the service against
 * third parties, which is exactly what one does not want if somebody compromises it.
 *
 * <h2>dispose has to be called</h2>
 *
 * <p>{@link #dispose} erases the secret material. Letting go of the reference is not enough: until
 * the collector picks it up, the keys are still in memory, and a dump of the process contains them.
 * It is the same reason why passwords go in a {@code char[]}.
 */
public interface GSSCredential extends Cloneable {

    /** It serves for both things. */
    public static final int INITIATE_AND_ACCEPT = 0;

    /** Only for connecting to others. */
    public static final int INITIATE_ONLY = 1;

    /** Only for receiving. See the note of the class. */
    public static final int ACCEPT_ONLY = 2;

    /** The default expiry of the mechanism. */
    public static final int DEFAULT_LIFETIME = 0;

    /** It does not expire. */
    public static final int INDEFINITE_LIFETIME = Integer.MAX_VALUE;

    /** It erases the secret material. See the note of the class. */
    void dispose() throws GSSException;

    /** The identity it answers for. */
    GSSName getName() throws GSSException;

    /** The same, in the form of that mechanism. */
    GSSName getName(Oid mech) throws GSSException;

    /**
     * How many seconds it has left.
     *
     * <p>Of whichever of its elements has least: the credential is valid as long as all of them
     * are.
     */
    int getRemainingLifetime() throws GSSException;

    /** How many seconds it has left for initiating with that mechanism. */
    int getRemainingInitLifetime(Oid mech) throws GSSException;

    /** How many for accepting. */
    int getRemainingAcceptLifetime(Oid mech) throws GSSException;

    /** What it is for: one of the three constants above. */
    int getUsage() throws GSSException;

    /** What it is for with that mechanism. */
    int getUsage(Oid mech) throws GSSException;

    /** The mechanisms it holds inside. */
    Oid[] getMechs() throws GSSException;

    /**
     * It adds an element of another mechanism to it.
     *
     * @param name the identity; null for the default one
     * @param initLifetime seconds for initiating
     * @param acceptLifetime seconds for accepting
     * @param mech the mechanism
     * @param usage one of the three constants above
     * @throws GSSException with {@link GSSException#DUPLICATE_ELEMENT} if it already had one of
     *     that mechanism
     */
    void add(GSSName name, int initLifetime, int acceptLifetime, Oid mech, int usage)
        throws GSSException;

    /** By identity and by what it contains. */
    boolean equals(Object another);

    /** Coherent with {@link #equals}. */
    int hashCode();
}
