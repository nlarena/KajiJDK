package javax.crypto.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;
import javax.crypto.spec.DHParameterSpec;

/**
 * KajiLibrary's javax.crypto.interfaces.DHPublicKey -- the public half of a Diffie-Hellman.
 *
 * <p>The value is {@link #getY}, which is the base raised to the private exponent, modulo the prime.
 * It is sent in the clear: whoever intercepts it cannot get the exponent back without solving a
 * discrete logarithm, and that is the whole trick.
 *
 * <p>What this number does <b>not</b> do is prove who the other side is. A raw Diffie-Hellman
 * authenticates nobody: two parties end up with the same secret, and if someone got in the middle,
 * with two different secrets and both parties happy. That is why in practice it always goes signed by
 * something --a certificate, a known key-- and using it bare is the classic mistake.
 *
 * <p>Its {@code getParams} resolves the same tie as in {@link DHPrivateKey}; see the note over there.
 */
public interface DHPublicKey extends DHKey, PublicKey {

    /**
     * From 1998. It is part of the public API: changing it breaks the deserialization of keys already
     * stored.
     */
    static final long serialVersionUID = -6628103563352519193L;

    /** The base raised to the private exponent, modulo the prime. See the class's note. */
    BigInteger getY();

    /** Resolves the tie between the two branches; see {@link DHPrivateKey#getParams}. */
    default DHParameterSpec getParams() {
        return null;
    }
}
