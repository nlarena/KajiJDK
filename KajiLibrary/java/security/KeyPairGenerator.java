package java.security;

// It generates public/private key pairs.
//
// Just like `Signature`, it extends its own SPI instead of containing it: it is a historical oddity
// of the API that allowed a provider to write a direct subclass.
//
// A KajiLibrary subset: **no provider registers this service**, so the three overloads of
// `getInstance` always throw `NoSuchAlgorithmException`. The class is whole all the same, because
// its shape is the one any provider added later has to fulfil.
//
// The four `initialize`s come in pairs: one with an explicit source of randomness and another
// without it. The one that does not receive it uses the default generator --the operating
// system's-- and not an invented source; see `KeyPairGeneratorSpi` for why that distinction is what
// matters most in the whole class.
public abstract class KeyPairGenerator extends KeyPairGeneratorSpi {

    private final String algorithm;

    Provider provider;

    protected KeyPairGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("KeyPairGenerator", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyPairGenerator not available");
    }

    public static KeyPairGenerator getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    public static KeyPairGenerator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("KeyPairGenerator", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static KeyPairGenerator build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof KeyPairGeneratorSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for KeyPairGenerator is not a KeyPairGeneratorSpi: "
                + s.getClassName());
        }
        KeyPairGeneratorDelegate d =
            new KeyPairGeneratorDelegate((KeyPairGeneratorSpi) o, algorithm);
        d.provider = s.getProvider();
        return d;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // The historical alias of `generateKeyPair()`. Both exist because one was added in 1.1 and the
    // other in 1.2, and neither could be taken out.
    public final KeyPair genKeyPair() {
        return this.generateKeyPair();
    }

    /**
     * It configures the key size, with the default generator of randomness.
     *
     * <p>"Default" does not mean "without randomness": it is the operating system's. See
     * {@link SecureRandom}.
     */
    public void initialize(int keysize) {
        this.initialize(keysize, new SecureRandom());
    }

    /**
     * The same, saying where the randomness comes from.
     *
     * <p>The base implementation does nothing, just as in the JDK: whoever gets here is a provider
     * that wrote a subclass of {@code KeyPairGenerator} and decided not to accept configuration,
     * and in that case it generates with its default values.
     */
    @Override
    public void initialize(int keysize, SecureRandom random) {
    }

    /**
     * It configures with concrete parameters -- a curve, a group -- and the default generator.
     *
     * @throws InvalidAlgorithmParameterException if the provider does not understand them
     */
    public void initialize(java.security.spec.AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        this.initialize(params, new SecureRandom());
    }

    /** The same, saying where the randomness comes from. */
    @Override
    public void initialize(java.security.spec.AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        super.initialize(params, random);
    }

    // It generates the pair. With nothing configured, the provider uses its default values.
    //
    // The base implementation returns **null**, which is what the JDK does and has to be replicated
    // even though it looks bad. The reason is that this is never reached: either the provider wrote
    // a subclass of `KeyPairGenerator` that overrides it, or it wrote a `KeyPairGeneratorSpi` and
    // then what runs is the forwarding of `KeyPairGeneratorDelegate`. This body exists only so that
    // the class does not have to declare itself abstract in the method.
    @Override
    public KeyPair generateKeyPair() {
        return null;
    }
}
