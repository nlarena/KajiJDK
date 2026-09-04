package java.security;

// Un proveedor criptografico fallo por dentro.
//
// Es **no chequeada** a proposito, y es la unica del paquete que lo es sin ser un error de
// argumento: señala que la implementacion esta rota —no que el llamador pidio algo imposible— y
// obligar a declararla en cada firma no le daria a nadie una forma de recuperarse. El que la
// atrapa no puede hacer otra cosa que abortar.
public class ProviderException extends RuntimeException {

    public ProviderException() {
        super();
    }

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderException(Throwable cause) {
        super(cause);
    }
}
