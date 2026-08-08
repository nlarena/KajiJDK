package java.lang;

// KajiLibrary's java.lang.RuntimeException — the unchecked branch (§11.2): thrown at
// runtime by the VM or by code, and not required to be declared or caught.
public class RuntimeException extends Exception {

    public RuntimeException() {
    }

    public RuntimeException(String message) {
        super(message);
    }
}
