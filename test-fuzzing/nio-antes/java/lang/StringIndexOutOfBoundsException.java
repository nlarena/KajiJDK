package java.lang;

// KajiLibrary's java.lang.StringIndexOutOfBoundsException — the String/StringBuilder flavour of
// an out-of-range index. The dedicated `int` constructor exists so the throw site can be a
// single cheap call: charAt() is hot, and building the message inline would put string
// concatenation on the fast path of a method whose whole job is one array load.
public class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {

    public StringIndexOutOfBoundsException() {
    }

    public StringIndexOutOfBoundsException(String message) {
        super(message);
    }

    public StringIndexOutOfBoundsException(int index) {
        super(describe(index));
    }

    // super(...) cannot touch `this`, so the message is built by a static.
    private static String describe(int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("String index out of range: ");
        sb.append(index);
        return sb.toString();
    }
}
