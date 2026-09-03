package java.security.spec;

// Los parametros de MGF1, la funcion de generacion de mascara de PKCS#1 v2.
//
// MGF1 convierte una semilla corta en todos los bytes que hagan falta, concatenando
// Hash(semilla || contador) con el contador creciendo. Es lo que usan OAEP y PSS para producir la
// mascara con la que enmascaran el bloque, y su unico parametro es **cual** hash.
//
// Que el hash de MGF1 sea configurable aparte del hash principal es una fuente clasica de errores de
// interoperabilidad: una firma PSS con SHA-256 como digest y SHA-1 como MGF1 es perfectamente legal
// y no verifica contra un verificador que asumio SHA-256 en los dos lados. Por eso el valor viaja
// explicito en `PSSParameterSpec` en vez de darse por sentado.
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

    // El nombre del hash, tal cual se lo pasaria a `MessageDigest.getInstance`.
    public String getDigestAlgorithm() {
        return this.mdName;
    }

    // El formato es parte del contrato observable: `PSSParameterSpec.toString()` mete este texto
    // adentro del suyo.
    @Override
    public String toString() {
        return "MGF1ParameterSpec[hashAlgorithm=" + this.mdName + "]";
    }
}
