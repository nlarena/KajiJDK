package java.net;

import java.io.IOException;

// La raiz de los errores del subsistema de sockets: "el socket dijo que no".
//
// KajiJDK no tiene pila de red, y sin embargo esta clase **si** corresponde que exista. La razon es
// que una excepcion no promete nada: no dice "yo se abrir un socket", dice "si algo falla al abrir
// un socket, se llama asi". Es un tipo, no una capacidad. Compilar contra ella y atraparla es
// correcto aunque en esta VM no la tire nadie de la biblioteca -- codigo portable la atrapa igual, y
// codigo que la construya y la tire por su cuenta obtiene exactamente el objeto que espera.
//
// Es el mismo criterio que hace legitimo tener `UnknownHostException` desde antes que hubiera con
// que resolver un nombre.
public class SocketException extends IOException {

    private static final long serialVersionUID = -5935874303556886934L;

    public SocketException(String msg) {
        super(msg);
    }

    public SocketException() {
    }

    // Esta y la de solo `Throwable` llegaron en Java 13, cuando el JDK dejo de perder la causa al
    // envolver errores del sistema operativo.
    public SocketException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SocketException(Throwable cause) {
        super(cause);
    }
}
