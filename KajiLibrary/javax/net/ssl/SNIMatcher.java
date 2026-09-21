package javax.net.ssl;

/**
 * The criterion by which a server accepts or rejects an SNI name it was sent.
 *
 * <p>It is a class and not a loose predicate because it carries the <strong>type</strong> it knows
 * how to examine: a matcher is only consulted for names of its own type, and without that datum the
 * server would have to pass everything to everyone.
 *
 * <p>Rejecting is not a configuration detail: if no matcher accepts, the server cuts the handshake.
 * It is the way for a server to serve only the names it really serves.
 */
public abstract class SNIMatcher {

    private final int type;

    /**
     * @throws IllegalArgumentException if the type does not fit in an unsigned byte
     */
    protected SNIMatcher(int type) {
        if (type < 0 || type > 255) {
            throw new IllegalArgumentException("type out of range: " + String.valueOf(type));
        }
        this.type = type;
    }

    /** The type of name this matcher examines. */
    public final int getType() {
        return this.type;
    }

    /** Whether the name is acceptable. */
    public abstract boolean matches(SNIServerName serverName);
}
