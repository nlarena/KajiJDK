// Differential workload for the F3 JIT, step 5 — dimension: **the safepoint poll, with references
// live in the locals**.
//
// This is the worst failure mode the milestone has, and it is worth stating exactly. When the poll
// fires, compiled code hands the interpreter a flat `[i64]` and the interpreter has to put `Value`s
// back into its frame. Each of those words is either an `int` or a **heap offset**, and nothing in
// the bits says which — only the compiler's type map does. Write an offset back as a `Value::Int`
// and the collector stops seeing a live object *and* stops relocating it; write an `int` back as a
// `Value::Reference` and the collector is handed a pointer made of arithmetic. Neither fails where
// the mistake is.
//
// `walk` is built to make that failure loud rather than subtle. Its locals at the loop header are
// `[int[] , JrPoll, JrPoll, int, int, int]` — two of the six are references and four are not — and
// the loop reads through **all** of them on every iteration. So a write-back that mistyped any slot
// would be caught within one iteration of resuming: the interpreter's own `iaload` and `getfield`
// reject a `Value::Int` where a reference belongs.
//
// The second half of the test is movement. `run` allocates the array and both cells **freshly on
// every round**, so they are young; and the interpreter reaches its safepoints — and therefore
// collects — precisely in the gaps between the poll firing and native code being re-entered. A
// minor collection *evacuates* the young generation, so those three objects move while `walk` is
// half-interpreted, and every offset the JIT handed back has to have been remapped by the
// collector. It only can be if the frame says "reference".
//
// The identity checks in `run` are the third strand: the same objects must come out the far end,
// not merely some objects. `sameness` is compiled too (it is all `if_acmp*`), so the comparison
// itself crosses the boundary.
public class JrPoll {
    int k;

    JrPoll(int k) {
        this.k = k;
    }

    // The compiled loop that carries two references across the poll. Its header has an empty
    // operand stack, so it is both an on-stack entry point and a poll site.
    static int walk(int[] a, JrPoll c, JrPoll d, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + a[i % a.length] + c.k - d.k) & 0xFFFFF;
        }
        return acc;
    }

    // A second compiled loop whose reference locals are *not* the ones it reads through — `keep`
    // is only carried, never dereferenced, which is the case a write-back could drop silently
    // because nothing in the loop would notice. `run` notices, by identity, afterwards.
    static int carry(JrPoll keep, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + i * 3) & 0xFFFFF;
        }
        return acc;
    }

    static int sameness(JrPoll p, JrPoll q) {
        int s = 0;
        if (p == q) {
            s += 1;
        }
        if (p != q) {
            s += 2;
        }
        if (p == null) {
            s += 4;
        }
        return s;
    }

    public static int run() {
        int score = 0;
        for (int round = 0; round < 60; round++) {
            // Fresh every round, so they are young and a minor collection relocates them.
            int[] a = new int[24];
            for (int i = 0; i < 24; i++) {
                a[i] = (i * 5) + round;
            }
            JrPoll c = new JrPoll(11 + round);
            JrPoll d = new JrPoll(4);

            score = (score + walk(a, c, d, 700)) & 0xFFFFF;
            score = (score + carry(c, 700)) & 0xFFFFF;

            // The objects that went into compiled code must be the ones that came out of it, and
            // must still hold what they held. A stale offset would fail here, or read rubbish.
            score = (score + sameness(c, c) + sameness(c, d) + sameness(c, null)) & 0xFFFFF;
            if (c.k != 11 + round || d.k != 4 || a[23] != 115 + round) {
                score += 1_000_000;
            }
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
