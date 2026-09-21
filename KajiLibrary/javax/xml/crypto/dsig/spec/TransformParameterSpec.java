package javax.xml.crypto.dsig.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.TransformParameterSpec -- the parameters of a transform.
 *
 * <p>A marker, without methods. Its only job is to <b>type</b>: {@code newTransform} receives one
 * of these and not just any {@link AlgorithmParameterSpec}, so passing it the parameters of a
 * signature algorithm does not compile.
 *
 * <p>It seems little and it avoids the classic mistake of this API. XML-DSig's parameters are all
 * {@code AlgorithmParameterSpec}s and without this hierarchy they would be interchangeable in the
 * compiler's eyes, with the failure appearing only when signing.
 */
public interface TransformParameterSpec extends AlgorithmParameterSpec {
}
