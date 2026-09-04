package javax.imageio;

import java.io.IOException;

/**
 * KajiLibrary's javax.imageio.IIOException -- fallo una operacion de lectura o escritura de imagenes.
 *
 * <p>Es una {@link IOException} y no una excepcion aparte, y esa decision explica como se usa la API:
 * un metodo que lee una imagen ya declara {@code IOException}, asi que agregar esto no obliga a
 * cambiar ninguna firma.
 *
 * <p>La usan los lectores y escritores para distinguir "el archivo esta roto o el formato no se
 * entiende" de "fallo el disco". Las dos salen como {@code IOException}; solo la primera es una
 * {@code IIOException}.
 *
 * <p>El constructor con causa es el importante: un decodificador que falla adentro casi siempre tiene
 * algo mas concreto que decir, y envolverlo conserva esa traza.
 */
public class IIOException extends IOException {

    private static final long serialVersionUID = -3216210718638985251L;

    /** @param message que paso */
    public IIOException(String message) {
        super(message);
    }

    /**
     * @param message que paso
     * @param cause la original
     */
    public IIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
