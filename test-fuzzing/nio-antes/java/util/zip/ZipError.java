package java.util.zip;

// An unrecoverable problem in the zip machinery. It extends `InternalError` — a
// `VirtualMachineError` — rather than `ZipException`, which says something about intent: a
// malformed archive is a `ZipException` the caller should handle, while this one means the
// implementation itself is in a state it cannot explain.
public class ZipError extends InternalError {

    public ZipError(String message) {
        super(message);
    }
}
