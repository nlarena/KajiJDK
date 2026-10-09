package javax.crypto.interfaces;

import javax.crypto.SecretKey;

/**
 * KajiLibrary's javax.crypto.interfaces.PBEKey -- a key derived from a password.
 *
 * <p>It exposes the three pieces of the derivation, and each of the three is here for a different
 * reason:
 *
 * <ul>
 *   <li>{@link #getPassword} -- the only thing the person remembers. It returns {@code char[]} and
 *       not {@code String} <b>on purpose</b>: an array can be overwritten after use, and a string
 *       stays in the literal pool until the collector picks it up, if it ever does. Every call has to
 *       return a fresh copy, so that wiping it does not break the key;
 *   <li>{@link #getSalt} -- what makes the same password give two people different keys. Without
 *       salt, a precomputed table breaks every account in a single pass, and that is why the salt
 *       <b>is not secret</b>: it is stored next to the result;
 *   <li>{@link #getIterationCount} -- how many times the function is repeated. It is the only thing
 *       that makes the derivation expensive, and it is the only defence against someone trying
 *       passwords by brute force. A number from the nineties --a thousand rounds-- protects nothing
 *       today.
 * </ul>
 *
 * <p>That all of this is queryable is uncomfortable from a security point of view and it is
 * necessary: without the salt and the rounds the same key cannot be derived again, and without being
 * able to derive it again nothing can be decrypted.
 */
public interface PBEKey extends SecretKey {

    /**
     * From 2000. It is part of the public API: changing it breaks the deserialization of keys already
     * stored.
     */
    static final long serialVersionUID = -1430015993304333921L;

    /** The password, in a fresh copy. See the class's note on why it is not a String. */
    char[] getPassword();

    /** The salt, or null if it has none. It is not secret. */
    byte[] getSalt();

    /** How many rounds of derivation. See the class's note. */
    int getIterationCount();
}
