package java.lang;

// Minimal IllegalArgumentException — thrown by an enum's synthetic `valueOf(String)`
// when the given name matches no constant (JLS §8.9.3).
public class IllegalArgumentException extends RuntimeException {
    public IllegalArgumentException() {
    }
}
