package java.security.spec;

// Algorithm parameters given by **name** instead of by value.
//
// It is the answer to a real problem of `ECParameterSpec`: describing P-256 by hand requires
// writing a 256-bit prime, two coefficients, a generator and an order, and getting one bit wrong is
// enough to end up operating over a different curve —possibly a weak one— without anything warning.
// A name is either known or it is not.
//
// The newer families (X25519, Ed25519, ML-KEM) are **only** named: their parameters are fixed by
// design, and offering no way to write them by hand is a security decision, not a limitation.
//
// This class keeps the name and nothing else. **It does not resolve the name to concrete
// parameters**: there is no table of curves here, because an incomplete or mistranscribed table
// would be worse than none.
public class NamedParameterSpec implements AlgorithmParameterSpec {

    public static final NamedParameterSpec X25519 = new NamedParameterSpec("X25519");
    public static final NamedParameterSpec X448 = new NamedParameterSpec("X448");
    public static final NamedParameterSpec ED25519 = new NamedParameterSpec("Ed25519");
    public static final NamedParameterSpec ED448 = new NamedParameterSpec("Ed448");

    // The three levels of ML-DSA and ML-KEM (FIPS 204 and 203). The number names the parameter set;
    // it is neither the key length nor, as this comment said, the security category (ML-KEM-768 is
    // category 3, ML-DSA-44 category 2).
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
