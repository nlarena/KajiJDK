package java.util;

// The width is negative, or the conversion does not take one.
public class IllegalFormatWidthException extends IllegalFormatException {

    private final int width;

    public IllegalFormatWidthException(int w) {
        this.width = w;
    }

    public int getWidth() {
        return width;
    }

    public String getMessage() {
        return Integer.toString(width);
    }
}
