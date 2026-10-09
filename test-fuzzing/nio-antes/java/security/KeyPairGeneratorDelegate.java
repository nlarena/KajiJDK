package java.security;

// The bridge between `KeyPairGenerator` and the provider's SPI, for the same reason as
// `SignatureDelegate`: the public class extends the SPI instead of containing it, so `getInstance`
// needs a concrete subclass that forwards.
final class KeyPairGeneratorDelegate extends KeyPairGenerator {

    private final KeyPairGeneratorSpi spi;

    KeyPairGeneratorDelegate(KeyPairGeneratorSpi spi, String algorithm) {
        super(algorithm);
        this.spi = spi;
    }

    // The two `initialize`s with an explicit source are forwarded; the other two are inherited from
    // `KeyPairGenerator`, which completes them with the default generator and lands here.
    @Override
    public void initialize(int keysize, SecureRandom random) {
        this.spi.initialize(keysize, random);
    }

    @Override
    public void initialize(java.security.spec.AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        this.spi.initialize(params, random);
    }

    @Override
    public KeyPair generateKeyPair() {
        return this.spi.generateKeyPair();
    }
}
