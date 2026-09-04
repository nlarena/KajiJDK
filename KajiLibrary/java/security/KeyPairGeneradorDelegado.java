package java.security;

// El puente entre `KeyPairGenerator` y el SPI del proveedor, por el mismo motivo que
// `SignatureDelegada`: la clase publica extiende al SPI en vez de contenerlo, asi que `getInstance`
// necesita una subclase concreta que reenvie.
final class KeyPairGeneradorDelegado extends KeyPairGenerator {

    private final KeyPairGeneratorSpi spi;

    KeyPairGeneradorDelegado(KeyPairGeneratorSpi spi, String algorithm) {
        super(algorithm);
        this.spi = spi;
    }

    // Los dos `initialize` con fuente explicita se reenvian; los otros dos heredan de
    // `KeyPairGenerator`, que los completa con el generador por omision y cae aca.
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
