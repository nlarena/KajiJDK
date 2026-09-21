package java.security;

import java.io.Serializable;

/**
 * KajiLibrary's java.security.SecureRandomSpi -- what a cryptographic generator has to know how to
 * do so that {@link SecureRandom} can use it.
 *
 * <p>It is a contract, not an implementation: three abstract methods and three with a default. The
 * three abstract ones are the minimum any generator knows how to do -- accept a seed, hand over
 * bytes, make a seed -- and the three with a default are of the 2017 extension for DRBGs, which an
 * old generator has no reason to know about.
 *
 * <h2>The difference between nextBytes and generateSeed</h2>
 *
 * <p>Both return bytes and they are not the same thing, and confusing them is the classic mistake:
 *
 * <ul>
 *   <li>{@code engineNextBytes} hands over the <b>output</b> of the generator. It can be as fast as
 *       it likes and comes from expanding the internal state.
 *   <li>{@code engineGenerateSeed} hands over <b>entropy</b>, for seeding another generator. It can
 *       be slow, it can block, and it must not come from expanding anything: seeding a generator
 *       with the output of another adds no entropy, it only spreads it out.
 * </ul>
 *
 * <h2>The defaults are the safe ones</h2>
 *
 * <p>{@code engineReseed} and {@code engineNextBytes(byte[], SecureRandomParameters)} throw
 * {@code UnsupportedOperationException}, and {@code engineGetParameters} returns null. It is on
 * purpose: a generator that is not a DRBG has no state to reseed and no parameters to answer, and
 * saying yes -- returning bytes without really reseeding -- would leave the caller believing they
 * refreshed the state when nothing happened.
 */
public abstract class SecureRandomSpi implements Serializable {

    private static final long serialVersionUID = -2991854161009191830L;

    private final SecureRandomParameters params;

    /** A generator with no parameters: the case of any generator that is not a DRBG. */
    public SecureRandomSpi() {
        this.params = null;
    }

    /**
     * A generator with parameters, which is what a DRBG declares.
     *
     * @throws IllegalArgumentException if the parameters are null
     */
    protected SecureRandomSpi(SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.params = params;
    }

    /**
     * It adds that seed to the state. <b>It adds</b>: it never replaces, so calling it cannot leave
     * the generator more predictable than it was already.
     */
    protected abstract void engineSetSeed(byte[] seed);

    /** It fills the array with the output of the generator. */
    protected abstract void engineNextBytes(byte[] bytes);

    /**
     * The same, with parameters per call. The default does not support it.
     *
     * @throws UnsupportedOperationException if this generator is not a DRBG
     */
    protected void engineNextBytes(byte[] bytes, SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }

    /** It returns `numBytes` bytes of <b>entropy</b>. See the note of the class. */
    protected abstract byte[] engineGenerateSeed(int numBytes);

    /**
     * It reseeds the internal state.
     *
     * @throws UnsupportedOperationException if this generator has no state to reseed
     */
    protected void engineReseed(SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }

    /** The parameters it was created with, or null if it has none. */
    protected SecureRandomParameters engineGetParameters() {
        return this.params;
    }

    @Override
    public String toString() {
        return this.params == null ? super.toString() : this.params.toString();
    }
}
