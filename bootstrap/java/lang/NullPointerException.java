package java.lang;

// Minimal NullPointerException — the VM synthesizes and throws one when code uses
// a null reference (null receiver of getfield/invoke*, null array, etc.).
public class NullPointerException extends RuntimeException {
    public NullPointerException() {
    }
}
