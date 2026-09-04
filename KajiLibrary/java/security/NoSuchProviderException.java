package java.security;

// No hay ningun proveedor registrado con ese nombre.
//
// Es distinta de `NoSuchAlgorithmException`: aca el problema es que el proveedor **no existe**, no
// que exista y no sepa el algoritmo. Por eso lleva dos constructores y no cuatro — nunca se arma
// envolviendo otra causa, porque no hay operacion que haya fallado por debajo: es una busqueda en
// una tabla que no encontro nada.
public class NoSuchProviderException extends GeneralSecurityException {

    public NoSuchProviderException() {
        super();
    }

    public NoSuchProviderException(String message) {
        super(message);
    }
}
