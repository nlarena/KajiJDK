package javax.xml.crypto.dsig.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.TransformParameterSpec -- los parametros de una
 * transformacion.
 *
 * <p>Marcadora, sin metodos. Su unico trabajo es <b>tipar</b>: {@code newTransform} recibe uno de
 * estos y no un {@link AlgorithmParameterSpec} cualquiera, asi que pasarle los parametros de un
 * algoritmo de firma no compila.
 *
 * <p>Parece poco y evita el error clasico de este API. Los parametros de XML-DSig son todos
 * {@code AlgorithmParameterSpec} y sin esta jerarquia serian intercambiables a los ojos del
 * compilador, con el fallo apareciendo recien al firmar.
 */
public interface TransformParameterSpec extends AlgorithmParameterSpec {
}
