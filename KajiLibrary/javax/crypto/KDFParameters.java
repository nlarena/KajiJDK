package javax.crypto;

/**
 * Los parametros con que se arma una funcion de derivacion de claves.
 *
 * <h2>Por que vacia</h2>
 *
 * <p>Marca, no describe. Cada funcion de derivacion tiene sus propios parametros de construccion y
 * no hay nada que las cinco tengan en comun, asi que la interfaz no puede pedir ningun metodo. Lo
 * que aporta es el tipo: {@link KDF#getInstance(String, KDFParameters)} no acepta cualquier objeto.
 *
 * <h2>Que no son</h2>
 *
 * <p>Estos son los parametros de la funcion, no los de una derivacion. Los de cada derivacion van en
 * {@link KDF#deriveKey}, y son un {@link java.security.spec.AlgorithmParameterSpec}. La diferencia
 * importa: la funcion se arma una vez y se usa muchas.
 *
 * @since 24
 */
public interface KDFParameters {
}
