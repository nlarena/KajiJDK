package javax.xml.crypto.dsig.spec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.C14NMethodParameterSpec -- the parameters of a
 * canonicalization.
 *
 * <p>It extends {@link TransformParameterSpec} and not {@code AlgorithmParameterSpec} directly, and
 * that says something about the model: a canonicalization <b>is</b> a transform --the one that
 * turns nodes into bytes-- and that is why its parameters hold in both places.
 */
public interface C14NMethodParameterSpec extends TransformParameterSpec {
}
