package java.security;

// La base de los problemas con claves: invalida, mal codificada, imposible de administrar.
//
// Se conserva como nivel intermedio —y no se colapsa contra `GeneralSecurityException`— porque
// `InvalidKeyException` y `KeyManagementException` cuelgan de ella, y hay codigo que quiere
// atrapar "cualquier cosa de claves" sin atrapar tambien un fallo de firma.
public class KeyException extends GeneralSecurityException {

    public KeyException() {
        super();
    }

    public KeyException(String message) {
        super(message);
    }

    public KeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyException(Throwable cause) {
        super(cause);
    }
}
