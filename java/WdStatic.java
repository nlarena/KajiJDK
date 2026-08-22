// Differential workload for the F3 JIT, step 4 — dimension: **`getstatic` of an `int`**.
//
// This is the first opcode in the compiled subset that reads the *heap*, so the questions it has to
// answer are new ones:
//
//   * does compiled code read the **right four bytes**? `own`, `inherited` and `far` name statics in
//     three different mirrors — the method's own class, its superclass, and an unrelated class —
//     and each has neighbours around it in the mirror, so an off-by-one slot would show up as
//     someone else's value rather than as a crash;
//   * is it a **live read** rather than a value baked in at compile time? `run` changes `MUTABLE`
//     between calls and sums what the compiled method reports. A compiler that folded the value in
//     would return the same number every round, and the score would be wrong by a large margin;
//   * does the address survive the **GC**? `churn` allocates hard enough to force collections
//     between the calls; a mirror that moved, or a heap that reallocated, would make the baked-in
//     address point at rubbish. (It does not: mirrors are `malloc_old`-allocated and in
//     `gc::compact`'s pinned set, and the heap's byte region is pre-reserved.)
//   * is a **non-`int`** static refused? `notAnInt` reads a `String` static, so it must never
//     compile — and must still compute the same answer, interpreted, in both arms.
//
// Nothing here writes a static from compiled code: `putstatic` is deliberately outside the subset
// (see `burst::compile`'s module docs). Every write below happens in `run`, which is full of
// invokes and is never compiled.
public class WdStatic extends WdStaticBase {
    static int OWN = 3;
    static int NEIGHBOUR = 1000;   // sits next to OWN in the mirror: catches an off-by-one slot
    static int MUTABLE = 0;

    static int own(int x) {
        return x + OWN + NEIGHBOUR;
    }

    static int inherited(int x) {
        return x * INHERITED;
    }

    static int far(int x) {
        return x + WdStaticOther.FAR;
    }

    // Reads the mutable static — the same compiled code, called again after `run` changed it.
    static int mutable(int x) {
        return MUTABLE + x;
    }

    // Every one of the three, plus arithmetic between them, in a loop so it also goes hot from the
    // inside (OSR) rather than only from its invocation counter.
    static int mixed(int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc += OWN * INHERITED + WdStaticOther.FAR - NEIGHBOUR / (i + 1);
        }
        return acc;
    }

    // Outside the subset: `TEXT` is a `String`, so the resolver refuses to give the JIT an address
    // and the whole method is ineligible. It must still compute the same thing in both arms.
    static int notAnInt(int x) {
        return x + WdStaticOther.TEXT.length();
    }

    // Allocation, to force the collector to run between the calls above.
    static int churn(int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            int[] a = new int[16];
            a[i % 16] = i;
            acc += a[i % 16];
        }
        return acc;
    }

    public static int run() {
        int score = 0;
        for (int round = 0; round < 300; round++) {
            MUTABLE = round;
            score += own(round);
            score += inherited(round);
            score += far(round);
            score += mutable(round);
            score += notAnInt(round);
            score += mixed(round % 9);
            score += churn(round % 11);
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
