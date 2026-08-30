import java.util.Enumeration;
import java.util.NoSuchElementException;

// Enumeration sobre un String[]. Una dimension, asi que compila con nuestro javac.
public class KeyEnum implements Enumeration<String> {
    private final String[] items;
    private int i;

    public KeyEnum(String[] items) {
        this.items = items;
        this.i = 0;
    }

    public boolean hasMoreElements() {
        return this.i < this.items.length;
    }

    public String nextElement() {
        if (this.i >= this.items.length) {
            throw new NoSuchElementException();
        }
        String s = this.items[this.i];
        this.i = this.i + 1;
        return s;
    }
}
