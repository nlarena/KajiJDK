// Probes for the **string pool** (JLS 3.10.5), through the whole VM rather than through the
// pool's own API: what a Java program can observe about the identity of a literal.
//
// Every question is asked through a *method call* or a *static field*, never inline, and that is
// the point of the shape rather than a style choice. `javac` folds `("a" == "a")` written inline
// straight to `iconst_0`/`iconst_1`: the method then compiles with no `ldc` in it at all and the
// probe passes without the VM ever being asked anything. That is precisely how FZ-008 stayed open
// for as long as it did (FZ-009), so every method here is written so `javac` cannot answer it, and
// `javap -c` shows the `ldc` and the `if_acmpne` that prove it.
//
// The expected answers are `java` 25's, measured and not assumed.
public class JsPool {
    /** Reached by `getstatic`, so the second literal arrives from somewhere `javac` cannot fold. */
    static String field = "kajiJsPooled";

    /** The same literal, in a different method, so the two `ldc`s are in different code. */
    static String literal() {
        return "kajiJsPooled";
    }

    /** Two `ldc`s of one literal are ONE object -- the whole of JLS 3.10.5. `java` says 1. */
    public static int sameLiteral() {
        String a = literal();
        String b = field;
        return a == b ? 1 : 0;
    }

    /**
     * A String the program COMPUTES is never the pooled one, even with equal contents. `java`
     * says 0, and a pool that swallowed computed strings would say 1 -- which is as wrong as
     * not pooling at all, in the other direction.
     */
    public static int computedIsFresh() {
        String a = literal();
        String b = new String(a);
        return a == b ? 1 : 0;
    }

    /** `intern()` on a computed copy hands the literal back. `java` says 1. */
    public static int internRoundTrip() {
        String a = literal();
        String b = new String(a).intern();
        return a == b ? 1 : 0;
    }

    /**
     * A NATIVE that builds a String hands back a fresh one each time. `System.mapLibraryName`
     * concatenates a suffix onto its argument, so its result is computed and not a symbol;
     * `java` says 0. It is the half of the classification that lives in `natives.rs` rather
     * than in `ldc`, and the only one a Java program can reach without reflection.
     */
    public static int nativeComputedIsFresh() {
        String a = System.mapLibraryName("kajiJs");
        String b = System.mapLibraryName("kajiJs");
        return a == b ? 1 : 0;
    }

    /** The four as one number, so a single run of `java JsPool` states all of them: 1010. */
    public static int run() {
        return sameLiteral() * 1000
                + computedIsFresh() * 100
                + internRoundTrip() * 10
                + nativeComputedIsFresh();
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
