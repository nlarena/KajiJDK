package java.util;

import java.io.IOException;

// El XML de un `Properties` no cumple el DTD de properties.
//
// Es una `IOException` y no una `RuntimeException` porque el que carga un properties ya esta
// obligado a tratar la entrada/salida: un archivo mal formado es otra forma de que la carga
// falle, no una categoria aparte que el llamador tenga que descubrir.
//
// El JDK la declara sin los constructores de `Throwable` que aceptan `null` de causa; se replica
// esa forma: las dos que hay, y ninguna mas.
public class InvalidPropertiesFormatException extends IOException {

    // Con `cause` como causa; el mensaje sale de ella.
    public InvalidPropertiesFormatException(Throwable cause) {
        super(cause == null ? null : cause.toString());
        this.initCause(cause);
    }

    // Con el mensaje dado.
    public InvalidPropertiesFormatException(String message) {
        super(message);
    }
}
