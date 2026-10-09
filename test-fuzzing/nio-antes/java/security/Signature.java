package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

// Digital signature: producing it and verifying it.
//
// ===============================================================================================
// THE CONTRACT THAT HAS TO BE READ TWICE
// ===============================================================================================
//
// `verify()` **returns a boolean**, the other way round from `Certificate.verify()`, which returns
// nothing and throws if it fails. The difference is deliberate and it is where the mistakes are
// made: here, an invalid signature **throws no exception**, it returns `false`. Code that does
//
//     s.initVerify(k); s.update(data); s.verify(signature);
//
// without looking at the result accepts any signature. The exception is reserved for "the object
// was not initialised" or "the provider broke", which are errors of the program, not of the
// attacker.
//
// The other point is the order: one has to initialise **before** feeding data, and each `sign()` or
// `verify()` leaves the object ready for another operation with the same key. Feeding data without
// initialising throws `SignatureException`, which is right: signing with undefined state would give
// a signature with no meaning.
//
// ===============================================================================================
// WHY THERE IS NO ALGORITHM, AND WHY THERE IS NO SecureRandom
// ===============================================================================================
//
// **There is no registered `Signature` provider**, so the three overloads of `getInstance` always
// throw `NoSuchAlgorithmException`. It is the central decision of this whole package: a
// `Signature.verify()` that returns true without verifying is not technical debt, it is a hole, and
// the only way of not having it is not offering the algorithm. The whole structure is there
// —`SignatureSpi` is the complete interface— so a provider that knows RSA or ECDSA plugs in and
// everything else works.
//
// `initSign(PrivateKey, SecureRandom)` is there, and the source it receives matters more than it
// seems: it is where the nonce of DSA and ECDSA comes from. See `SignatureSpi` for what happens if
// that nonce repeats.
public abstract class Signature extends SignatureSpi {

    // It has not been said yet whether it signs or verifies.
    protected static final int UNINITIALIZED = 0;

    // Ready to sign.
    protected static final int SIGN = 2;

    // Ready to verify.
    protected static final int VERIFY = 3;

    // Which of the three states it is in. It is `protected` and not private because the direct
    // subclasses —the ones a provider writes without going through a separate SPI— look at it.
    protected int state = UNINITIALIZED;

    private final String algorithm;

    // The provider is only had by the instance that comes out of `getInstance`; a subclass written
    // by hand has none.
    Provider provider;

    protected Signature(String algorithm) {
        this.algorithm = algorithm;
    }

    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("Signature", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " Signature not available");
    }

    public static Signature getInstance(String algorithm, String provider)
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

    public static Signature getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("Signature", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static Signature build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof SignatureSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for Signature is not a SignatureSpi: " + s.getClassName());
        }
        SignatureDelegate d = new SignatureDelegate((SignatureSpi) o, algorithm);
        d.provider = s.getProvider();
        return d;
    }

    // The provider that resolved the algorithm, or null if this instance did not come out of
    // `getInstance`.
    public final Provider getProvider() {
        return this.provider;
    }

    // It prepares to verify with this public key. It discards any data that had been fed before.
    public final void initVerify(PublicKey publicKey) throws InvalidKeyException {
        this.engineInitVerify(publicKey);
        this.state = VERIFY;
    }

    // It prepares to verify with the key a certificate brings.
    //
    // It is not only a shortcut: it checks the KeyUsage extension before accepting the key. If the
    // certificate says that its key does not serve for signing —bit 0, digitalSignature, off— it is
    // rejected. Without that, a certificate issued for encrypting could be used for validating
    // signatures, which is exactly what KeyUsage exists to prevent.
    //
    // The detail that matters, and that is easy to read the wrong way round: **it is only looked at
    // if the extension comes marked as critical**. It is what the JDK does and it is coherent with
    // the model of X.509 —a non-critical extension is a recommendation whoever does not understand
    // it can ignore— but it lets through certificates with a non-critical KeyUsage that says no.
    // Whoever needs the strict rule has to look at `getKeyUsage()` themselves.
    public final void initVerify(java.security.cert.Certificate certificate)
            throws InvalidKeyException {
        if (certificate instanceof java.security.cert.X509Certificate) {
            java.security.cert.X509Certificate cert =
                (java.security.cert.X509Certificate) certificate;
            java.util.Set<String> criticas = cert.getCriticalExtensionOIDs();
            if (criticas != null && !criticas.isEmpty() && criticas.contains("2.5.29.15")) {
                boolean[] uses = cert.getKeyUsage();
                if (uses != null && !uses[0]) {
                    throw new InvalidKeyException("Wrong key usage");
                }
            }
        }
        PublicKey publicKey = certificate.getPublicKey();
        this.engineInitVerify(publicKey);
        this.state = VERIFY;
    }

    // It prepares to sign with this private key.
    public final void initSign(PrivateKey privateKey) throws InvalidKeyException {
        this.engineInitSign(privateKey);
        this.state = SIGN;
    }

    /**
     * The same, saying where the randomness comes from.
     *
     * <p>The source is the one of the nonce per signature. See {@link SignatureSpi} for why a
     * repeated nonce in ECDSA reveals the private key.
     */
    public final void initSign(PrivateKey privateKey, SecureRandom random)
            throws InvalidKeyException {
        this.engineInitSign(privateKey, random);
        this.state = SIGN;
    }

    // It closes the operation and returns the signature. The object is left ready to sign again
    // with the same key.
    public final byte[] sign() throws SignatureException {
        if (this.state == SIGN) {
            return this.engineSign();
        }
        throw new SignatureException("object not initialized for signing");
    }

    // The same, leaving the signature in a buffer. It returns how many bytes it took.
    public final int sign(byte[] outbuf, int offset, int len) throws SignatureException {
        if (this.state != SIGN) {
            throw new SignatureException("object not initialized for signing");
        }
        if (outbuf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("offset or len is less than 0");
        }
        if (outbuf.length - offset < len) {
            throw new IllegalArgumentException(
                "Output buffer too small for specified offset and length");
        }
        return this.engineSign(outbuf, offset, len);
    }

    // It verifies. **It returns false if the signature is not valid; it does not throw.** See the
    // note of the class.
    public final boolean verify(byte[] signature) throws SignatureException {
        if (this.state == VERIFY) {
            return this.engineVerify(signature);
        }
        throw new SignatureException("object not initialized for verification");
    }

    public final boolean verify(byte[] signature, int offset, int length)
            throws SignatureException {
        if (this.state != VERIFY) {
            throw new SignatureException("object not initialized for verification");
        }
        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset or length is less than 0");
        }
        if (signature.length - offset < length) {
            throw new IllegalArgumentException(
                "signature too small for specified offset and length");
        }
        return this.engineVerify(signature, offset, length);
    }

    public final void update(byte b) throws SignatureException {
        if (this.state == VERIFY || this.state == SIGN) {
            this.engineUpdate(b);
        } else {
            throw new SignatureException("object not initialized for signature or verification");
        }
    }

    public final void update(byte[] data) throws SignatureException {
        if (this.state == SIGN || this.state == VERIFY) {
            if (data == null) {
                throw new IllegalArgumentException("data is null");
            }
            this.engineUpdate(data, 0, data.length);
        } else {
            throw new SignatureException("object not initialized for signature or verification");
        }
    }

    public final void update(byte[] data, int off, int len) throws SignatureException {
        if (this.state == SIGN || this.state == VERIFY) {
            if (data == null) {
                throw new IllegalArgumentException("data is null");
            }
            if (off < 0 || len < 0) {
                throw new IllegalArgumentException("off or len is less than 0");
            }
            if (data.length - off < len) {
                throw new IllegalArgumentException(
                    "data too small for specified offset and length");
            }
            this.engineUpdate(data, off, len);
        } else {
            throw new SignatureException("object not initialized for signature or verification");
        }
    }

    // It feeds from a buffer. It consumes it whole: on returning, `position` was left at `limit`.
    public final void update(ByteBuffer data) throws SignatureException {
        if (this.state != SIGN && this.state != VERIFY) {
            throw new SignatureException("object not initialized for signature or verification");
        }
        if (data == null) {
            throw new NullPointerException();
        }
        this.engineUpdate(data);
    }

    // The name of the algorithm: "SHA256withRSA".
    public final String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String toString() {
        String label = "";
        if (this.state == UNINITIALIZED) {
            label = "<not initialized>";
        } else if (this.state == VERIFY) {
            label = "<initialized for verifying>";
        } else if (this.state == SIGN) {
            label = "<initialized for signing>";
        }
        return "Signature object: " + this.getAlgorithm() + label;
    }

    // Parameters by name. Discouraged since JDK 1.2: the names were never standardised, so the same
    // string meant different things depending on the provider.
    public final void setParameter(String param, Object value) throws InvalidParameterException {
        this.engineSetParameter(param, value);
    }

    // The good way of passing parameters. For RSASSA-PSS it is not optional: without
    // `PSSParameterSpec`, the hash and the length of the salt are left to the provider's criterion
    // and the signature does not verify on the other side.
    public final void setParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        this.engineSetParameter(params);
    }

    // The effective parameters, including the ones the provider chose by itself. It serves for
    // finding out what was really used and being able to repeat it on the other side.
    public final AlgorithmParameters getParameters() {
        return this.engineGetParameters();
    }

    public final Object getParameter(String param) throws InvalidParameterException {
        return this.engineGetParameter(param);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
