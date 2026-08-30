// Differential workload for the F3 JIT — dimension: **`ldc` of a `String` literal**.
//
// Refused by the compiler until the string pool landed (FZ-008), and for a reason that was about
// the VM rather than the compiler: `strings::intern` allocated a fresh `String` in Eden on every
// `ldc`, so there was no permanent offset to bake — and baking one would have made `"a" == "a"`
// answer `true` in compiled code and `false` in the interpreter, which is worse than a method that
// does not compile.
//
// What this asks is exactly that and nothing else: the two arms must agree that two `ldc`s of one
// literal are **the same object**. The comparison goes through locals because `javac` folds
// `("a" == "a")` written inline and the VM would never see it — the mistake that hid FZ-008.
//
// Deliberately free of anything else about `String`: this harness boots from `boot/`, not from
// `KajiLibrary`, so a probe touching `new String(…)` or `equals` would be measuring which of the
// two class libraries is on the path rather than what the compiler did with an `ldc`.
public class LdcStr {
    static int probe(int i) {
        String a = "kaji";
        String b = "kaji";
        String c = "otro";
        int r = 0;
        if (a == b) { r += 1; }   // two `ldc`s of one literal: the same instance
        if (a != c) { r += 2; }   // a different literal: a different one
        return r + (i & 7);
    }

    public static int run() {
        int acc = 0;
        for (int i = 0; i < 400; i++) { acc = ((acc * 31) + probe(i)); }
        return acc;
    }

    public static void main(String[] a) { System.out.println(run()); }
}
