package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.DigestMethod -- what a datum is digested with.
 *
 * <p>Each {@link Reference} carries one: it is the algorithm the digest of what that reference
 * points to was computed with.
 *
 * <p>{@link #SHA1} is still on the list and should not be used. Its collision resistance has been
 * broken since 2017, and a collision in a reference's digest means two different data pass the same
 * validation. It stays defined because there are old signatures that have to be readable.
 *
 * <p>None of the standard algorithms takes parameters, so {@link #getParameterSpec} returns null
 * almost always. The method exists for the ones that do take them.
 */
public interface DigestMethod extends XMLStructure, AlgorithmMethod {

    /** SHA-1. Broken; do not use. */
    static final String SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    /** 224-bit SHA-2. */
    static final String SHA224 = "http://www.w3.org/2001/04/xmldsig-more#sha224";

    /** 256-bit SHA-2. */
    static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";

    /** 384-bit SHA-2. */
    static final String SHA384 = "http://www.w3.org/2001/04/xmldsig-more#sha384";

    /** 512-bit SHA-2. */
    static final String SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    /** RIPEMD-160. */
    static final String RIPEMD160 = "http://www.w3.org/2001/04/xmlenc#ripemd160";

    /** 224-bit SHA-3. */
    static final String SHA3_224 = "http://www.w3.org/2007/05/xmldsig-more#sha3-224";

    /** 256-bit SHA-3. */
    static final String SHA3_256 = "http://www.w3.org/2007/05/xmldsig-more#sha3-256";

    /** 384-bit SHA-3. */
    static final String SHA3_384 = "http://www.w3.org/2007/05/xmldsig-more#sha3-384";

    /** 512-bit SHA-3. */
    static final String SHA3_512 = "http://www.w3.org/2007/05/xmldsig-more#sha3-512";

    /** The algorithm's parameters, or null. */
    AlgorithmParameterSpec getParameterSpec();
}
