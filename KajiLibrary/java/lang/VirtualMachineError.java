package java.lang;

// KajiLibrary's java.lang.VirtualMachineError — the abstract parent of "the VM is broken or
// out of a resource it needs". These are not application bugs to be caught and handled: by
// the time one is thrown the VM has already failed to do its job, and the state of the
// program is generally not recoverable.
public abstract class VirtualMachineError extends Error {

    public VirtualMachineError() {
    }

    public VirtualMachineError(String message) {
        super(message);
    }

    public VirtualMachineError(String message, Throwable cause) {
        super(message, cause);
    }

    public VirtualMachineError(Throwable cause) {
        super(cause);
    }
}
