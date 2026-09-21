package javax.management.remote;

import java.security.BasicPermission;

/**
 * KajiLibrary's javax.management.remote.SubjectDelegationPermission -- permission to act on
 * another's behalf.
 *
 * <p>A JMX client authenticated as one may ask that its operations run as another; this was the
 * permission that enabled it, with the delegate's name as the target.
 *
 * <p>Marked for removal together with the whole {@code SecurityManager}: without a security
 * manager there is nobody to check it, so it no longer protects anything. It is kept so that old
 * code compiles.
 *
 * <p>It inherits from {@link BasicPermission}, so it supports wildcards: {@code "*"} allows
 * delegating to anybody and {@code "a.b.*"} to anybody with that prefix.
 */
@Deprecated(since = "25", forRemoval = true)
public final class SubjectDelegationPermission extends BasicPermission {

    private static final long serialVersionUID = 1481618113008682343L;

    /**
     * @param name the target, with optional wildcards
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it is empty
     */
    public SubjectDelegationPermission(String name) {
        super(name);
    }

    /**
     * The same; the actions have to be null or empty.
     *
     * @throws IllegalArgumentException if actions are given
     */
    public SubjectDelegationPermission(String name, String actions) {
        super(name, actions);
    }
}
