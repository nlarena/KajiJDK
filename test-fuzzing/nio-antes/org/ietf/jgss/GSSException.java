package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.GSSException -- a GSS-API operation failed.
 *
 * <p>It carries <b>two</b> codes and that is its peculiarity: a major code, from this list, which
 * is common to every implementation of GSS-API, and a minor one, which the mechanism underneath
 * defines --Kerberos, for example-- and which means nothing outside it.
 *
 * <p>The division comes from GSS-API being a layer over different mechanisms: the major code lets
 * an application react without knowing which one is underneath, and the minor one keeps the detail
 * for the log. That is why {@link #getMessage} joins them when there are both, with the minor one
 * in parentheses.
 *
 * <p>The major code is an {@code int} and not an enum because the API is a literal translation of
 * the IETF standard, which defines it that way. A value that is not in the list is described as
 * "failure unspecified": it is what the JDK does and it keeps a new mechanism from breaking the
 * formatting of the message.
 */
public class GSSException extends Exception {

    private static final long serialVersionUID = -2706218945227726672L;

    /** The labels of the channel do not match. */
    public static final int BAD_BINDINGS = 1;

    /** A mechanism that is not there was asked for. */
    public static final int BAD_MECH = 2;

    /** The name does not serve. */
    public static final int BAD_NAME = 3;

    /** The type of name is not supported. */
    public static final int BAD_NAMETYPE = 4;

    /** The status selector does not serve. */
    public static final int BAD_STATUS = 5;

    /** The token did not pass the integrity check. */
    public static final int BAD_MIC = 6;

    /** The context expired. */
    public static final int CONTEXT_EXPIRED = 7;

    /** The credentials expired. */
    public static final int CREDENTIALS_EXPIRED = 8;

    /** The credential is broken. */
    public static final int DEFECTIVE_CREDENTIAL = 9;

    /** The token is broken. */
    public static final int DEFECTIVE_TOKEN = 10;

    /** Unspecified failure. It is also what is answered for any unknown code. */
    public static final int FAILURE = 11;

    /** There is no context, or it was already destroyed. */
    public static final int NO_CONTEXT = 12;

    /** No valid credentials were given. */
    public static final int NO_CRED = 13;

    /** The quality of protection asked for is not supported. */
    public static final int BAD_QOP = 14;

    /** The operation is not authorised. */
    public static final int UNAUTHORIZED = 15;

    /** The operation is not available. */
    public static final int UNAVAILABLE = 16;

    /** Adding a credential element that was already there was asked for. */
    public static final int DUPLICATE_ELEMENT = 17;

    /** The name has elements of several mechanisms. */
    public static final int NAME_NOT_MN = 18;

    /** The token is a duplicate of an earlier one. */
    public static final int DUPLICATE_TOKEN = 19;

    /** The token has already expired. */
    public static final int OLD_TOKEN = 20;

    /** A later token has already been processed. */
    public static final int UNSEQ_TOKEN = 21;

    /** A token that was expected is missing. */
    public static final int GAP_TOKEN = 22;

    /** The texts of the major codes, indexed by the code. */
    private static final String[] MAJOR_TEXT = {
        "Failure unspecified at GSS-API level",
        "Channel binding mismatch",
        "Unsupported mechanism requested",
        "Invalid name provided",
        "Name of unsupported type provided",
        "Invalid input status selector",
        "Token had invalid integrity check",
        "Specified security context expired",
        "Expired credentials detected",
        "Defective credential detected",
        "Defective token detected",
        "Failure unspecified at GSS-API level",
        "Security context init/accept not yet called or context deleted",
        "No valid credentials provided",
        "Unsupported QOP value",
        "Operation unauthorized",
        "Operation unavailable",
        "Duplicate credential element requested",
        "Name contains multi-mechanism elements",
        "The token was a duplicate of an earlier token",
        "The token's validity period has expired",
        "A later token has already been processed",
        "An expected per-message token was not received",
    };

    private final int major;

    private int minor;

    private String minorMessage;

    /** Only with the major code. */
    public GSSException(int majorCode) {
        this.major = majorCode;
        this.minor = 0;
        this.minorMessage = null;
    }

    /**
     * With the two codes.
     *
     * @param minorCode the one of the mechanism underneath
     * @param minorString what that mechanism says
     */
    public GSSException(int majorCode, int minorCode, String minorString) {
        this.major = majorCode;
        this.minor = minorCode;
        this.minorMessage = minorString;
    }

    /** The major code, from the list above. */
    public int getMajor() {
        return this.major;
    }

    /** The one of the mechanism, or 0 if there is none. */
    public int getMinor() {
        return this.minor;
    }

    /** The text of the major code. See the note of the class on the unknown codes. */
    public String getMajorString() {
        if (this.major > 0 && this.major < MAJOR_TEXT.length) {
            return MAJOR_TEXT[this.major];
        }
        return MAJOR_TEXT[0];
    }

    /** What the mechanism said, or null. */
    public String getMinorString() {
        return this.minorMessage;
    }

    /**
     * It sets the code of the mechanism after construction.
     *
     * <p>It exists because the GSS-API layer builds the exception before the mechanism finishes
     * telling its part.
     *
     * <p>It always assigns both things, even with code 0. It is worth knowing because the code is
     * what rules: with 0, {@link #getMessage} does <b>not</b> show the text that was set here, even
     * though {@link #getMinorString} does return it.
     */
    public void setMinor(int minorCode, String message) {
        this.minor = minorCode;
        this.minorMessage = message;
    }

    /** The message, with the prefix of the type. */
    public String toString() {
        return "GSSException: " + getMessage();
    }

    /**
     * The text of the major code and, if there is a minor code, that of the minor one in
     * parentheses.
     *
     * <p>What decides is the minor <b>code</b> and not the text: with a code different from 0 and a
     * null text, {@code "(Mechanism level: null)"} comes out. It is what the JDK does and it has
     * its logic -- a code with no text is a mechanism that failed and could not explain itself, and
     * hiding it would lose the datum that it failed.
     */
    public String getMessage() {
        if (this.minor == 0) {
            return getMajorString();
        }
        return getMajorString() + " (Mechanism level: " + this.minorMessage + ")";
    }
}
