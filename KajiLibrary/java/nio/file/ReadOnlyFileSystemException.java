package java.nio.file;

// Se intento escribir en un sistema de archivos montado de solo lectura.
//
// **Hereda de `UnsupportedOperationException`, no de `IOException`**, y la distincion es util: no es
// que la escritura fallo, es que en ese sistema **no existe** la operacion. Reintentar no tiene
// sentido.
public class ReadOnlyFileSystemException extends UnsupportedOperationException {

    private static final long serialVersionUID = -6822409595617487197L;

    /** Sin mensaje. */
    public ReadOnlyFileSystemException() {
    }
}
