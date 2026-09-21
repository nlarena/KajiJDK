package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.GSSName -- an identity, in one format or in several.
 *
 * <p>A GSS-API name is not a string: it is an identity that may have a different representation in
 * each mechanism. {@code "john@EXAMPLE.COM"} written by a person is a <b>generic</b> name; the same
 * one, resolved for Kerberos, is something else with other bytes.
 *
 * <h2>Generic names and mechanism names</h2>
 *
 * <p>{@link #isMN} tells the two forms apart and {@link #canonicalize} goes from the first to the
 * second. The difference matters when comparing: two generic names that look different may resolve
 * to the same identity --capitals, equivalent domains, aliases-- and only the mechanism knows
 * whether they are the same.
 *
 * <p>Hence the <b>two</b> {@code equals}. The one that receives a {@link GSSName} may throw and is
 * the one that really compares, resolving whatever is needed. The one that receives an {@code
 * Object} cannot throw --it inherits that from {@code Object}-- and that is why, when the
 * comparison cannot be made, it returns false instead of warning. Putting generic names into a
 * {@code HashSet} is, for that reason, a silent source of duplicates: using {@code canonicalize}
 * first is the only thing that avoids it.
 *
 * <h2>The types of name</h2>
 *
 * <p>The {@code NT_*} constants are OIDs that say <b>how to read</b> the string that was passed.
 * {@link #NT_USER_NAME} is a local user, {@link #NT_HOSTBASED_SERVICE} is {@code service@host}
 * --the most used in practice-- and {@link #NT_EXPORT_NAME} is the binary form of
 * {@link #export()}, meant for keeping and comparing without resolving anything again.
 *
 * <p>{@link #NT_ANONYMOUS} is the only one that names nobody: it is the identity of whoever
 * authenticates without saying who they are, and {@link #isAnonymous} is the way of not treating it
 * as an ordinary name by accident.
 */
public interface GSSName {

    /** {@code service@host}; the most used. */
    public static final Oid NT_HOSTBASED_SERVICE = Oid.literal("1.2.840.113554.1.2.1.4");

    /** A local user. */
    public static final Oid NT_USER_NAME = Oid.literal("1.2.840.113554.1.2.1.1");

    /** A numeric user identifier, in bytes. */
    public static final Oid NT_MACHINE_UID_NAME = Oid.literal("1.2.840.113554.1.2.1.2");

    /** The same, written as text. */
    public static final Oid NT_STRING_UID_NAME = Oid.literal("1.2.840.113554.1.2.1.3");

    /** Nobody. See the note of the class. */
    public static final Oid NT_ANONYMOUS = Oid.literal("1.3.6.1.5.6.3");

    /** The binary form of {@link #export}. */
    public static final Oid NT_EXPORT_NAME = Oid.literal("1.3.6.1.5.6.4");

    /**
     * Whether they name the same identity.
     *
     * @throws GSSException if the comparison could not be made
     */
    boolean equals(GSSName another) throws GSSException;

    /**
     * The same, without being able to warn.
     *
     * @return false if the comparison could not be made; see the note of the class
     */
    boolean equals(Object another);

    /** Coherent with the {@code equals} of {@code Object}. */
    int hashCode();

    /**
     * The same name, resolved for that mechanism.
     *
     * @param mech the OID of the mechanism
     */
    GSSName canonicalize(Oid mech) throws GSSException;

    /**
     * The binary form, for keeping or comparing.
     *
     * @throws GSSException if the name is not of a mechanism; it has to be canonicalised first
     */
    byte[] export() throws GSSException;

    /** The readable form. */
    String toString();

    /** The type of name of the string {@link #toString} returns. */
    Oid getStringNameType() throws GSSException;

    /** Whether it is the anonymous name. See the note of the class. */
    boolean isAnonymous();

    /** Whether it is already resolved for a mechanism. */
    boolean isMN();
}
