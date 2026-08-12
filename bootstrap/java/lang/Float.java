package java.lang;

// java.lang.Float — the float box (JLS §5.1.7). No cache (see Double).
public final class Float {
    public static final Class<Float> TYPE = (Class<Float>) Class.getPrimitiveClass("float");

    private final float value;

    public Float(float value) {
        this.value = value;
    }

    public static Float valueOf(float f) {
        return new Float(f);
    }

    public float floatValue() {
        return value;
    }
}
