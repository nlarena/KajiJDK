package javax.crypto.interfaces;

import javax.crypto.spec.DHParameterSpec;

/**
 * KajiLibrary's javax.crypto.interfaces.DHKey -- what the two halves of a Diffie-Hellman key share.
 *
 * <p>A single method, and what it returns is the part of the agreement that <b>is not secret and has
 * to match</b>: the prime modulus and the base. Without the same parameters on both sides there is no
 * shared secret, so this travels in the clear along with the public key.
 *
 * <p>That the parameters are public does not mean they are all the same. Choosing a small modulus, or
 * one that is not a safe prime, leaves the exchange within reach of a precomputed discrete-logarithm
 * attack -- which is exactly what brought down the 1024-bit primes that were used from memory
 * everywhere.
 */
public interface DHKey {

    /** The modulus, the base and the length of the private exponent. */
    DHParameterSpec getParams();
}
