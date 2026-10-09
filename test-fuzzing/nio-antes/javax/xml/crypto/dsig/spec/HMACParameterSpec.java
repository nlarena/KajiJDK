package javax.xml.crypto.dsig.spec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.HMACParameterSpec -- how many bits of the HMAC are kept.
 *
 * <p>A single number: the length, in bits, the HMAC output is <b>truncated</b> to.
 *
 * <p>Truncating is allowed by the specification and is a bad idea almost always. An HMAC-SHA1
 * truncated to 80 bits is what the XML-DSig specification gives as an example, and today that is
 * within reach of brute force; the current recommendation is not to truncate below half of the
 * output, and in practice not to truncate.
 *
 * <p>The class does not validate the value: it accepts any integer. It is the implementation that
 * rejects the ones it cannot handle, and that is why an absurd length is discovered when signing
 * and not when building this.
 */
public final class HMACParameterSpec implements SignatureMethodParameterSpec {

    /** The length in bits. */
    private final int outputLength;

    /**
     * @param outputLength how many bits to truncate to; see the class note
     */
    public HMACParameterSpec(int outputLength) {
        this.outputLength = outputLength;
    }

    /** The length in bits. */
    public int getOutputLength() {
        return this.outputLength;
    }
}
