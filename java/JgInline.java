// F3 step 10 — **a deopt inside inlined code, with operands live in registers in *both* frames.**
//
// Step 8 flattened a call chain into one native frame and made a deopt rebuild it. Step 10 puts
// the operand stacks of those frames into registers — and, because a body's operands are addressed
// by their *native* slot index, the caller's and the callee's occupy disjoint registers rather than
// competing for them. A stub inside the callee therefore walks the caller chain and, at each level,
// spills that frame's live operands from that frame's own registers.
//
// `outer` is arranged so both halves are exercised at once: three of its own operands are still
// live under the invoke, and the callee's stack straddles the end of the cache — some of its
// positions are registers, one is a frame slot. If a frame's spill named the wrong register the
// rebuilt chain computes a different sum, or the divisor the rebuilt callee re-reads is not zero
// and the `catch` never runs.
public class JgInline {
    // Inlined into `outer`. Its own operand stack sits above the caller's, so its positions are the
    // ones nearest the end of the cache.
    static int inner(int p, int q, int r) {
        return p + (q + (r + (p / q)));
    }

    static int outer(int a, int b, int c, int k) {
        return a + (b + (c + inner(a, k, c)));
    }

    // A second level, so a deopt has to rebuild two frames above the root.
    static int middle(int a, int b, int k) {
        return a + (b + outer(a, b, a, k));
    }

    static int run() {
        int acc = 0;
        for (int i = 1; i <= 3000; i++) {
            int a = i & 0x3F;
            int b = (i >> 3) & 0x1F;
            int c = (i * 5) & 0x7F;
            try {
                acc = (acc + outer(a, b, c, i % 6)) & 0xFFFFF;
            } catch (ArithmeticException ex) {
                acc = (acc + 19) & 0xFFFFF;
            }
            try {
                acc = (acc + middle(a, b, i % 10)) & 0xFFFFF;
            } catch (ArithmeticException ex) {
                acc = (acc + 23) & 0xFFFFF;
            }
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
