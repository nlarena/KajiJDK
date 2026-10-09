package javax.naming.ldap;

import java.io.Serializable;

/**
 * A modifier that travels attached to an LDAP operation.
 *
 * <h2>What the mechanism is for</h2>
 *
 * <p>LDAP has few operations --search, add, modify, delete-- and one way to extend them without
 * changing the protocol: <em>controls</em>. Each one is identified by an OID and carries its data
 * BER-encoded; a server that understands it changes its behaviour, and one that does not looks at
 * it and carries on.
 *
 * <p>That is how paging ({@link PagedResultsControl}) or sorting ({@link SortControl}) are asked
 * for without there being "paged search" and "sorted search" operations.
 *
 * <h2>{@link #isCritical}, which is what matters</h2>
 *
 * <p>A critical control the server does not understand makes the operation <strong>fail</strong>;
 * a non-critical one is silently ignored. The choice is not one of style: asking for non-critical
 * sorting and getting unsorted results without noticing is worse than an error.
 */
public interface Control extends Serializable {

    /** The operation fails if the server does not understand the control. */
    boolean CRITICAL = true;

    /** The server ignores it if it does not understand it. */
    boolean NONCRITICAL = false;

    /** The OID that identifies the control. */
    String getID();

    /** Whether it is critical; see the class note. */
    boolean isCritical();

    /** The control's data, BER-encoded, or {@code null} if it carries none. */
    byte[] getEncodedValue();
}
