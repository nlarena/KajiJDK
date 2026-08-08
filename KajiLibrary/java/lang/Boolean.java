package java.lang;

// KajiLibrary's java.lang.Boolean — the boxed-boolean wrapper. Unlike the numeric wrappers
// it extends Object (a boolean is not a Number), but it does implement Comparable, with
// false ordered before true. `valueOf` hands back the two shared, immutable instances.
public final class Boolean implements Comparable<Boolean> {

    public static final Boolean TRUE = new Boolean(true);

    public static final Boolean FALSE = new Boolean(false);

    private final boolean value;

    public Boolean(boolean value) {
        this.value = value;
    }

    public static Boolean valueOf(boolean b) {
        return b ? TRUE : FALSE;
    }

    public boolean booleanValue() {
        return value;
    }

    public int compareTo(Boolean o) {
        return this.value == o.value ? 0 : (this.value ? 1 : -1);
    }
}
