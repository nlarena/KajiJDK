package java.util;

// A character conversion was given a value that is not a valid code point.
public class IllegalFormatCodePointException extends IllegalFormatException {

    private final int codePoint;

    public IllegalFormatCodePointException(int c) {
        this.codePoint = c;
    }

    public int getCodePoint() {
        return codePoint;
    }

    public String getMessage() {
        return "Code point = 0x" + Integer.toHexString(codePoint);
    }
}
