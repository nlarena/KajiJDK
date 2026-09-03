package java.security.spec;

// Parametros de algoritmo dichos por **nombre** en vez de por valor.
//
// Es la respuesta a un problema real de `ECParameterSpec`: describir P-256 a mano exige escribir un
// primo de 256 bits, dos coeficientes, un generador y un orden, y basta con equivocarse en un bit
// para terminar operando sobre una curva distinta —posiblemente debil— sin que nada avise. Decir
// "secp256r1" no se puede escribir mal a medias: o el nombre existe o no.
//
// Las familias nuevas (X25519, Ed25519, ML-KEM) directamente **solo** se nombran: sus parametros son
// fijos por diseño, y no ofrecer forma de escribirlos a mano es una decision de seguridad, no una
// limitacion.
//
// Esta clase guarda el nombre y nada mas. **No resuelve el nombre a parametros concretos**: no hay
// tabla de curvas aca, porque una tabla incompleta o mal transcripta seria peor que no tenerla.
public class NamedParameterSpec implements AlgorithmParameterSpec {

    public static final NamedParameterSpec X25519 = new NamedParameterSpec("X25519");
    public static final NamedParameterSpec X448 = new NamedParameterSpec("X448");
    public static final NamedParameterSpec ED25519 = new NamedParameterSpec("Ed25519");
    public static final NamedParameterSpec ED448 = new NamedParameterSpec("Ed448");

    // Los tres niveles de ML-DSA y ML-KEM (FIPS 204 y 203). El numero es la categoria de parametros,
    // no el largo de la clave.
    public static final NamedParameterSpec ML_DSA_44 = new NamedParameterSpec("ML-DSA-44");
    public static final NamedParameterSpec ML_DSA_65 = new NamedParameterSpec("ML-DSA-65");
    public static final NamedParameterSpec ML_DSA_87 = new NamedParameterSpec("ML-DSA-87");
    public static final NamedParameterSpec ML_KEM_512 = new NamedParameterSpec("ML-KEM-512");
    public static final NamedParameterSpec ML_KEM_768 = new NamedParameterSpec("ML-KEM-768");
    public static final NamedParameterSpec ML_KEM_1024 = new NamedParameterSpec("ML-KEM-1024");

    private final String name;

    public NamedParameterSpec(String stdName) {
        if (stdName == null) {
            throw new NullPointerException("stdName must not be null");
        }
        this.name = stdName;
    }

    public String getName() {
        return this.name;
    }
}
