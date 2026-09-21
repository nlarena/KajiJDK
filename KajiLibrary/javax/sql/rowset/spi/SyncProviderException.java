package javax.sql.rowset.spi;

import java.sql.SQLException;

/**
 * The synchronization failed, and inside comes <strong>what</strong> failed.
 *
 * <h2>Why this exception carries data</h2>
 *
 * <p>Because a synchronization failure is almost never total. Of two hundred modified rows, one
 * hundred and ninety-eight were written fine and two clashed with somebody else's changes. An
 * exception with a message would force redoing everything; the {@link SyncResolver} that comes
 * inside allows resolving <strong>only those two</strong> and trying again.
 *
 * <p>It is the reason {@code acceptChanges} throws this and not an ordinary {@code SQLException}:
 * the caller needs the rows, not a message.
 *
 * @since 1.5
 */
public class SyncProviderException extends SQLException {

    private static final long serialVersionUID = -3985215347103826532L;

    private SyncResolver syncResolver;

    /** Without detail and without a resolver. */
    public SyncProviderException() {
        super();
    }

    /**
     * With a message.
     *
     * @param msg the message
     */
    public SyncProviderException(String msg) {
        super(msg);
    }

    /**
     * With the resolver that carries the rows in conflict.
     *
     * @param syncResolver the resolver
     * @throws IllegalArgumentException if it is {@code null}
     */
    public SyncProviderException(SyncResolver syncResolver) {
        super();
        if (syncResolver == null) {
            throw new IllegalArgumentException("the SyncResolver cannot be null");
        }
        this.syncResolver = syncResolver;
    }

    /**
     * The resolver with the rows in conflict.
     *
     * @return the resolver, or {@code null} if this exception carries none
     */
    public SyncResolver getSyncResolver() {
        return syncResolver;
    }

    /**
     * Sets the resolver.
     *
     * @param syncResolver the resolver
     * @throws IllegalArgumentException if it is {@code null}
     */
    public void setSyncResolver(SyncResolver syncResolver) {
        if (syncResolver == null) {
            throw new IllegalArgumentException("the SyncResolver cannot be null");
        }
        this.syncResolver = syncResolver;
    }
}
