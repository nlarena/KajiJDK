package javax.xml.crypto.dsig.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec -- los parametros de un
 * algoritmo de firma.
 *
 * <p>Marcadora. Su implementacion habitual es {@link HMACParameterSpec}: el unico de los algoritmos
 * clasicos que tiene algo que configurar.
 */
public interface SignatureMethodParameterSpec extends AlgorithmParameterSpec {
}
