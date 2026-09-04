package java.security;

// Fallo en el manejo de claves visto desde afuera de una operacion concreta: un almacen que no se
// pudo abrir, una clave que no se pudo publicar o revocar.
//
// Cuelga de `KeyException` y no de `GeneralSecurityException` porque el sujeto sigue siendo la
// clave; lo que cambia es que el problema es de administracion y no de uso.
public class KeyManagementException extends KeyException {

    public KeyManagementException() {
        super();
    }

    public KeyManagementException(String message) {
        super(message);
    }

    public KeyManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyManagementException(Throwable cause) {
        super(cause);
    }
}
