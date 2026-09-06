package javax.crypto;

/**
 * Un cifrador que no cifra: entrega los bytes tal como los recibe.
 *
 * <h2>Para que sirve algo que no hace nada</h2>
 *
 * <p>Para poder escribir el programa una sola vez. Un protocolo que puede negociar cifrado o no
 * cifrado --y son muchos-- tendria si no dos caminos distintos, uno con {@link Cipher} y otro sin
 * el, y esos dos caminos se desincronizan. Con esto hay uno solo, y la decision de cifrar queda en
 * un lugar: cual cifrador se arma.
 *
 * <p>Tambien sirve para medir cuanto cuesta la maquinaria sin el algoritmo, y para probar el resto
 * del programa sin necesitar claves.
 *
 * <h2>No hace falta configurarlo</h2>
 *
 * <p>Nace listo. Es la unica diferencia visible con cualquier otro {@link Cipher}, que tira si se lo
 * usa sin {@code init}, y tiene sentido: no hay nada que configurar cuando no hay clave.
 *
 * @since 1.4
 */
public class NullCipher extends Cipher {

    /** Uno. */
    public NullCipher() {
        super(new NullCipherSpi(), null, null);
        this.sinConfigurar = true;
    }
}
