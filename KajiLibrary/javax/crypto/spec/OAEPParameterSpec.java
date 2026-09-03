package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;

/**
 * Los parametros de OAEP: que hash, que funcion de mascara y que etiqueta.
 *
 * <p>Los dos hashes son independientes y por eso son dos campos: el del mensaje y el de la funcion
 * de mascara pueden ser distintos, y en la practica muchas veces lo son porque el receptor espera
 * una combinacion concreta. Configurar mal cualquiera de los dos hace que el descifrado falle sin
 * decir por que, que es lo esperable de un relleno que no debe filtrar informacion.
 *
 * <p>{@link #DEFAULT} es SHA-1 con MGF1-SHA-1 y etiqueta vacia, que es lo que PKCS#1 define como
 * omision. **No es una recomendacion**: SHA-1 esta obsoleto y una aplicacion nueva deberia elegir
 * SHA-256 explicitamente. Esta ahi porque el estandar lo define asi y porque hace falta para
 * interoperar con lo que ya existe.
 */
public class OAEPParameterSpec implements AlgorithmParameterSpec {

    /** SHA-1, MGF1 con SHA-1 y etiqueta vacia. Ver la nota de la clase. */
    public static final OAEPParameterSpec DEFAULT = new OAEPParameterSpec(
            "SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);

    private final String mdName;
    private final String mgfName;
    private final AlgorithmParameterSpec mgfSpec;
    private final PSource pSrc;

    /**
     * @throws NullPointerException si alguno de los tres nombres o la fuente son nulos --`mgfSpec`
     *     si puede ser nulo, y significa que la funcion de mascara no lleva parametros--
     */
    public OAEPParameterSpec(String mdName, String mgfName, AlgorithmParameterSpec mgfSpec,
            PSource pSrc) {
        if (mdName == null) {
            throw new NullPointerException("el algoritmo de digesto no puede ser nulo");
        }
        if (mgfName == null) {
            throw new NullPointerException("el algoritmo de mascara no puede ser nulo");
        }
        if (pSrc == null) {
            throw new NullPointerException("la fuente de la etiqueta no puede ser nula");
        }
        this.mdName = mdName;
        this.mgfName = mgfName;
        this.mgfSpec = mgfSpec;
        this.pSrc = pSrc;
    }

    /** El hash del mensaje. */
    public String getDigestAlgorithm() {
        return this.mdName;
    }

    /** La funcion generadora de mascara. */
    public String getMGFAlgorithm() {
        return this.mgfName;
    }

    /** Los parametros de la funcion de mascara, o nulo si no lleva. */
    public AlgorithmParameterSpec getMGFParameters() {
        return this.mgfSpec;
    }

    /** De donde sale la etiqueta. */
    public PSource getPSource() {
        return this.pSrc;
    }
}
