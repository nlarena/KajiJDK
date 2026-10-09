package java.security;

import java.util.Random;

/**
 * KajiLibrary's java.security.SecureRandom -- the generator the keys come out of.
 *
 * <p>It inherits from {@link Random} and that inheritance is historical, not conceptual: the only
 * thing they share is the shape. A {@code Random} is a <b>predictable</b> generator -- the same
 * seed, the same series -- and it is the right thing for a simulation or a game. A
 * {@code SecureRandom} promises the opposite, and using the first where the second is needed is the
 * most common security mistake there is: the code works, the tests pass, and the keys are
 * guessable.
 *
 * <h2>What is behind it here</h2>
 *
 * <p>The stock provider registers a single algorithm, {@code "OS-PRNG"}, which is a direct pass to
 * the generator of the operating system -- {@code BCryptGenRandom} on Windows, {@code /dev/urandom}
 * on the rest. See {@code OsPrngSpi} for why a direct pass was chosen and not a DRBG of our own.
 *
 * <p><b>Noted difference with the JDK</b>: there {@code getInstance("SHA1PRNG")} and
 * {@code getInstance("DRBG")} work. Here they throw {@code NoSuchAlgorithmException}, because this
 * library does not implement those two algorithms and returning another one with that name would be
 * lying about which construction is running. What does work, and is what almost everybody uses, is
 * {@code new SecureRandom()}: it takes the first there is, which here is the system's.
 *
 * <h2>Three details of the contract that are forgotten</h2>
 *
 * <ol>
 *   <li>{@code setSeed} <b>adds</b> entropy, it does not replace it. Two generators with the same
 *       seed do <b>not</b> give the same series, the other way round from {@link Random}. Counting
 *       on that to reproduce a run is a mistake.
 *   <li>{@code generateSeed} is not {@code nextBytes}: the first hands over entropy for seeding
 *       another generator and the second hands over output. See {@link SecureRandomSpi}.
 *   <li>{@code setSeed(long)} with zero <b>does nothing</b>. It has to be that way because the
 *       constructor of {@link Random} calls {@code setSeed} before this object has its generator
 *       built; without that way out, building a {@code SecureRandom} would blow up.
 * </ol>
 */
public class SecureRandom extends Random {

    private static final long serialVersionUID = 4940670005562187L;

    private final SecureRandomSpi secureRandomSpi;
    private final Provider provider;
    private String algorithm;

    /**
     * The first generator any installed provider offers.
     *
     * @throws ProviderException if there is none. The JDK guarantees that there is always one; here
     *     that one is the operating system's, and if the system cannot give entropy there is
     *     nothing reasonable to return.
     */
    public SecureRandom() {
        // `super(0)` and not `super()`: the constructor of Random calls setSeed, which is
        // overridden, and there is no `secureRandomSpi` yet. With zero, the override does nothing.
        super(0);
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = firstSecureRandom(provs[i]);
            if (s != null) {
                try {
                    this.secureRandomSpi = (SecureRandomSpi) s.newInstance(null);
                    this.provider = provs[i];
                    this.algorithm = s.getAlgorithm();
                    return;
                } catch (NoSuchAlgorithmException e) {
                    // A provider that announces a service and cannot build it does not disqualify
                    // the ones that come afterwards.
                    i = i + 1;
                    continue;
                }
            }
            i = i + 1;
        }
        throw new ProviderException("no SecureRandom implementation is installed");
    }

    /**
     * The same, seeded with those bytes.
     *
     * <p>The seed <b>adds</b>: two generators built with the same one do not give the same series.
     */
    public SecureRandom(byte[] seed) {
        this();
        this.secureRandomSpi.engineSetSeed(seed);
    }

    /** The constructor for whoever brings their own implementation. */
    protected SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider) {
        super(0);
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = null;
    }

    private static Provider.Service firstSecureRandom(Provider p) {
        java.util.Iterator<Provider.Service> it = p.getServices().iterator();
        while (it.hasNext()) {
            Provider.Service s = it.next();
            if ("SecureRandom".equals(s.getType())) {
                return s;
            }
        }
        return null;
    }

    /**
     * The generator of that algorithm, of the first provider that offers it.
     *
     * @throws NoSuchAlgorithmException if no provider offers it
     */
    public static SecureRandom getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("SecureRandom", algorithm);
            if (s != null) {
                return build(s, algorithm, provs[i]);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " SecureRandom not available");
    }

    /**
     * The same, demanding that provider.
     *
     * @throws NoSuchProviderException if there is no installed provider with that name
     */
    public static SecureRandom getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    /** The same, with the instance of the provider instead of its name. */
    public static SecureRandom getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider.Service s = provider.getService("SecureRandom", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm, provider);
    }

    /**
     * The generator of that algorithm configured with those parameters.
     *
     * <p>The parameters are only understood by a DRBG. The stock generator of this library is not
     * one, so here this overload always ends in {@code NoSuchAlgorithmException}: the service
     * exists but does not accept parameters, and that is the error that corresponds.
     */
    public static SecureRandom getInstance(String algorithm, SecureRandomParameters params)
            throws NoSuchAlgorithmException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("SecureRandom", algorithm);
            if (s != null) {
                return buildWithParams(s, algorithm, provs[i], params);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " SecureRandom not available");
    }

    /** The same, demanding that provider by name. */
    public static SecureRandom getInstance(String algorithm, SecureRandomParameters params,
            String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, params, p);
    }

    /** The same, with the instance of the provider. */
    public static SecureRandom getInstance(String algorithm, SecureRandomParameters params,
            Provider provider) throws NoSuchAlgorithmException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider.Service s = provider.getService("SecureRandom", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return buildWithParams(s, algorithm, provider, params);
    }

    private static SecureRandom build(Provider.Service s, String algorithm, Provider p)
            throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom((SecureRandomSpi) s.newInstance(null), p);
        sr.algorithm = algorithm;
        return sr;
    }

    private static SecureRandom buildWithParams(Provider.Service s, String algorithm, Provider p,
            SecureRandomParameters params) throws NoSuchAlgorithmException {
        Object o = s.newInstance(params);
        if (!(o instanceof SecureRandomSpi)) {
            throw new NoSuchAlgorithmException(algorithm + " does not accept parameters");
        }
        SecureRandom sr = new SecureRandom((SecureRandomSpi) o, p);
        sr.algorithm = algorithm;
        return sr;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    /** The name of the algorithm, or {@code "unknown"} if it was built with an SPI by hand. */
    public String getAlgorithm() {
        return this.algorithm == null ? "unknown" : this.algorithm;
    }

    @Override
    public String toString() {
        return this.secureRandomSpi.toString();
    }

    /** The parameters it was created with, or null if it has none. */
    public SecureRandomParameters getParameters() {
        return this.secureRandomSpi.engineGetParameters();
    }

    /** It adds that seed. See the note of the class: it adds, it does not replace. */
    public void setSeed(byte[] seed) {
        if (seed == null) {
            throw new NullPointerException("seed is null");
        }
        this.secureRandomSpi.engineSetSeed(seed);
    }

    /**
     * It adds the eight bytes of that integer as a seed.
     *
     * <p>With zero it does nothing, and it is not a whim: the constructor of {@link Random} calls
     * this method before the generator exists. See the note of the class.
     */
    @Override
    public void setSeed(long seed) {
        if (seed != 0) {
            this.secureRandomSpi.engineSetSeed(longToByteArray(seed));
        }
    }

    private static byte[] longToByteArray(long l) {
        byte[] out = new byte[8];
        int i = 0;
        while (i < 8) {
            out[i] = (byte) l;
            l = l >> 8;
            i = i + 1;
        }
        return out;
    }

    /** It fills the array with the output of the generator. */
    @Override
    public void nextBytes(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        this.secureRandomSpi.engineNextBytes(bytes);
    }

    /**
     * The same, with parameters per call.
     *
     * @throws UnsupportedOperationException if the generator is not a DRBG
     */
    public void nextBytes(byte[] bytes, SecureRandomParameters params) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.secureRandomSpi.engineNextBytes(bytes, params);
    }

    /**
     * The `numBits` bits from below, taken from the generator.
     *
     * <p>It is the method {@link Random} calls from {@code nextInt}, {@code nextLong} and company,
     * and that is why overriding it is enough for **all** of them to become cryptographic. It is
     * `final`: a subclass that changed it could return predictable bits without anything else of
     * the class finding out.
     */
    @Override
    protected final int next(int numBits) {
        int numBytes = (numBits + 7) / 8;
        byte[] b = new byte[numBytes];
        int next = 0;
        this.nextBytes(b);
        int i = 0;
        while (i < numBytes) {
            next = (next << 8) + (b[i] & 0xFF);
            i = i + 1;
        }
        return next >>> (numBytes * 8 - numBits);
    }

    /**
     * Bytes of entropy, from the default generator.
     *
     * <p>It is static and therefore does not say which generator they come from: it uses the same
     * one as {@code new SecureRandom()}. For seeding something of one's own the instance one is
     * preferable.
     */
    public static byte[] getSeed(int numBytes) {
        return new SecureRandom().generateSeed(numBytes);
    }

    /** Bytes of <b>entropy</b>, not of output. See the note of {@link SecureRandomSpi}. */
    public byte[] generateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        return this.secureRandomSpi.engineGenerateSeed(numBytes);
    }

    /**
     * The strongest generator there is.
     *
     * <p>In the JDK it is chosen with the security property {@code securerandom.strongAlgorithms}.
     * Here there is no configuration file to read (see {@link Security}), and the only stock
     * generator is the operating system's -- which is precisely the one that property would name
     * --, so it returns the same one as {@code new SecureRandom()}.
     *
     * @throws NoSuchAlgorithmException if there is none installed
     */
    public static SecureRandom getInstanceStrong() throws NoSuchAlgorithmException {
        try {
            return new SecureRandom();
        } catch (ProviderException e) {
            throw new NoSuchAlgorithmException("no strong SecureRandom is available", e);
        }
    }

    /**
     * It reseeds the internal state with new entropy.
     *
     * @throws UnsupportedOperationException if the generator has no internal state to reseed
     *     -- which is the case of the direct pass to the system, see {@code OsPrngSpi}
     */
    public void reseed() {
        this.secureRandomSpi.engineReseed(null);
    }

    /**
     * The same, with parameters.
     *
     * @throws UnsupportedOperationException if the generator is not a DRBG
     */
    public void reseed(SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.secureRandomSpi.engineReseed(params);
    }
}
