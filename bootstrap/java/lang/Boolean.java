package java.lang;

// java.lang.Boolean — the boolean box (JLS §5.1.7). Only two values exist, so
// valueOf always returns one of the canonical TRUE/FALSE instances.
public final class Boolean {
    public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean");

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

    public boolean equals(Object o) {
        if (o instanceof Boolean) {
            return value == ((Boolean) o).value;
        }
        return false;
    }

    public int hashCode() {
        return value ? 1231 : 1237;
    }
}
