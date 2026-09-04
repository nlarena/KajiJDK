package java.net;

import java.io.IOException;

/**
 * KajiLibrary's java.net.UnknownServiceException -- la conexion no soporta la operacion que se le
 * pidio.
 *
 * <p>La distincion con {@link java.net.MalformedURLException} es la que vale la pena tener clara, y
 * es la razon de que sean dos excepciones y no una: aquella dice que **la URL no se entiende**; esta
 * dice que se entendio perfectamente y que **no hay como atenderla**. Un `http://ejemplo` mal escrito
 * es lo primero; un `http://ejemplo` bien escrito en una biblioteca que no trae cliente HTTP es lo
 * segundo. Quien las confunde va a buscar el error en el lugar equivocado.
 *
 * <p>Es lo que tira {@link URL#openStream()} para todo esquema que no sea `file:`.
 */
public class UnknownServiceException extends IOException {

    public UnknownServiceException() {
        super();
    }

    public UnknownServiceException(String msg) {
        super(msg);
    }
}
