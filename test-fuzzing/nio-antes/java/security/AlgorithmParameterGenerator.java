package java.security;

// It generates algorithm parameters: the primes of DSA, a curve made to measure.
//
// Unlike `AlgorithmParameters`, which decodes parameters that exist already, this one produces
// them. In practice it is hardly used: serious domain parameters come from standards, and
// generating one's own is an expensive way of ending up with parameters worse than the published
// ones.
//
// A KajiLibrary subset: **no provider registers this service**, so the three overloads of
// `getInstance` always throw `NoSuchAlgorithmException`. The class is whole all the same, because
// its shape is the one any provider added later has to fulfil.
//
// The four `init`s come in pairs: one with an explicit source of randomness and another without it.
// The one that does not receive it **does not generate without randomness** -- it uses the default
// generator, which is the operating system's. See `SecureRandom`.
public class AlgorithmParameterGenerator {

    private final AlgorithmParameterGeneratorSpi paramGenSpi;
    private final Provider provider;
    private final String algorithm;

    protected AlgorithmParameterGenerator(AlgorithmParameterGeneratorSpi paramGenSpi,
                                          Provider provider, String algorithm) {
        this.paramGenSpi = paramGenSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("AlgorithmParameterGenerator", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(
            algorithm + " AlgorithmParameterGenerator not available");
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm, String provider)
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

    public static AlgorithmParameterGenerator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("AlgorithmParameterGenerator", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static AlgorithmParameterGenerator build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof AlgorithmParameterGeneratorSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for AlgorithmParameterGenerator is not an "
                + "AlgorithmParameterGeneratorSpi: " + s.getClassName());
        }
        return new AlgorithmParameterGenerator(
            (AlgorithmParameterGeneratorSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Initialises by size, with the default generator of randomness.
     *
     * <p>"Default" does not mean "without randomness": it is the operating system's. See
     * {@link SecureRandom}.
     */
    public final void init(int size) {
        this.paramGenSpi.engineInit(size, new SecureRandom());
    }

    /** The same, saying where the randomness comes from. */
    public final void init(int size, SecureRandom random) {
        this.paramGenSpi.engineInit(size, random);
    }

    /**
     * Initialises with concrete parameters and the default generator.
     *
     * @throws InvalidAlgorithmParameterException if the parameters do not serve this generator
     */
    public final void init(java.security.spec.AlgorithmParameterSpec genParamSpec)
            throws InvalidAlgorithmParameterException {
        this.paramGenSpi.engineInit(genParamSpec, new SecureRandom());
    }

    /** The same, saying where the randomness comes from. */
    public final void init(java.security.spec.AlgorithmParameterSpec genParamSpec,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        this.paramGenSpi.engineInit(genParamSpec, random);
    }

    // The generated parameters.
    public final AlgorithmParameters generateParameters() {
        return this.paramGenSpi.engineGenerateParameters();
    }
}
