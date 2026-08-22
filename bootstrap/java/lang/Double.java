package java.lang;

// java.lang.Double — the double box (JLS §5.1.7). No cache: floating boxes are
// always fresh (the JLS never demands identity for them). equals/hashCode are
// omitted for now — bit-exact semantics need doubleToLongBits, a native we
// don't have yet; identity equals from Object suffices until then.
public final class Double {
    public static final Class<Double> TYPE = (Class<Double>) Class.getPrimitiveClass("double");

    private final double value;

    public Double(double value) {
        this.value = value;
    }

    public static Double valueOf(double d) {
        return new Double(d);
    }

    public double doubleValue() {
        return value;
    }
}
