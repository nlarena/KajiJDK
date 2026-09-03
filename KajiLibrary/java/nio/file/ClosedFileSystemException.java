package java.nio.file;

// Se uso un `FileSystem` que ya estaba cerrado.
//
// El de KajiJDK --`FileSystems.getDefault()`-- no se puede cerrar: `close()` no hace nada y
// `isOpen()` es siempre `true`, igual que el por omision del JDK. Asi que esta excepcion nunca sale
// de aca; existe para el codigo que la atrapa y para los proveedores que si se cierran.
public class ClosedFileSystemException extends IllegalStateException {

    private static final long serialVersionUID = -8158336077256193488L;

    /** Sin mensaje. */
    public ClosedFileSystemException() {
    }
}
