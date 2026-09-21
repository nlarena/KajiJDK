package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.Destroyable -- something that keeps a secret and can erase it.
 *
 * <p>Both methods have a default implementation, and the two chosen say "I cannot do that": {@code
 * destroy()} throws and {@code isDestroyed()} returns false. That is on purpose and it is the safe
 * default. A {@code destroy()} that did nothing and an {@code isDestroyed()} that returned true
 * would leave the caller believing the secret is no longer in memory when it is, which is exactly
 * the error this interface exists to avoid.
 *
 * <p>Erasing a secret in Java has a limit worth keeping in mind anyway: if the secret is a {@code
 * String}, there is no way to erase it -- it is immutable and lives until the collector picks it
 * up. That is why passwords are passed as {@code char[]} and not as {@code String}.
 */
public interface Destroyable {

    /**
     * Erases the secret this object keeps.
     *
     * @throws DestroyFailedException if it cannot -- see {@link DestroyFailedException}
     */
    default void destroy() throws DestroyFailedException {
        throw new DestroyFailedException();
    }

    /** Whether the secret was already erased. */
    default boolean isDestroyed() {
        return false;
    }
}
