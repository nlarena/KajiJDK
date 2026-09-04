package javax.xml.crypto.dsig.spec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.C14NMethodParameterSpec -- los parametros de una
 * canonicalizacion.
 *
 * <p>Extiende {@link TransformParameterSpec} y no {@code AlgorithmParameterSpec} directamente, y eso
 * dice algo del modelo: una canonicalizacion <b>es</b> una transformacion --la que convierte nodos en
 * bytes-- y por eso sus parametros valen en los dos lugares.
 */
public interface C14NMethodParameterSpec extends TransformParameterSpec {
}
