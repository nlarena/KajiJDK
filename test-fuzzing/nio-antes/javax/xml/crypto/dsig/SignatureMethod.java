package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignatureMethod -- what the {@link SignedInfo} is signed
 * with.
 *
 * <p>The list is long because it crosses three things: the key family --RSA, DSA, ECDSA, EdDSA, or
 * HMAC with a secret key--, the digest, and in the case of RSA the padding scheme.
 *
 * <h2>What is worth knowing when choosing</h2>
 *
 * <ul>
 *   <li>the {@code _SHA1} ones are broken and remain for compatibility;
 *   <li>the {@code HMAC_*} ones are not public-key signatures: they use a <b>shared secret</b>, so
 *       anyone who can validate can also sign. They do not serve for non-repudiation;
 *   <li>the {@code _RSA_MGF1} ones and {@link #RSA_PSS} use PSS padding, which is the one
 *       recommended today; the {@code RSA_SHA*} ones use PKCS#1 v1.5, which is still acceptable but
 *       no longer recommended for new work.
 * </ul>
 *
 * <p>{@link #getParameterSpec} returns something only for the ones that take parameters: HMAC --the
 * truncated length-- and RSA-PSS.
 */
public interface SignatureMethod extends XMLStructure, AlgorithmMethod {

    /** DSA with SHA1. */
    static final String DSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";

    /** DSA with SHA256. */
    static final String DSA_SHA256 = "http://www.w3.org/2009/xmldsig11#dsa-sha256";

    /** RSA PKCS#1 v1.5 with SHA1. */
    static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    /** RSA PKCS#1 v1.5 with SHA224. */
    static final String RSA_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha224";

    /** RSA PKCS#1 v1.5 with SHA256. */
    static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /** RSA PKCS#1 v1.5 with SHA384. */
    static final String RSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";

    /** RSA PKCS#1 v1.5 with SHA512. */
    static final String RSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    /** RSA-PSS with SHA1 and MGF1 mask. */
    static final String SHA1_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1";

    /** RSA-PSS with SHA224 and MGF1 mask. */
    static final String SHA224_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha224-rsa-MGF1";

    /** RSA-PSS with SHA256 and MGF1 mask. */
    static final String SHA256_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1";

    /** RSA-PSS with SHA384 and MGF1 mask. */
    static final String SHA384_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha384-rsa-MGF1";

    /** RSA-PSS with SHA512 and MGF1 mask. */
    static final String SHA512_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha512-rsa-MGF1";

    /** ECDSA with SHA1. */
    static final String ECDSA_SHA1 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1";

    /** ECDSA with SHA224. */
    static final String ECDSA_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224";

    /** ECDSA with SHA256. */
    static final String ECDSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";

    /** ECDSA with SHA384. */
    static final String ECDSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";

    /** ECDSA with SHA512. */
    static final String ECDSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512";

    /** HMAC with SHA1; it uses a secret key, not a key pair. */
    static final String HMAC_SHA1 = "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    /** HMAC with SHA224; it uses a secret key, not a key pair. */
    static final String HMAC_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha224";

    /** HMAC with SHA256; it uses a secret key, not a key pair. */
    static final String HMAC_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";

    /** HMAC with SHA384; it uses a secret key, not a key pair. */
    static final String HMAC_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";

    /** HMAC with SHA512; it uses a secret key, not a key pair. */
    static final String HMAC_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";

    /** RSA with PSS padding; its parameters go in an {@code RSAPSSParameterSpec}. */
    static final String RSA_PSS = "http://www.w3.org/2007/05/xmldsig-more#rsa-pss";

    /** EdDSA over curve 25519. */
    static final String ED25519 = "http://www.w3.org/2021/04/xmldsig-more#eddsa-ed25519";

    /** EdDSA over curve 448. */
    static final String ED448 = "http://www.w3.org/2021/04/xmldsig-more#eddsa-ed448";

    /** RSA-PSS with SHA3-224 and MGF1 mask. */
    static final String SHA3_224_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-224-rsa-MGF1";

    /** RSA-PSS with SHA3-256 and MGF1 mask. */
    static final String SHA3_256_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-256-rsa-MGF1";

    /** RSA-PSS with SHA3-384 and MGF1 mask. */
    static final String SHA3_384_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-384-rsa-MGF1";

    /** RSA-PSS with SHA3-512 and MGF1 mask. */
    static final String SHA3_512_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-512-rsa-MGF1";

    /** ECDSA with SHA3-224. */
    static final String ECDSA_SHA3_224 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-224";

    /** ECDSA with SHA3-256. */
    static final String ECDSA_SHA3_256 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-256";

    /** ECDSA with SHA3-384. */
    static final String ECDSA_SHA3_384 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-384";

    /** ECDSA with SHA3-512. */
    static final String ECDSA_SHA3_512 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-512";

    /** The algorithm's parameters, or null. */
    AlgorithmParameterSpec getParameterSpec();
}
