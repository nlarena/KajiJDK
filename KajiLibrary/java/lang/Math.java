package java.lang;

// KajiLibrary's java.lang.Math — the int operations the VM intrinsifies to CPU
// instructions (HotSpot does the same); routed through the native bridge.
public final class Math {

    // Non-instantiable: Math is a static-only utility (matches the JDK, which hides
    // the constructor). Without this, javac would synthesize a *public* default one.
    private Math() {}

    public static native int abs(int a);

    public static native int max(int a, int b);

    public static native int min(int a, int b);
}
