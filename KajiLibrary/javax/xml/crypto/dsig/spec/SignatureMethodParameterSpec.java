package javax.xml.crypto.dsig.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec -- the parameters of a
 * signature algorithm.
 *
 * <p>A marker. Its usual implementation is {@link HMACParameterSpec}: the only one of the classic
 * algorithms that has something to configure.
 */
public interface SignatureMethodParameterSpec extends AlgorithmParameterSpec {
}
