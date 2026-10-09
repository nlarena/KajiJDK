package java.io;

import java.io.IOException;

// Thrown when a charset is named by String and no such charset is installed. It is checked
// rather than unchecked because the name usually comes from configuration or a protocol
// header, i.e. from outside the program, where the compiler cannot vouch for it.
public class UnsupportedEncodingException extends IOException {

    public UnsupportedEncodingException() {
    }

    public UnsupportedEncodingException(String message) {
        super(message);
    }
}
