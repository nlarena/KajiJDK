// Baseline workload — dimension: **calls** (`invokestatic`).
//
// Recursive fib in a tight loop: the body is trivial arithmetic, so almost all of the
// non-trivial work is the call itself — resolving the constant-pool methodref, building
// the callee frame, and returning. This is the workload that pays the full price of
// re-resolving a call site on *every* execution (the `(class, index)` cache is hit, but
// the hit still allocates the key), so contrasting its ns/opcode against BmLoop's bounds
// what one call costs today. No allocation → no GC.
public class BmInvoke {
    static int bmFib(int n) {
        if (n < 2) {
            return n;
        }
        return bmFib(n - 1) + bmFib(n - 2);
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 96; i++) {
            acc = (acc + bmFib(18) + i) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
