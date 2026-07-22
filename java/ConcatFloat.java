// Pins the formatting of floating-point arguments spliced by an `invokedynamic`
// concatenation. The call site descriptor is `(DF)Ljava/lang/String;` — the double and
// the float arrive raw, so the VM itself must render them exactly as Java does.
// Returns 42 when both match; otherwise a code saying which one diverged.
public class ConcatFloat {
    public static int run() {
        double d = 1.0;
        float f = 1.0f;
        if (!("d=" + d).equals("d=1.0")) return -1;
        if (!("f=" + f).equals("f=1.0")) return -2;
        return 42;
    }
}
