package java.security.spec;

// Marks the **transparent** parameters of a cryptographic algorithm.
//
// It declares not a single method, and that is the whole idea: the opaque counterpart is
// `java.security.AlgorithmParameters`, which keeps the parameters encoded. An
// `AlgorithmParameterSpec` is the version the program can read field by field — the RSA modulus and
// exponent, the EC curve — and the interface exists so any of those can be passed through one
// common parameter without the receiver knowing which one it is. (This note said
// `AlgorithmParameters` only knows how to return them as bytes; its `getParameterSpec` also hands
// them back as a spec.)
public interface AlgorithmParameterSpec {
}
