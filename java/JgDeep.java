// F3 step 10 — **expressions deeper than the register cache**, and the two x86-64 instructions
// with fixed registers sitting exactly on its edge.
//
// The operand stack's lowest eight positions live in R8-R15; everything above stays in a frame
// slot. So the interesting programs are not the ones that fit — those are the easy half — but the
// ones that straddle the boundary, where one operand of an instruction is a register and the other
// is memory. Every expression below is right-associated on purpose: `a + (b + (c + ...))` pushes
// one operand per nesting level before the first `iadd`, so the parenthesisation *is* the stack
// depth, and ten levels put two positions past the cache.
//
// `mixed` is the one that would have been hard under a different register choice. At its `idiv`
// the stack is nine deep: the dividend is the last cached position and the divisor the first
// uncached one, and `idiv` clobbers RDX:RAX implicitly. At its `ishl` the same two positions meet
// `CL`. Neither RAX, RCX nor RDX is ever an operand's home, so nothing has to be moved out of the
// way — and this program is what says so from the outside, by computing the same number the
// interpreter does.
public class JgDeep {
    // Ten operands live at once — two past the eight the cache holds.
    static int deep(int a, int b, int c, int d, int e) {
        return a + (b + (c + (d + (e + (a + (b + (c + (d + e))))))));
    }

    // `/`, `%`, `<<` and `>>>` each in the middle of an expression with seven live operands under
    // them: the fixed-register instructions, at the register/memory boundary.
    static int mixed(int a, int b, int c, int d, int e) {
        int q = a + (b + (c + (d + (e + (a + (b + (c / d)))))));
        int r = a + (b + (c + (d + (e + (a + (b + (c % d)))))));
        int s = a + (b + (c + (d + (e + (a + (b + (c << d)))))));
        int u = a + (b + (c + (d + (e + (a + (b + (c >>> d)))))));
        return (q ^ r) + (s ^ u);
    }

    // The same two shapes one level shallower, so the fixed-register instruction's operands are
    // *both* cached — the case the boundary version has to agree with.
    static int shallow(int a, int b, int c, int d) {
        int q = a + (b + (c / d));
        int s = a + (b + (c << d));
        return q + s + (a - b) * (c | d);
    }

    static int run() {
        int acc = 0;
        for (int i = 1; i <= 4000; i++) {
            int a = i & 0x3F;
            int b = (i >> 2) & 0x1F;
            int c = (i * 7) & 0xFF;
            int d = (i & 0x0F) + 1;
            int e = (i >> 5) & 0x3F;
            acc = (acc + deep(a, b, c, d, e)) & 0xFFFFF;
            acc = (acc + mixed(a, b, c, d, e)) & 0xFFFFF;
            acc = (acc + shallow(a, b, c, d)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
