package java.lang;

// Minimal java.lang.Math — the int methods, native (HotSpot intrinsifies these to
// CPU instructions; we route them through the bridge).
public final class Math {
    public static native int abs(int a);

    public static native int max(int a, int b);

    public static native int min(int a, int b);
}
