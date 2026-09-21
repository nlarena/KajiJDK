package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.Refreshable -- a credential that expires and can be renewed.
 *
 * <p>Both methods are abstract, unlike {@link Destroyable}, which has them by default. The reason
 * is that here there is no safe default: {@code isCurrent()} would have to say false -- "I do not
 * know whether it is still valid" -- and then {@code refresh()} would be called always, or say true
 * and lie. Whoever implements this interface does so because they know when their credential
 * expires; whoever does not know does not implement it.
 */
public interface Refreshable {

    /** Whether the credential is still valid. */
    boolean isCurrent();

    /**
     * Renews the credential.
     *
     * @throws RefreshFailedException if it could not -- the old credential may keep serving
     */
    void refresh() throws RefreshFailedException;
}
