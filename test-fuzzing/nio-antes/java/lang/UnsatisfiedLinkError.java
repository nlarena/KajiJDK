package java.lang;

// KajiLibrary's java.lang.UnsatisfiedLinkError — a `native` method was called and no native
// implementation was bound to it. In this VM the "native" implementations are Rust intrinsics
// dispatched by name, so this is what an unrecognised intrinsic name amounts to.
public class UnsatisfiedLinkError extends LinkageError {

    public UnsatisfiedLinkError() {
    }

    public UnsatisfiedLinkError(String message) {
        super(message);
    }
}
