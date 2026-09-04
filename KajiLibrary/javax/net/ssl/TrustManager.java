package javax.net.ssl;

/**
 * Quien decide si las credenciales del <em>otro</em> son de fiar.
 *
 * <p>La contraparte de {@link KeyManager}: uno presenta, el otro juzga. Tambien es una interfaz
 * marcadora, y por la misma razon — ver {@link X509TrustManager} para la forma que se usa en la
 * practica.
 */
public interface TrustManager {
}
