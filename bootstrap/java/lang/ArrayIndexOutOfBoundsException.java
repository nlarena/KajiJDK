package java.lang;

// Minimal ArrayIndexOutOfBoundsException — thrown by an out-of-range array access.
// (Real hierarchy goes through IndexOutOfBoundsException; we flatten it for now.)
public class ArrayIndexOutOfBoundsException extends RuntimeException {
    public ArrayIndexOutOfBoundsException() {
    }
}
