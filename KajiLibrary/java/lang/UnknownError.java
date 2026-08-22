package java.lang;

// KajiLibrary's java.lang.UnknownError — a serious failure occurred in the VM but the VM
// could not classify it. The catch-all of last resort; in practice almost never seen.
public class UnknownError extends VirtualMachineError {

    public UnknownError() {
    }

    public UnknownError(String message) {
        super(message);
    }
}
