// Measurement workload for the F3 JIT — dimension: **a hot frame-local method**.
//
// The body of `mix` is `BmLoop`'s loop, verbatim; the difference is the shape of the *call graph*
// around it. `BmLoop.run()` is entered once and loops 900 000 times inside, which an invocation
// counter can never see — this tier compiles at method entry and has no on-stack replacement, so
// `BmLoop` measures the interpreter no matter how hot its loop gets. `JtLoop` splits the same
// 900 000 iterations across 3 000 calls, which is what a method-granularity JIT is able to notice.
//
// So the pair is the experiment: `JtLoop` is the treatment (its inner method compiles), `BmLoop` is
// its own control (identical arithmetic, nothing compiled). Any change that moves both moved the
// code layout, not the JIT.
public class JtLoop {
    static int mix(int seed, int n) {
        int acc = seed;
        for (int i = 0; i < n; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 7);
            if ((i & 15) == 0) {
                acc = acc - 3;
            }
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        int acc = 1;
        for (int k = 0; k < 3000; k++) {
            acc = mix(acc, 300);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
