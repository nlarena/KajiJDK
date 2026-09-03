package java.security.interfaces;

import java.security.InvalidParameterException;
import java.security.SecureRandom;

/**
 * KajiLibrary's java.security.interfaces.DSAKeyPairGenerator -- la configuracion extra que acepta un
 * generador de claves DSA.
 *
 * <p>La implementa un {@link java.security.KeyPairGenerator} que genere DSA, y existe porque DSA
 * tiene una configuracion que no entra en "cuantos bits": los <b>parametros de dominio</b> p, q y g,
 * que se comparten entre muchas claves y son caros de generar.
 *
 * <h2>Por que hay dos formas de inicializar</h2>
 *
 * <p>Generar parametros de dominio es buscar primos grandes con una relacion entre ellos, y eso
 * cuesta segundos o minutos. Por eso la practica normal es <b>reusarlos</b>: una organizacion genera
 * un juego y todas sus claves lo comparten. Las dos formas cubren los dos casos:
 *
 * <ul>
 *   <li>{@link #initialize(DSAParams, SecureRandom)} -- ya los tengo, usa estos.
 *   <li>{@link #initialize(int, boolean, SecureRandom)} -- genera unos nuevos de ese tamaño, o
 *       tomalos de los precalculados que traiga el proveedor.
 * </ul>
 *
 * <p>Compartir parametros de dominio <b>no</b> debilita las claves: p, q y g son publicos y estan en
 * el certificado. Lo que nunca se comparte es la clave privada x, que sale del azar.
 *
 * <h2>La fuente de azar no es opcional</h2>
 *
 * <p>Los dos metodos la reciben, y en los dos se usa para lo mismo que en cualquier generador de
 * claves: elegir la clave privada. Ver {@link java.security.KeyPairGeneratorSpi} para por que el API
 * no deja generar sin decir de donde sale.
 *
 * <p><b>Esta biblioteca no trae ningun generador DSA</b>: la interfaz esta para que un proveedor que
 * lo escriba encaje, igual que {@code X509Certificate} esta sin que haya ningun parser de
 * certificados.
 */
public interface DSAKeyPairGenerator {

    /**
     * Usa estos parametros de dominio.
     *
     * @param params los p, q y g ya calculados
     * @param random de donde sale la clave privada
     * @throws InvalidParameterException si el generador no acepta esos parametros
     */
    void initialize(DSAParams params, SecureRandom random) throws InvalidParameterException;

    /**
     * Genera o toma parametros de dominio de ese tamaño.
     *
     * @param modlen    los bits de p
     * @param genParams si <b>generar</b> parametros nuevos, o tomar los precalculados que traiga el
     *     proveedor. Generar es caro; tomarlos es instantaneo y no es menos seguro, porque los
     *     parametros son publicos. Un proveedor que no tenga precalculados para ese tamaño y reciba
     *     false tiene que rechazar en vez de generar igual: el llamador pidio no esperar
     * @param random    de donde sale la clave privada
     * @throws InvalidParameterException si el tamaño no le sirve, o si se pidieron precalculados que
     *     no tiene
     */
    void initialize(int modlen, boolean genParams, SecureRandom random)
        throws InvalidParameterException;
}
