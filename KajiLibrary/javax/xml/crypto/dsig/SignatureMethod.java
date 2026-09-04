package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignatureMethod -- con que se firma el {@link SignedInfo}.
 *
 * <p>La lista es larga porque cruza tres cosas: la familia de clave --RSA, DSA, ECDSA, EdDSA, o HMAC
 * con clave secreta--, el resumen, y en el caso de RSA el esquema de relleno.
 *
 * <h2>Lo que conviene saber al elegir</h2>
 *
 * <ul>
 *   <li>los {@code _SHA1} estan rotos y siguen por compatibilidad;
 *   <li>los {@code HMAC_*} no son firma de clave publica: usan un <b>secreto compartido</b>, asi que
 *       cualquiera que pueda validar tambien puede firmar. No sirven para no repudio;
 *   <li>los {@code _RSA_MGF1} y {@link #RSA_PSS} usan el relleno PSS, que es el recomendado hoy; los
 *       {@code RSA_SHA*} usan PKCS#1 v1.5, que sigue siendo aceptable pero ya no se recomienda para
 *       lo nuevo.
 * </ul>
 *
 * <p>{@link #getParameterSpec} devuelve algo solo para los que llevan parametros: HMAC --el largo
 * truncado-- y RSA-PSS.
 */
public interface SignatureMethod extends XMLStructure, AlgorithmMethod {

    /** DSA con SHA1. */
    static final String DSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";

    /** DSA con SHA256. */
    static final String DSA_SHA256 = "http://www.w3.org/2009/xmldsig11#dsa-sha256";

    /** RSA PKCS#1 v1.5 con SHA1. */
    static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    /** RSA PKCS#1 v1.5 con SHA224. */
    static final String RSA_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha224";

    /** RSA PKCS#1 v1.5 con SHA256. */
    static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /** RSA PKCS#1 v1.5 con SHA384. */
    static final String RSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";

    /** RSA PKCS#1 v1.5 con SHA512. */
    static final String RSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    /** RSA-PSS con SHA1 y mascara MGF1. */
    static final String SHA1_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1";

    /** RSA-PSS con SHA224 y mascara MGF1. */
    static final String SHA224_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha224-rsa-MGF1";

    /** RSA-PSS con SHA256 y mascara MGF1. */
    static final String SHA256_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1";

    /** RSA-PSS con SHA384 y mascara MGF1. */
    static final String SHA384_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha384-rsa-MGF1";

    /** RSA-PSS con SHA512 y mascara MGF1. */
    static final String SHA512_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha512-rsa-MGF1";

    /** ECDSA con SHA1. */
    static final String ECDSA_SHA1 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1";

    /** ECDSA con SHA224. */
    static final String ECDSA_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224";

    /** ECDSA con SHA256. */
    static final String ECDSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";

    /** ECDSA con SHA384. */
    static final String ECDSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";

    /** ECDSA con SHA512. */
    static final String ECDSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512";

    /** HMAC con SHA1; usa clave secreta, no par de claves. */
    static final String HMAC_SHA1 = "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    /** HMAC con SHA224; usa clave secreta, no par de claves. */
    static final String HMAC_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha224";

    /** HMAC con SHA256; usa clave secreta, no par de claves. */
    static final String HMAC_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";

    /** HMAC con SHA384; usa clave secreta, no par de claves. */
    static final String HMAC_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";

    /** HMAC con SHA512; usa clave secreta, no par de claves. */
    static final String HMAC_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";

    /** RSA con relleno PSS; sus parametros van en un {@code RSAPSSParameterSpec}. */
    static final String RSA_PSS = "http://www.w3.org/2007/05/xmldsig-more#rsa-pss";

    /** EdDSA sobre la curva 25519. */
    static final String ED25519 = "http://www.w3.org/2021/04/xmldsig-more#eddsa-ed25519";

    /** EdDSA sobre la curva 448. */
    static final String ED448 = "http://www.w3.org/2021/04/xmldsig-more#eddsa-ed448";

    /** RSA-PSS con SHA3-224 y mascara MGF1. */
    static final String SHA3_224_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-224-rsa-MGF1";

    /** RSA-PSS con SHA3-256 y mascara MGF1. */
    static final String SHA3_256_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-256-rsa-MGF1";

    /** RSA-PSS con SHA3-384 y mascara MGF1. */
    static final String SHA3_384_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-384-rsa-MGF1";

    /** RSA-PSS con SHA3-512 y mascara MGF1. */
    static final String SHA3_512_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha3-512-rsa-MGF1";

    /** ECDSA con SHA3-224. */
    static final String ECDSA_SHA3_224 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-224";

    /** ECDSA con SHA3-256. */
    static final String ECDSA_SHA3_256 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-256";

    /** ECDSA con SHA3-384. */
    static final String ECDSA_SHA3_384 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-384";

    /** ECDSA con SHA3-512. */
    static final String ECDSA_SHA3_512 = "http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-512";

    /** Los parametros del algoritmo, o null. */
    AlgorithmParameterSpec getParameterSpec();
}
