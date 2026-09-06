package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * El relleno del bloque descifrado no es el que tendria que ser.
 *
 * <p>Casi siempre significa que la clave esta mal. Un cifrado por bloques descifra cualquier cosa
 * --la operacion no puede fallar-- y lo que sale con la clave equivocada es ruido; el unico lugar
 * donde se nota es el relleno del final, que no cierra.
 *
 * <p>Por eso hay que tener cuidado con esta excepcion: contestarle distinto al que manda datos
 * segun si el relleno cerro o no es la fuga por la que se descifra un mensaje entero sin la clave.
 * Quien la atrapa deberia dar siempre el mismo error hacia afuera.
 *
 * @since 1.4
 */
public class BadPaddingException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** Una sin mensaje. */
    public BadPaddingException() {
        super();
    }

    /**
     * Una con mensaje.
     *
     * @param msg que paso
     */
    public BadPaddingException(String msg) {
        super(msg);
    }
}
