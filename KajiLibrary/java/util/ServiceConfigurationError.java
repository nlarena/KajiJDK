package java.util;

// Algo salio mal cargando un proveedor de servicio.
//
// Es un `Error` y no una `Exception`, que es una eleccion fuerte del JDK y esta razonada: una
// configuracion de servicios rota no es una condicion que el programa pueda manejar, es un
// despliegue mal armado. Tratarla como recuperable llevaria a programas que arrancan a medias.
public class ServiceConfigurationError extends Error {

    // Con el mensaje dado.
    public ServiceConfigurationError(String msg) {
        super(msg);
    }

    // Con el mensaje dado y `cause` como causa.
    public ServiceConfigurationError(String msg, Throwable cause) {
        super(msg, cause);
    }
}
