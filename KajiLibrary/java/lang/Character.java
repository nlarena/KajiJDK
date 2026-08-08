package java.lang;

// KajiLibrary's java.lang.Character — the boxed-char wrapper. Like Boolean (and unlike the
// numeric wrappers) it extends Object, not Number, but it implements Comparable (chars
// order by code-unit value). `valueOf`/`charValue` are the boxing/unboxing hooks. The range
// spans the whole 16-bit code-unit space (a char is unsigned): 0 .. 0xffff.
public final class Character implements Comparable<Character> {

    public static final char MIN_VALUE = (char) 0;

    public static final char MAX_VALUE = (char) 0xffff;

    private final char value;

    public Character(char value) {
        this.value = value;
    }

    public static Character valueOf(char c) {
        return new Character(c);
    }

    public char charValue() {
        return value;
    }

    public int compareTo(Character o) {
        return this.value - o.value;
    }
}
