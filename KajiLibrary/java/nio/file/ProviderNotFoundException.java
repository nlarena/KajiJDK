package java.nio.file;

// No hay ningun proveedor instalado para el esquema que se pidio.
//
// KajiJDK tiene exactamente uno --el de `file`-- y no hay mecanismo de servicios que instale otros,
// asi que cualquier otro esquema termina aca.
public class ProviderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -1880012509822920354L;

    /** Sin mensaje. */
    public ProviderNotFoundException() {
    }

    /** @param msg el detalle */
    public ProviderNotFoundException(String msg) {
        super(msg);
    }
}
