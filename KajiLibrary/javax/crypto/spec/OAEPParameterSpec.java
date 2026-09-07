package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;

/**
 * OAEP's parameters: which hash, which mask function and which label.
 *
 * <p>The two hashes are independent and that is why they are two fields: the message's and the mask
 * function's can be different, and in practice they often are because the receiver expects a
 * particular combination. Getting either of them wrong makes decryption fail without saying why,
 * which is what is to be expected of a padding that must not leak information.
 *
 * <p>{@link #DEFAULT} is SHA-1 with MGF1-SHA-1 and an empty label, which is what PKCS#1 defines as
 * the default. **It is not a recommendation**: SHA-1 is obsolete and a new application should choose
 * SHA-256 explicitly. It is there because the standard defines it that way and because it is needed
 * to interoperate with what already exists.
 */
public class OAEPParameterSpec implements AlgorithmParameterSpec {

    /** SHA-1, MGF1 with SHA-1 and an empty label. See the class's note. */
    public static final OAEPParameterSpec DEFAULT = new OAEPParameterSpec(
            "SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);

    private final String mdName;
    private final String mgfName;
    private final AlgorithmParameterSpec mgfSpec;
    private final PSource pSrc;

    /**
     * @throws NullPointerException if either of the two names or the source are null --`mgfSpec` may
     *     be null, and it means the mask function takes no parameters--
     */
    public OAEPParameterSpec(String mdName, String mgfName, AlgorithmParameterSpec mgfSpec,
            PSource pSrc) {
        if (mdName == null) {
            throw new NullPointerException("the digest algorithm cannot be null");
        }
        if (mgfName == null) {
            throw new NullPointerException("the mask algorithm cannot be null");
        }
        if (pSrc == null) {
            throw new NullPointerException("the label's source cannot be null");
        }
        this.mdName = mdName;
        this.mgfName = mgfName;
        this.mgfSpec = mgfSpec;
        this.pSrc = pSrc;
    }

    /** The message's hash. */
    public String getDigestAlgorithm() {
        return this.mdName;
    }

    /** The mask generation function. */
    public String getMGFAlgorithm() {
        return this.mgfName;
    }

    /** The mask function's parameters, or null if it takes none. */
    public AlgorithmParameterSpec getMGFParameters() {
        return this.mgfSpec;
    }

    /** Where the label comes from. */
    public PSource getPSource() {
        return this.pSrc;
    }
}
