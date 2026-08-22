package java.lang;

// KajiLibrary's java.lang.StrictMath.
//
// StrictMath exists for one reason: REPRODUCIBILITY. java.lang.Math is allowed to use whatever
// the hardware offers — a platform's sin() may differ in the last bit from another's, and the
// JIT may substitute an intrinsic — whereas StrictMath is contractually bound to produce the
// same bits everywhere, because every transcendental in it is specified to follow the published
// fdlibm algorithms. Two machines that agree on StrictMath.sin(x) do so by construction, not by
// luck; that is what makes it usable in a checksum, a replayed simulation, or a consensus
// protocol.
//
// WHICH IS PRECISELY WHY THIS CLASS IS TINY. KajiLibrary's Math currently offers three integer
// operations (abs, max, min), and those are exact — there is one right answer, so delegating is
// genuinely strict. Everything else in the JDK's StrictMath is floating point, and we have no
// fdlibm port. Writing our own sin() and calling it StrictMath.sin would be a LIE in the API:
// the name is a promise about bit patterns, and code that trusted it would silently disagree
// with every other JVM. So the floating-point surface is omitted rather than approximated, and
// stays omitted until an fdlibm port exists.
//
// The constants E, PI and TAU are omitted for a second, separate reason: compiler finding #112
// makes `static final` primitives read back as zero, so declaring them would be worse than not
// having them.
public final class StrictMath {

    // Non-instantiable: a static-only utility, exactly as the JDK hides it. Without this,
    // javac would synthesize a *public* default constructor.
    private StrictMath() {
    }

    // The integer operations, forwarded to Math. Integer abs/max/min have a single exact
    // answer on every platform, so "strict" and "fast" are the same function here — the JDK
    // forwards them for the same reason.
    public static int abs(int a) {
        return Math.abs(a);
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }
}
