package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Derives keys from other material.
 *
 * <h2>Why deriving is necessary</h2>
 *
 * <p>Because what one has is nearly never usable as a key as it is. The secret that comes out of an
 * agreement is not uniformly distributed --some values are likelier than others--; a password has
 * very little entropy; and from a single secret several different keys are usually needed, one per
 * direction and another to authenticate. Deriving turns one thing into the other, and it also makes
 * the derived keys unrelatable to each other: having one does not help in finding the rest.
 *
 * <h2>The two sets of parameters</h2>
 *
 * <p>The function's go in {@link #getInstance(String, KDFParameters)} and are fixed once; each
 * derivation's go in {@link #deriveKey} and change on every call. Separating them is what allows the
 * function to be built once and many different keys derived from it.
 *
 * <h2>{@link #deriveData}</h2>
 *
 * <p>It returns bytes instead of a key, for what is not a key: an initialization vector, a salt, an
 * identifier. It comes from the same place and with the same guarantees.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers derivation functions, so
 * {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. Registering a provider
 * of one's own makes it work.
 *
 * @since 24
 */
public final class KDF {

    private final KDFSpi spi;
    private final Provider provider;
    private final String algorithm;

    private KDF(KDFSpi spi, Provider provider, String algorithm) {
        this.spi = spi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * The name it was asked for by.
     *
     * @return the algorithm
     */
    public String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Whose the implementation is.
     *
     * @return the provider's name
     */
    public String getProviderName() {
        return this.provider.getName();
    }

    /**
     * The parameters it was built with.
     *
     * @return the parameters, or {@code null} if it was given none
     */
    public KDFParameters getParameters() {
        return this.spi.engineGetParameters();
    }

    /**
     * One for that algorithm.
     *
     * @param algorithm the algorithm
     * @return the function
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws NullPointerException if the algorithm is {@code null}
     */
    public static KDF getInstance(String algorithm) throws NoSuchAlgorithmException {
        try {
            return withParameters(algorithm, null, null, null);
        } catch (InvalidAlgorithmParameterException e) {
            // With no parameters there are no parameters to refuse.
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * One from that provider, named.
     *
     * @param algorithm the algorithm
     * @param provider the provider's name
     * @return the function
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     */
    public static KDF getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        try {
            return getInstance(algorithm, null, provider);
        } catch (InvalidAlgorithmParameterException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * One from that provider.
     *
     * @param algorithm the algorithm
     * @param provider the provider
     * @return the function
     * @throws NoSuchAlgorithmException if that provider does not have it
     */
    public static KDF getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        try {
            return getInstance(algorithm, null, provider);
        } catch (InvalidAlgorithmParameterException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * One for that algorithm, with those parameters.
     *
     * @param algorithm the algorithm
     * @param kdfParameters the function's parameters, or {@code null}
     * @return the function
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public static KDF getInstance(String algorithm, KDFParameters kdfParameters)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return withParameters(algorithm, kdfParameters, null, null);
    }

    /**
     * One from that provider, named, with those parameters.
     *
     * @param algorithm the algorithm
     * @param kdfParameters the function's parameters, or {@code null}
     * @param provider the provider's name
     * @return the function
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public static KDF getInstance(String algorithm, KDFParameters kdfParameters, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        final Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, kdfParameters, p);
    }

    /**
     * One from that provider, with those parameters.
     *
     * @param algorithm the algorithm
     * @param kdfParameters the function's parameters, or {@code null}
     * @param provider the provider
     * @return the function
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public static KDF getInstance(String algorithm, KDFParameters kdfParameters, Provider provider)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        return withParameters(algorithm, kdfParameters, provider, null);
    }

    /**
     * Derives a key.
     *
     * @param alg which algorithm the key is for
     * @param derivationSpec this derivation's parameters
     * @return the key
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws NoSuchAlgorithmException if there is no way to assemble a key of that algorithm
     * @throws NullPointerException if the algorithm is {@code null}
     */
    public SecretKey deriveKey(String alg, AlgorithmParameterSpec derivationSpec)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        if (alg == null) {
            throw new NullPointerException("the algorithm must not be null");
        }
        return this.spi.engineDeriveKey(alg, derivationSpec);
    }

    /**
     * Derives bytes.
     *
     * @param derivationSpec this derivation's parameters
     * @return the bytes
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public byte[] deriveData(AlgorithmParameterSpec derivationSpec)
            throws InvalidAlgorithmParameterException {
        return this.spi.engineDeriveData(derivationSpec);
    }

    /**
     * Looks the service up and builds the function.
     *
     * <p>The function's parameters travel as the service's construction parameter, which is what
     * that argument of {@link Provider.Service#newInstance} exists for.
     */
    private static KDF withParameters(String algorithm, KDFParameters params, Provider only,
            String unused)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = only == null
                ? Security.getProviders() : new Provider[] {only};
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("KDF", algorithm);
            if (s != null) {
                final Object o = s.newInstance(params);
                if (!(o instanceof KDFSpi)) {
                    throw new NoSuchAlgorithmException(
                            "class configured for KDF is not a KDFSpi: " + s.getClassName());
                }
                return new KDF((KDFSpi) o, s.getProvider(), algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KDF not available");
    }
}
