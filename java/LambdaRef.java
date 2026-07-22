// A lambda that captures a **reference**, with a garbage collection in between. The
// capture lives in a synthetic class the VM mints, which has no class file — so the only
// reason the collector can see it is that the class declares its reference layout.
//
// Without that, the captured String is never marked (it could be collected out from under
// the lambda) and never rewritten when a moving collection relocates it.
public class LambdaRef {
    public static int run() {
        String greeting = "hola";
        Sizer f = s -> (s + greeting).length();

        if (f.apply("a") != 5) return -1; // "ahola"

        // Move things: the capture must survive *and* be updated to the new address.
        System.gc();

        if (f.apply("a") != 5) return -2;
        if (f.apply("bb") != 6) return -3;

        return 42;
    }
}

interface Sizer {
    int apply(String s);
}
