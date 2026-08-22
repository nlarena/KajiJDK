// Differential workload for the F3 JIT, step 4 — dimension: **`tableswitch` and `lookupswitch`**.
//
// A switch is the first multi-way branch in the compiled subset, and three details of it are easy
// to get wrong in a way that ordinary cases never notice:
//
//   * the **padding** — 0 to 3 bytes that align the operand table to a 4-byte boundary *of the code
//     array*, so whether it is there at all depends on where the opcode happens to land. The two
//     switch methods below sit at different offsets inside their bodies for exactly that reason;
//   * the **default** arm, which is a branch target like any other. Every method here is called
//     with keys that miss;
//   * the **boundaries** — the first case, the last case, one below the first, one above the last,
//     and (for `lookup`) `Integer.MIN_VALUE`/`MAX_VALUE`, where a signed/unsigned comparison or an
//     off-by-one in the range check would show up and nowhere else.
//
// Everything here stays inside the subset: int keys, int arithmetic, int returns.
public class WdSwitch {
    // Dense and contiguous from 0 — javac emits `tableswitch`.
    static int table(int k) {
        switch (k) {
            case 0: return 10;
            case 1: return 20;
            case 2: return 30;
            case 3: return 40;
            case 4: return 50;
            default: return -1;
        }
    }

    // Dense but starting at a **negative** low, so the table's `low` is negative and every case
    // compares against a negative immediate.
    static int negative(int k) {
        switch (k) {
            case -3: return 1;
            case -2: return 2;
            case -1: return 3;
            case 0: return 4;
            case 1: return 5;
            default: return 99;
        }
    }

    // Sparse — javac emits `lookupswitch`. The extreme keys are here on purpose: a compare chain
    // that got the operand size wrong, or a range check that used unsigned comparisons, answers
    // `default` for one of the two ends.
    static int lookup(int k) {
        switch (k) {
            case -2147483648: return 1;
            case -100: return 2;
            case 0: return 3;
            case 7: return 4;
            case 1000: return 5;
            case 2147483647: return 6;
            default: return 0;
        }
    }

    // Arms that **fall through** into each other, so several bytecode arms share one target and the
    // scan must reach the same pc from several branches at the same stack depth.
    static int fallThrough(int k) {
        int r = 0;
        switch (k) {
            case 1:
            case 2:
            case 3:
                r += 5;
                break;
            case 4:
                r += 7;
                // falls into case 5
            case 5:
                r += 11;
                break;
            default:
                r -= 1;
        }
        return r;
    }

    // A switch **inside a loop**: the loop's back-edge is a `goto` past the switch, but some arms
    // `continue`, which makes a switch arm itself a backward branch. That is the shape the OSR
    // scan has to walk through the switch to see.
    static int inLoop(int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            switch (i % 4) {
                case 0: acc += 1; break;
                case 1: acc += 2; break;
                case 2: continue;
                default: acc -= 1;
            }
            acc += 100;
        }
        return acc;
    }

    // The whole thing, scored into one int. Each helper is called well past the JIT's threshold so
    // it compiles, and with keys that hit every arm, the default, and both sides of every boundary.
    public static int run() {
        int score = 0;
        for (int round = 0; round < 200; round++) {
            for (int k = -5; k <= 6; k++) {
                score += table(k);
                score += negative(k);
                score += lookup(k);
                score += fallThrough(k);
            }
            score += lookup(-2147483648);
            score += lookup(2147483647);
            score += lookup(1000);
            score += inLoop(round % 17);
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
