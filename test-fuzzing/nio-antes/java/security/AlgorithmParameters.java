package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

// The parameters of an algorithm, in **opaque** form.
//
// The pair with `AlgorithmParameterSpec` is the central idea: the spec is transparent and the
// program reads it field by field; this keeps the same parameters encoded in DER and knows how to
// convert between the two forms. It serves for moving parameters from one place to another without
// understanding them —which is what is needed in order to talk to a counterpart that uses an
// algorithm one does not implement.
//
// The object is a **single shot**: it is built uninitialised, `init` is called once, and from there
// it is read-only. Calling `init` twice throws, and calling `getEncoded` before initialising throws
// too. That is what keeps some parameters from changing after somebody used them to take a
// decision.
//
// ===============================================================================================
// THE FACTORY HAS NO PROVIDERS, AND THAT IS ON PURPOSE
// ===============================================================================================
//
// `getInstance` walks the registered providers looking for a service of type
// "AlgorithmParameters", and there is none: `KajiProvider` only offers digests. That is, today the
// three overloads of `getInstance` always throw `NoSuchAlgorithmException`.
//
// It is the right answer and not a hole: encoding parameters is writing DER specific to each
// algorithm, and an `AlgorithmParameters` that returned bytes that are not the parameters would be
// exactly the kind of member that lies. The structure is left written and working —the life cycle,
// the delegation to the spi, the state errors— for the day there is a provider to register.
public class AlgorithmParameters {

    private final AlgorithmParametersSpi paramSpi;
    private final Provider provider;
    private final String algorithm;

    // Whether `init` has been called already.
    private boolean initialized;

    protected AlgorithmParameters(AlgorithmParametersSpi paramSpi, Provider provider,
                                  String algorithm) {
        this.paramSpi = paramSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameters getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("AlgorithmParameters", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " AlgorithmParameters not available");
    }

    public static AlgorithmParameters getInstance(String algorithm, String provider)
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

    public static AlgorithmParameters getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("AlgorithmParameters", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static AlgorithmParameters build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof AlgorithmParametersSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for AlgorithmParameters is not an AlgorithmParametersSpi: "
                + s.getClassName());
        }
        return new AlgorithmParameters((AlgorithmParametersSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(AlgorithmParameterSpec paramSpec)
            throws InvalidParameterSpecException {
        if (this.initialized) {
            throw new InvalidParameterSpecException("already initialized");
        }
        this.paramSpi.engineInit(paramSpec);
        this.initialized = true;
    }

    public final void init(byte[] params) throws IOException {
        if (this.initialized) {
            throw new IOException("already initialized");
        }
        this.paramSpi.engineInit(params);
        this.initialized = true;
    }

    public final void init(byte[] params, String format) throws IOException {
        if (this.initialized) {
            throw new IOException("already initialized");
        }
        this.paramSpi.engineInit(params, format);
        this.initialized = true;
    }

    // The transparent version of these parameters, of the type asked for.
    public final <T extends AlgorithmParameterSpec> T getParameterSpec(Class<T> paramSpec)
            throws InvalidParameterSpecException {
        if (!this.initialized) {
            throw new InvalidParameterSpecException("not initialized");
        }
        return this.paramSpi.engineGetParameterSpec(paramSpec);
    }

    public final byte[] getEncoded() throws IOException {
        if (!this.initialized) {
            throw new IOException("not initialized");
        }
        return this.paramSpi.engineGetEncoded();
    }

    public final byte[] getEncoded(String format) throws IOException {
        if (!this.initialized) {
            throw new IOException("not initialized");
        }
        return this.paramSpi.engineGetEncoded(format);
    }

    // It returns null if it is not initialised, and not an empty string or "<uninitialized>". It is
    // what the JDK does and there is code that checks it.
    @Override
    public final String toString() {
        if (!this.initialized) {
            return null;
        }
        return this.paramSpi.engineToString();
    }
}
