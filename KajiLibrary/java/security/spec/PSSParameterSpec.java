package java.security.spec;

// The parameters of RSASSA-PSS (PKCS#1 v2.1): which hash, which MGF, with which parameters, how
// much salt and which trailer byte.
//
// PSS exists because the old PKCS#1 v1.5 padding is deterministic and its security could never be
// proven; PSS is probabilistic —hence the salt— and has a security proof. The price is that there
// are five parameters that **have to match exactly** between signer and verifier, and none of them
// travels inside the signature. A disagreement in any of them does not give a clear error: it gives
// a valid signature that does not verify.
//
// The salt length is the one most often wrong. This class's default is 20 —the size of SHA-1, by
// inheritance— while current practice is to use the size of the chosen digest. With SHA-256 and a
// salt of 20 the signature is legal and does not verify against a verifier that expects 32.
public class PSSParameterSpec implements AlgorithmParameterSpec {

    // The only trailer value PKCS#1 defines: the byte 0xBC at the end of the encoded block.
    public static final int TRAILER_FIELD_BC = 1;

    // The historical values, all SHA-1. Kept as they were because it is the compatibility default,
    // not because it is the recommended choice.
    public static final PSSParameterSpec DEFAULT =
        new PSSParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, 20, TRAILER_FIELD_BC);

    private final String mdName;
    private final String mgfName;
    private final AlgorithmParameterSpec mgfSpec;
    private final int saltLen;
    private final int trailerField;

    public PSSParameterSpec(String mdName, String mgfName, AlgorithmParameterSpec mgfSpec,
                            int saltLen, int trailerField) {
        if (mdName == null) {
            throw new NullPointerException("digest algorithm is null");
        }
        if (mgfName == null) {
            throw new NullPointerException("mask generation function algorithm is null");
        }
        if (saltLen < 0) {
            throw new IllegalArgumentException("negative saltLen value: " + saltLen);
        }
        if (trailerField < 0) {
            throw new IllegalArgumentException("negative trailerField: " + trailerField);
        }
        this.mdName = mdName;
        this.mgfName = mgfName;
        this.mgfSpec = mgfSpec;
        this.saltLen = saltLen;
        this.trailerField = trailerField;
    }

    // Only the salt length, with the rest at the historical SHA-1 values.
    public PSSParameterSpec(int saltLen) {
        if (saltLen < 0) {
            throw new IllegalArgumentException("negative saltLen value: " + saltLen);
        }
        this.mdName = "SHA-1";
        this.mgfName = "MGF1";
        this.mgfSpec = MGF1ParameterSpec.SHA1;
        this.saltLen = saltLen;
        this.trailerField = TRAILER_FIELD_BC;
    }

    public String getDigestAlgorithm() {
        return this.mdName;
    }

    public String getMGFAlgorithm() {
        return this.mgfName;
    }

    // The MGF parameters, or null if none were given.
    public AlgorithmParameterSpec getMGFParameters() {
        return this.mgfSpec;
    }

    public int getSaltLength() {
        return this.saltLen;
    }

    public int getTrailerField() {
        return this.trailerField;
    }

    // The "maskGenAlgorithm" field prints the MGF's **spec** and not its name —it says
    // "MGF1ParameterSpec[hashAlgorithm=SHA-1]" and not "MGF1"— and even says "null" if there is no
    // spec. It is odd and it is what the JDK does: replicated as is, because the `toString` of
    // these classes ends up in logs people compare across implementations.
    @Override
    public String toString() {
        return "PSSParameterSpec[hashAlgorithm=" + this.mdName
            + ", maskGenAlgorithm=" + this.mgfSpec
            + ", saltLength=" + this.saltLen
            + ", trailerField=" + this.trailerField
            + "]";
    }
}
