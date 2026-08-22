// Differential workload for the F3 JIT, group 3 stage 2 — the **benchmark harness's own
// polymorphism**, asked of the inline cache.
//
// `BmVirtual` is this milestone's dynamic-dispatch control, and its four classes (`BmShape`,
// `BmSq`, `BmCir`, `BmTri`) are the real thing: a three-deep hierarchy all overriding `f`. What
// `BmVirtual.run` itself does with them is read them out of a `BmShape[]`, and `aaload` is outside
// this tier's subset — so `run` does not compile, with or without an inline cache, and the harness
// workload is unchanged by F2. This file asks the same question of the same classes in a shape the
// subset *can* express, so the answer is measured rather than assumed:
//
//  - `steady` is monomorphic — one `BmSq`, forever — and is what the cache is for.
//  - `rotate` is `BmVirtual.run`'s dispatch, minus the array: **one** call site whose receiver
//    changes every iteration. The guard is baked for whichever class the interpreter last saw, so
//    roughly two calls in three miss and deopt. The point of the test is not the hit rate; it is
//    that the sum is exactly the interpreter's, which is only true if every miss ran the *right*
//    body.
public class JcShapes {
    // One receiver class for the whole run.
    static int steady(BmShape s, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + s.f(i)) & 0xFFFFF;
        }
        return acc;
    }

    // One call site, three receiver classes, rotating. The rotation is by identity rather than by
    // an array so that the *dispatch* is the only thing out of reach.
    static int rotate(BmShape p, BmShape q, BmShape r, int n) {
        int acc = 0;
        BmShape s = p;
        for (int i = 0; i < n; i++) {
            acc = (acc + s.f(i)) & 0xFFFFF;
            if (s == p) {
                s = q;
            } else if (s == q) {
                s = r;
            } else {
                s = p;
            }
        }
        return acc;
    }

    public static int run() {
        BmSq sq = new BmSq(1);
        BmCir cir = new BmCir(2);
        BmTri tri = new BmTri(3);

        int score = 0;
        for (int round = 0; round < 40; round++) {
            score = (score + steady(sq, 300)) & 0xFFFFF;
            score = (score + rotate(sq, cir, tri, 300)) & 0xFFFFF;
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
