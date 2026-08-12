package java.lang;

// java.lang.Character — the char box (JLS §5.1.7). Cache covers 0..127 (ASCII),
// the range the JLS requires to box to identical objects.
public final class Character {
    public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");

    private final char value;

    public Character(char value) {
        this.value = value;
    }

    private static class CharacterCache {
        static final Character[] cache = new Character[128];

        static {
            for (int i = 0; i < 128; i++) {
                cache[i] = new Character((char) i);
            }
        }
    }

    public static Character valueOf(char c) {
        if (c <= 127) {
            return CharacterCache.cache[c];
        }
        return new Character(c);
    }

    public char charValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o instanceof Character) {
            return value == ((Character) o).value;
        }
        return false;
    }

    public int hashCode() {
        return value;
    }
}
