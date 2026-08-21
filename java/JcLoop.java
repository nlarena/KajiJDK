// Differential workload for the F3 JIT, group 3 stage 1 — dimension: a **loop inside an inlined
// callee**, and the safepoint poll that now sits at its header.
//
// Until this stage a callee containing a backward branch was refused outright, and its caller with
// it: only the root's loop headers carried a poll, so an inlined loop would have been one native
// code could not be pulled out of. The observation that lifts it is that a poll is just a resume
// site, and resume sites have carried the frames inlining removed since step 8. So an inlined
// header polls, and a poll taken there hands back a **chain** of interpreter frames.
//
// Three shapes, and each is one thing that can go wrong:
//
//  - `flat` has no loop of its own, so every poll it takes is inside an expansion — and it makes
//    **two** calls in one expression, so when the second one polls the caller is holding a live
//    operand (the first call's result). A stub that rebuilt only the innermost frame, or that
//    spilled the caller's stack without stopping short of the arguments, moves the answer.
//  - `nested` loops *and* inlines a loop, so the poll can fire at either level and the two exits
//    have to be told apart by the key native code reports — a root site is named by its pc, an
//    inlined one by a number past every pc there is.
//  - `sameness` is the identity check: the objects that went into compiled code have to be the ones
//    that came out. `run` allocates freshly every round, so they are young, and the interpreter
//    collects in the gaps between a poll firing and native code being re-entered — a minor
//    collection **moves** them while `inner` is half-interpreted, and every offset the JIT handed
//    back only survives that if the rebuilt frame said "reference".
public class JcLoop {
    int k;

    JcLoop(int k) {
        this.k = k;
    }

    // The callee that loops. Its header's operand stack is empty, so it can carry a poll — which is
    // exactly the condition this stage checks before it agrees to inline a looping body.
    static int inner(int[] a, JcLoop c, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + a[i % a.length] + c.k) & 0xFFFFF;
        }
        return acc;
    }

    // A callee that does **not** loop, so it runs to its `return` before anything can poll. It is
    // here to leave a live operand on `flat`'s stack underneath the call that does loop.
    static int bump(JcLoop c) {
        return c.k * 7;
    }

    // A root with no loop of its own. When `inner`'s header polls, this frame is in the middle of a
    // call with **two** operands live underneath its arguments — so the stub has to spill exactly
    // those two, and the rebuilt frame has to carry exactly those two. One too few and the `iadd`s
    // below run out of stack; one too many (the arguments, which are the callee's locals now) and
    // every argument comes back twice.
    static int flat(int[] a, JcLoop c, JcLoop d, int n) {
        return (bump(c) + (a.length + inner(a, d, n))) & 0xFFFFF;
    }

    // The **middle** of a three-deep chain: it is itself inlined, and it inlines the looping
    // callee. When `inner` polls, this frame exists in no machine register anywhere — it has to be
    // materialised from nothing, at the pc of its own invoke, holding exactly the one operand
    // (`c.k`) it had pushed before the call and **not** the three arguments it had given away.
    static int mid(int[] a, JcLoop c, int n) {
        return (c.k + inner(a, c, n)) & 0xFFFFF;
    }

    // The root of that chain: root -> `mid` -> `inner`, three frames, two of them virtual.
    static int deep(int[] a, JcLoop c, int n) {
        return mid(a, c, n);
    }

    // A root that loops and inlines a loop.
    static int nested(int[] a, JcLoop c, int rounds, int n) {
        int acc = 0;
        for (int i = 0; i < rounds; i++) {
            acc = (acc + inner(a, c, n) + i) & 0xFFFFF;
        }
        return acc;
    }

    static int sameness(JcLoop p, JcLoop q) {
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
            JcLoop c = new JcLoop(11 + round);
            JcLoop d = new JcLoop(4);

            score = (score + flat(a, c, d, 90)) & 0xFFFFF;
            score = (score + nested(a, c, 7, 90)) & 0xFFFFF;
            score = (score + deep(a, d, 90)) & 0xFFFFF;

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
