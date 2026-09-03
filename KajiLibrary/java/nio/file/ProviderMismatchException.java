package java.nio.file;

// Se mezclo un objeto de un proveedor con otro que no es el suyo -- por ejemplo un `Path` de un ZIP
// pasado a un metodo del sistema de archivos por omision.
//
// **No es una `IOException`.** Hereda de `IllegalArgumentException` porque el error esta en el
// argumento y se detecta sin tocar el disco. En KajiJDK aparece cuando a un metodo que espera un
// `Path` de esta biblioteca le llega una implementacion ajena.
public class ProviderMismatchException extends IllegalArgumentException {

    private static final long serialVersionUID = 4990847485741612530L;

    /** Sin mensaje. */
    public ProviderMismatchException() {
    }

    /** @param msg el detalle */
    public ProviderMismatchException(String msg) {
        super(msg);
    }
}
