package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.DigestMethod -- con que se resume un dato.
 *
 * <p>Cada {@link Reference} lleva uno: es el algoritmo con el que se calculo el resumen de lo que esa
 * referencia apunta.
 *
 * <p>{@link #SHA1} sigue en la lista y no hay que usarlo. Esta roto para resistir colisiones desde
 * 2017, y una colision en el resumen de una referencia significa que dos datos distintos pasan la
 * misma validacion. Sigue definido porque hay firmas viejas que hay que poder leer.
 *
 * <p>Ninguno de los algoritmos estandar lleva parametros, asi que {@link #getParameterSpec} devuelve
 * null casi siempre. El metodo existe para los que si los lleven.
 */
public interface DigestMethod extends XMLStructure, AlgorithmMethod {

    /** SHA-1. Roto; no usar. */
    static final String SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    /** SHA-2 de 224 bits. */
    static final String SHA224 = "http://www.w3.org/2001/04/xmldsig-more#sha224";

    /** SHA-2 de 256 bits. */
    static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";

    /** SHA-2 de 384 bits. */
    static final String SHA384 = "http://www.w3.org/2001/04/xmldsig-more#sha384";

    /** SHA-2 de 512 bits. */
    static final String SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    /** RIPEMD-160. */
    static final String RIPEMD160 = "http://www.w3.org/2001/04/xmlenc#ripemd160";

    /** SHA-3 de 224 bits. */
    static final String SHA3_224 = "http://www.w3.org/2007/05/xmldsig-more#sha3-224";

    /** SHA-3 de 256 bits. */
    static final String SHA3_256 = "http://www.w3.org/2007/05/xmldsig-more#sha3-256";

    /** SHA-3 de 384 bits. */
    static final String SHA3_384 = "http://www.w3.org/2007/05/xmldsig-more#sha3-384";

    /** SHA-3 de 512 bits. */
    static final String SHA3_512 = "http://www.w3.org/2007/05/xmldsig-more#sha3-512";

    /** Los parametros del algoritmo, o null. */
    AlgorithmParameterSpec getParameterSpec();
}
