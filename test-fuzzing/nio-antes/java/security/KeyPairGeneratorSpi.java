package java.security;

// What a provider has to write in order to generate key pairs.
//
// Both `initialize`s receive the source of randomness, and the API does not allow generating
// without it. The reason is worth saying whole: **generating a key pair is exactly the operation
// that depends most on randomness**. An RSA key comes from two primes chosen at random; an EC key,
// from a scalar at random. With a predictable generator, the keys are predictable, and a
// predictable private key protects nothing. That is why the one that receives the source is
// **abstract** and the one that receives parameters has a default that **rejects**: a provider that
// does not know how to handle parameters has to say so, not ignore them and generate something
// else.
public abstract class KeyPairGeneratorSpi {

    public KeyPairGeneratorSpi() {
    }

    /**
     * It initialises by key size.
     *
     * @param random where the randomness comes from. See the note of the class: it is not a detail
     */
    public abstract void initialize(int keysize, SecureRandom random);

    /**
     * It initialises with concrete parameters -- a curve, a group -- when the size is not enough.
     *
     * <p>The default <b>rejects</b>. A provider that does not understand the parameters has to say
     * so: ignoring them and generating with its own would give a key that is not the one asked for,
     * and whoever receives it has no way of noticing.
     *
     * @throws InvalidAlgorithmParameterException always, unless the provider overrides it
     */
    public void initialize(java.security.spec.AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }

    // It generates the pair. It can be called several times and each one gives a different pair.
    public abstract KeyPair generateKeyPair();
}
