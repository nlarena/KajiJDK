package java.lang;

// KajiLibrary's java.lang.VerifyError — the class file parses, but the bytecode verifier
// rejected it: the operand stack does not type-check, a local is read as the wrong type, a
// branch lands mid-instruction. The verifier is what lets the interpreter skip those checks
// at run time, so failing it is fatal to the class.
public class VerifyError extends LinkageError {

    public VerifyError() {
    }

    public VerifyError(String message) {
        super(message);
    }
}
