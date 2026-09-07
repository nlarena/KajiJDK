package javax.crypto.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.DHParameterSpec;

/**
 * KajiLibrary's javax.crypto.interfaces.DHPrivateKey -- the private half of a Diffie-Hellman.
 *
 * <p>The value is {@link #getX}, the private exponent. It is the only number in the exchange that
 * does not leave the machine: everything else --the modulus, the base, the other side's public key--
 * travels in the clear and is worth nothing without this one.
 *
 * <p>That the interface <b>exposes</b> it is worth a look. A key living in a token or a hardware
 * module cannot answer {@code getX}, and that is why that kind of key implements {@code PrivateKey}
 * but not this interface. Asking for a {@code DHPrivateKey} is, in fact, asking for the secret to be
 * in memory.
 *
 * <h2>Why {@code getParams} has a default returning null</h2>
 *
 * <p>{@link DHKey} declares {@code getParams()} returning {@link DHParameterSpec}, and
 * {@code AsymmetricKey} brings another one with a default returning {@link AlgorithmParameterSpec}.
 * The two have the same signature --the first is a covariant override of the second-- and Java
 * requires an interface inheriting from both branches to <b>resolve the tie</b> by hand.
 *
 * <p>This default is that resolution, and it returns null because it has nowhere to take anything
 * from: an interface has no state. It is the same default as the JDK's, and it means what it says
 * --"this key does not publish its parameters"-- so whoever implements it for real has to override
 * it.
 */
public interface DHPrivateKey extends DHKey, PrivateKey {

    /**
     * From 1998, when serialization crossed versions by hand.
     *
     * <p>It is part of the public API and cannot be changed: changing it breaks the deserialization
     * of any key stored with the previous version.
     */
    static final long serialVersionUID = 2211791113380396553L;

    /** The private exponent. See the class's note. */
    BigInteger getX();

    /** Resolves the tie between the two branches; see the class's note. */
    default DHParameterSpec getParams() {
        return null;
    }
}
