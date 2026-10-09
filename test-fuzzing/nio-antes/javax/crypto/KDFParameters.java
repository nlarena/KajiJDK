package javax.crypto;

/**
 * The parameters a key derivation function is built with.
 *
 * <h2>Why it is empty</h2>
 *
 * <p>It marks, it does not describe. Every derivation function has its own construction parameters
 * and there is nothing all five have in common, so the interface cannot demand any method. What it
 * contributes is the type: {@link KDF#getInstance(String, KDFParameters)} does not accept just any
 * object.
 *
 * <h2>What they are not</h2>
 *
 * <p>These are the function's parameters, not a derivation's. Each derivation's go in
 * {@link KDF#deriveKey}, and they are an {@link java.security.spec.AlgorithmParameterSpec}. The
 * difference matters: the function is built once and used many times.
 *
 * @since 24
 */
public interface KDFParameters {
}
