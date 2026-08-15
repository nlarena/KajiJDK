// F3 step 10 — **branches and merges with the operand stack non-empty.**
//
// The usual worry about caching the operand stack in registers is the *edge*: two paths reach a
// merge and have to agree about which value is in which register, so a local allocator spills at
// every branch and every join. This allocator has nothing to reconcile, and these programs are what
// says so from the outside.
//
// The reason is that the mapping is from an operand's **position**, not from a value, and the
// operand-stack depth at a pc is already single-valued — [`scan_body`] recomputes it and refuses
// any method where two paths disagree ([`Ineligible::StackMismatch`]). So both arms of a ternary
// leave their result in the same place because they leave it at the same *depth*, and no spill,
// no reload and no reconciliation is emitted at all.
//
// The three shapes below put a merge at a cached position, at the last cached position, and one
// past the end of the cache — the last being where an off-by-one in the mapping would show up as
// a value read from a register that a branch left in a frame slot, or the reverse.
public class JgEdge {
    // A merge at position 3, comfortably inside the cache.
    static int near(int a, int b, int c, int d) {
        return a + (b + (c + (a > b ? c * d : d - c)));
    }

    // A merge at position 7 — the last register the cache has.
    static int edge(int a, int b, int c, int d, int e) {
        return a + (b + (c + (d + (e + (a + (b + (c > d ? e * a : e - a)))))));
    }

    // A merge at position 8 — the first position that is a frame slot.
    static int past(int a, int b, int c, int d, int e) {
        return a + (b + (c + (d + (e + (a + (b + (c + (c > d ? e * a : e - a))))))));
    }

    // Two merges in one expression, at two different depths, plus a loop around them so the method
    // is entered on-stack as well as from the top.
    static int both(int a, int b, int c, int d, int e, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + near(a + i, b, c, d) + edge(a, b + i, c, d, e)) & 0xFFFFF;
        }
        return acc;
    }

    static int run() {
        int acc = 0;
        for (int i = 1; i <= 2000; i++) {
            int a = i & 0x3F;
            int b = (i >> 2) & 0x1F;
            int c = (i * 7) & 0xFF;
            int d = (i & 0x0F) + 1;
            int e = (i >> 5) & 0x3F;
            acc = (acc + near(a, b, c, d)) & 0xFFFFF;
            acc = (acc + edge(a, b, c, d, e)) & 0xFFFFF;
            acc = (acc + past(a, b, c, d, e)) & 0xFFFFF;
            acc = (acc + both(a, b, c, d, e, 20)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
