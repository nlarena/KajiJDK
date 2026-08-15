// Differential workload for the F3 JIT, step 5 — dimension: **references**.
//
// Until this step the compiled subset could not name a reference at all, so `aload_0` — the first
// byte of every instance method — put 354 of the corpus's 710 methods out of reach. This file is
// the correctness half of taking that back, and every method in it is chosen for a question that
// only arises once a heap offset can sit in a local slot:
//
//   * does `getfield` read the **right four bytes**? `getOwn`, `getBase` and `getAfter` read three
//     fields of one object — one declared here, one **inherited** (superclass fields are laid out
//     first, so an offset folded from the wrong class is off by the whole prefix), and one sitting
//     **past a `long`**, whose 8-alignment inserts a padding slot that a naive walk would miss.
//     `pad` sits next to `base` for the same reason `WdStatic` has a neighbour: an off-by-one slot
//     shows up as someone else's value rather than as a crash.
//   * is a **null receiver** handled? `nullField` is called with `null` on every round. Compiled
//     code raises no exception — it gives up, the interpreter re-runs the method and throws the
//     `NullPointerException`, and the Java `catch` sees an ordinary one. That the score counts
//     those catches is the proof that a deopt is invisible to the program.
//   * do `arraylength` and `iaload` agree with the interpreter, **including their bounds**? `sum`
//     walks an array to its length (and is the file's one compiled *loop*, so it is also entered
//     on-stack); `at` is called in range, one before the start and one past the end.
//   * are **references compared as identities**? `nullness` asks all four of `ifnull`,
//     `ifnonnull`, `if_acmpeq` and `if_acmpne` about the same pair.
//   * can a method **return** a reference? `pick` is the first `areturn` in the subset's history,
//     and `run` checks *which* object came back, not merely that one did — a returned offset that
//     was off by a slot would still be a valid reference to something else.
//
// Nothing here writes the heap from compiled code: `putfield` and `iastore` stay outside the
// subset, for the same reason `putstatic` does (see `burst::compile`'s module docs). Every write
// below happens in a constructor or in `run`, neither of which is ever compiled.
class JrBase {
    int base;
    int pad; // sits next to `base` in the layout: catches an off-by-one slot

    JrBase(int b) {
        this.base = b;
        this.pad = 999_999;
    }
}

public class JrRef extends JrBase {
    int own;
    long wide; // category-2: 8-aligned, so it may insert a padding slot before itself
    int after; // ...and `after` therefore sits past that padding

    JrRef(int b, int o) {
        super(b);
        this.own = o;
        this.wide = 7L;
        this.after = o * 3;
    }

    // `aload_0; getfield; ireturn` — the shape that was unreachable before this step.
    int getOwn() {
        return this.own;
    }

    // Inherited: the field is declared in JrBase, whose fields the layout puts first.
    int getBase() {
        return this.base;
    }

    // Declared after a `long`, so its offset includes the 8-alignment padding.
    int getAfter() {
        return this.after;
    }

    // Three field reads and arithmetic between them, so a single wrong offset cannot cancel out.
    int mix(int x) {
        return ((x + this.own) * this.base - this.after) & 0xFFFFF;
    }

    // Called with `null` every round: compiled code deopts, the interpreter throws.
    static int nullField(JrRef r) {
        return r.own;
    }

    // `arraylength` in the loop condition and `iaload` in the body — and the only loop here, so
    // this is the method that gets entered **on-stack** with a reference live in local 0.
    static int sum(int[] a) {
        int acc = 0;
        for (int i = 0; i < a.length; i++) {
            acc = (acc + a[i]) & 0xFFFFF;
        }
        return acc;
    }

    // In range on most calls, and off both ends on the rest.
    static int at(int[] a, int i) {
        return a[i];
    }

    // All four reference comparisons about one pair of references.
    static int nullness(Object p, Object q) {
        int s = 0;
        if (p == null) {
            s += 1;
        }
        if (p != null) {
            s += 2;
        }
        if (p == q) {
            s += 4;
        }
        if (p != q) {
            s += 8;
        }
        return s;
    }

    // The first `areturn`: a reference leaves compiled code in the same 32 bits an `int` does.
    static Object pick(Object p, Object q, int which) {
        if (which == 0) {
            return p;
        }
        if (which == 1) {
            return q;
        }
        return null;
    }

    public static int run() {
        JrRef r = new JrRef(5, 9);
        JrRef s = new JrRef(6, 10);
        int[] a = new int[64];
        for (int i = 0; i < 64; i++) {
            a[i] = (i * 7) - 13;
        }

        int score = 0;
        for (int round = 0; round < 400; round++) {
            score = (score + r.getOwn() + s.getBase() + r.getAfter() + r.mix(round)) & 0xFFFFF;
            score = (score + sum(a) + at(a, round % 64)) & 0xFFFFF;
            score = (score + nullness(r, s) + nullness(null, null) + nullness(r, r)) & 0xFFFFF;

            // Identity of a returned reference, not merely its non-nullness.
            Object picked = pick(r, s, round % 3);
            if (picked == r) {
                score += 1;
            } else if (picked == s) {
                score += 2;
            } else if (picked == null) {
                score += 4;
            } else {
                score += 1_000_000; // a reference to something else entirely
            }

            // The three deopt sites, each caught as the ordinary exception it must become.
            try {
                score += nullField(null);
            } catch (NullPointerException e) {
                score += 8;
            }
            try {
                score += at(a, -1);
            } catch (ArrayIndexOutOfBoundsException e) {
                score += 16;
            }
            try {
                score += at(a, 64);
            } catch (ArrayIndexOutOfBoundsException e) {
                score += 32;
            }
            score &= 0xFFFFF;
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
