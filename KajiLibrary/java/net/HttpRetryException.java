package java.net;

import java.io.IOException;

// Un pedido HTTP hay que repetirlo, pero automaticamente no se puede.
//
// El caso tipico: se mando un POST con cuerpo, el servidor contesto 3xx, y reintentar significaria
// volver a mandar el cuerpo -- que puede ser un stream ya consumido, o una operacion que no es
// idempotente. En vez de decidir por el llamador, el JDK aborta y le entrega los datos que
// necesitaria para decidir: el codigo, la razon, y a donde redirigia.
//
// Por eso los tres accessors: sin ellos la excepcion diria "reintenta" sin decir que ni adonde, y
// no serviria de nada.
public class HttpRetryException extends IOException {

    private static final long serialVersionUID = -9186022286469111381L;

    private final int responseCode;
    private final String location;

    public HttpRetryException(String detail, int code) {
        super(detail);
        this.responseCode = code;
        this.location = null;
    }

    public HttpRetryException(String detail, int code, String location) {
        super(detail);
        this.responseCode = code;
        this.location = location;
    }

    public int responseCode() {
        return this.responseCode;
    }

    // Es el mensaje de detalle, no un campo aparte: el JDK reusa `getMessage()` aca. Se deja igual
    // porque el contrato publico es "la razon", y la razon es lo que se paso como detalle.
    public String getReason() {
        return super.getMessage();
    }

    /** El {@code Location} de la respuesta, o null si no habia. */
    public String getLocation() {
        return this.location;
    }
}
