package javax.xml.crypto.dsig.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.DigestMethodParameterSpec -- los parametros de un
 * resumen.
 *
 * <p>Marcadora, y <b>ninguno</b> de los algoritmos de resumen estandar lleva parametros: SHA-256 no
 * se configura. Existe igual porque el API tiene que poder recibirlos de un algoritmo que si los
 * lleve, y porque sin ella {@code newDigestMethod} aceptaria los parametros de cualquier otra cosa.
 */
public interface DigestMethodParameterSpec extends AlgorithmParameterSpec {
}
