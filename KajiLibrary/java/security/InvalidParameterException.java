package java.security;

// Un parametro invalido pasado a un metodo del paquete.
//
// Es de las pocas del paquete que **no** hereda de `GeneralSecurityException`: hereda de
// `IllegalArgumentException`, y eso no es un accidente historico sino la distincion util. Un
// argumento invalido es un error de quien llama y se arregla cambiando el codigo, asi que no tiene
// por que ser chequeado. Un algoritmo ausente o una firma que no se puede procesar son estados del
// mundo, y esos si.
public class InvalidParameterException extends IllegalArgumentException {

    public InvalidParameterException() {
        super();
    }

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParameterException(Throwable cause) {
        super(cause);
    }
}
