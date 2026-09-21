package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

// It converts between the two forms of a key: the opaque one (`Key`) and the transparent one
// (`KeySpec`).
//
// It is the piece that makes the two representations of any use. A `Key` can live in a token and
// not let itself be looked at; a `KeySpec` is material the program can build by hand or read from a
// file. `KeyFactory` is the only road between the two, and that is why it is what is used for
// loading a key from its encoding —the most common case of all.
//
// `translateKey` deserves a line apart: it converts a key of **another** provider into one of this
// one. It is what allows a key that arrived from outside to be taken and used with a provider that
// only knows how to work with its own, without exporting it or building it again from bytes.
//
// ===============================================================================================
// THE FACTORY HAS NO PROVIDERS
// ===============================================================================================
//
// Just like `AlgorithmParameters`: `KajiProvider` only offers digests, so the three overloads of
// `getInstance` always throw `NoSuchAlgorithmException`. No `KeyFactory` is registered because
// implementing one honestly asks for a DER parser and the arithmetic of the algorithm, and neither
// of the two things is written. The structure is left ready for the day there is one.
public class KeyFactory {

    private final KeyFactorySpi spi;
    private final Provider provider;
    private final String algorithm;

    protected KeyFactory(KeyFactorySpi keyFacSpi, Provider provider, String algorithm) {
        this.spi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("KeyFactory", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyFactory not available");
    }

    public static KeyFactory getInstance(String algorithm, String provider)
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

    public static KeyFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("KeyFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static KeyFactory build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof KeyFactorySpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for KeyFactory is not a KeyFactorySpi: " + s.getClassName());
        }
        return new KeyFactory((KeyFactorySpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final PublicKey generatePublic(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spi.engineGeneratePublic(keySpec);
    }

    public final PrivateKey generatePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spi.engineGeneratePrivate(keySpec);
    }

    public final <T extends KeySpec> T getKeySpec(Key key, Class<T> keySpec)
            throws InvalidKeySpecException {
        return this.spi.engineGetKeySpec(key, keySpec);
    }

    public final Key translateKey(Key key) throws InvalidKeyException {
        return this.spi.engineTranslateKey(key);
    }
}
