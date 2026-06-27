package java.lang;

// Minimal java.lang.Integer — just the bit-twiddling statics, native (they map to
// CPU instructions: popcnt, lzcnt).
public final class Integer {
    public static native int bitCount(int i);

    public static native int numberOfLeadingZeros(int i);
}
