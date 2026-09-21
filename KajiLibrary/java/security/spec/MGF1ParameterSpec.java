package java.security.spec;

// The parameters of MGF1, the mask generation function of PKCS#1 v2.
//
// MGF1 turns a short seed into as many bytes as needed, concatenating Hash(seed || counter) with
// the counter increasing. It is what OAEP and PSS use to produce the mask they apply to the block,
// and its only parameter is **which** hash.
//
// That the MGF1 hash is configurable separately from the main hash is a classic source of
// interoperability errors: a PSS signature with SHA-256 as digest and SHA-1 as MGF1 is perfectly
// legal and does not verify against a verifier that assumed SHA-256 on both sides. That is why the
// value travels explicitly in `PSSParameterSpec` instead of being taken for granted.
public class MGF1ParameterSpec implements AlgorithmParameterSpec {

    public static final MGF1ParameterSpec SHA1 = new MGF1ParameterSpec("SHA-1");
    public static final MGF1ParameterSpec SHA224 = new MGF1ParameterSpec("SHA-224");
    public static final MGF1ParameterSpec SHA256 = new MGF1ParameterSpec("SHA-256");
    public static final MGF1ParameterSpec SHA384 = new MGF1ParameterSpec("SHA-384");
    public static final MGF1ParameterSpec SHA512 = new MGF1ParameterSpec("SHA-512");
    public static final MGF1ParameterSpec SHA512_224 = new MGF1ParameterSpec("SHA-512/224");
    public static final MGF1ParameterSpec SHA512_256 = new MGF1ParameterSpec("SHA-512/256");
    public static final MGF1ParameterSpec SHA3_224 = new MGF1ParameterSpec("SHA3-224");
    public static final MGF1ParameterSpec SHA3_256 = new MGF1ParameterSpec("SHA3-256");
    public static final MGF1ParameterSpec SHA3_384 = new MGF1ParameterSpec("SHA3-384");
    public static final MGF1ParameterSpec SHA3_512 = new MGF1ParameterSpec("SHA3-512");

    private final String mdName;

    public MGF1ParameterSpec(String mdName) {
        if (mdName == null) {
            throw new NullPointerException("digest algorithm is null");
        }
        this.mdName = mdName;
    }

    // The name of the hash, exactly as it would be passed to `MessageDigest.getInstance`.
    public String getDigestAlgorithm() {
        return this.mdName;
    }

    // The format is part of the observable contract: `PSSParameterSpec.toString()` puts this text
    // inside its own.
    @Override
    public String toString() {
        return "MGF1ParameterSpec[hashAlgorithm=" + this.mdName + "]";
    }
}
