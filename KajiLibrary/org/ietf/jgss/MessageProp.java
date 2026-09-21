package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.MessageProp -- what protection is asked for, and what was obtained.
 *
 * <p>The same object travels in both directions, and there lies what one has to understand: when
 * <b>sending</b>, the caller fills it in to ask --quality of protection and whether it wants
 * encryption--; when <b>receiving</b>, the implementation fills it in to tell what really happened.
 *
 * <p>Reusing one object for both things saves a class and has a practical consequence: a
 * {@code MessageProp} that is passed to {@code unwrap} comes back <b>modified</b>, and reusing it
 * afterwards for sending drags along whatever was left inside.
 *
 * <h2>The four supplementary states</h2>
 *
 * <p>{@link #isDuplicateToken}, {@link #isOldToken}, {@link #isUnseqToken} and {@link #isGapToken}
 * are warnings about the <b>order</b> of the messages, not about their contents. And they are
 * warnings and not errors on purpose: the message was decrypted fine and is authentic, what happens
 * is that it arrived twice, or late, or before another one. What to do with that depends on the
 * application --over UDP a disorder is normal, over a session it is a replay attack-- and that is
 * why the library reports instead of deciding.
 *
 * <p>Whoever does not look at them loses the whole detection of replay, which is the classic
 * mistake of this API.
 */
public class MessageProp {

    private int qop;

    private boolean privacyState;

    private boolean duplicate = false;

    private boolean old = false;

    private boolean unseq = false;

    private boolean gap = false;

    private int minorStatus = 0;

    private String minorString = null;

    /**
     * With the default quality.
     *
     * @param privState whether encryption is asked for besides integrity
     */
    public MessageProp(boolean privState) {
        this(0, privState);
    }

    /**
     * @param qop the quality of protection; 0 is the default of the mechanism
     * @param privState whether encryption is asked for besides integrity
     */
    public MessageProp(int qop, boolean privState) {
        this.qop = qop;
        this.privacyState = privState;
    }

    /** The quality of protection. */
    public int getQOP() {
        return this.qop;
    }

    /** Whether there is encryption and not only integrity. */
    public boolean getPrivacy() {
        return this.privacyState;
    }

    /** Ver {@link #getQOP}. */
    public void setQOP(int qop) {
        this.qop = qop;
    }

    /** Ver {@link #getPrivacy}. */
    public void setPrivacy(boolean privState) {
        this.privacyState = privState;
    }

    /** The token had already been received. See the note of the class. */
    public boolean isDuplicateToken() {
        return this.duplicate;
    }

    /** The token is too old to know whether it is a duplicate. */
    public boolean isOldToken() {
        return this.old;
    }

    /** It arrived after a later one. */
    public boolean isUnseqToken() {
        return this.unseq;
    }

    /** At least one earlier token is missing. */
    public boolean isGapToken() {
        return this.gap;
    }

    /** The code of the mechanism, or 0. */
    public int getMinorStatus() {
        return this.minorStatus;
    }

    /** What the mechanism said, or null. */
    public String getMinorString() {
        return this.minorString;
    }

    /**
     * The four states at once.
     *
     * <p>It is the implementation that calls it, not whoever uses the API.
     */
    public void setSupplementaryStates(boolean duplicate, boolean old, boolean unseq, boolean gap,
                                       int minorStatus, String minorString) {
        this.duplicate = duplicate;
        this.old = old;
        this.unseq = unseq;
        this.gap = gap;
        this.minorStatus = minorStatus;
        this.minorString = minorString;
    }
}
