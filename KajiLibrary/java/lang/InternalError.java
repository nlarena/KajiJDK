package java.lang;

// KajiLibrary's java.lang.InternalError — the VM detected an inconsistency in itself. Unlike
// the other VirtualMachineErrors it always means a bug in the VM (or in a native library),
// never a legitimate limit the program hit.
public class InternalError extends VirtualMachineError {

    public InternalError() {
    }

    public InternalError(String message) {
        super(message);
    }

    public InternalError(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalError(Throwable cause) {
        super(cause);
    }
}
