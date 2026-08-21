// Differential workload for the F3 JIT, group 2 — dimension: **richer references**.
//
// Step 5 gave the compiled subset references it could *carry*; the wide-type step gave it every
// primitive field. This file is the correctness half of the three things that were left, and every
// method in it exists for a question that only arises once one of them is in:
//
//   * can a `getfield` produce a **reference**, and can the result be dereferenced again?
//     `chain` is `r.mid.leaf.v` — three loads, the first two of them references, so an offset
//     folded wrong at any hop lands somewhere else entirely rather than crashing. `viaStatic`
//     asks the same of `getstatic`.
//   * is a **null** reference an ordinary value? `nullable` loads a field that is null on half its
//     calls and only ever *tests* it (`ifnull`/`ifnonnull`). Loading `null` must not deopt — only
//     dereferencing one may — and the deopt counter is what proves it.
//   * are **`volatile`** fields readable and (for primitives) writable? `readVi`/`bumpVi`,
//     `readVl`/`setVl`, `readVstat`/`bumpVstat` and `readVr` cover an `int`, a `long`, a static and
//     a **reference read**. `bumpVi` is deliberately non-idempotent (`+= d`), so a deopt that
//     restarted rather than resumed would show up in the total.
//   * does **`checkcast`** mean what the JVMS says when native code only recognises the *exact*
//     class? `cast` is called with the exact class (a hit), with a genuine **subtype** (which
//     deopts, and the interpreter then passes it) and with an unrelated class (which deopts, and
//     the interpreter then throws the `ClassCastException` the `catch` below counts). `isLeaf` is
//     the same three cases through `instanceof`, plus `null`, which must answer `0` with no deopt.
//   * is a **class literal** the same pinned object every time? `leafClass` is a compiled
//     `ldc JxLeaf.class`; `run` compares it for **identity** against its own interpreted one, and
//     does so again after enough allocation to have provoked collections — a mirror is
//     `malloc_old`ed and pinned, so the answer must not change.
//
// What is deliberately *not* here: an `ldc` of a `String`. This VM allocates a fresh `String` in
// Eden for every one (there is no interning table at all), so there is no permanent offset for
// compiled code to bake in — see `burst::compile`'s module docs. And no compiled write of a
// reference field: `JxRich.<init>` writes `mid` and `vr` and is refused for exactly that reason,
// which is the group's hard limit and the thing that keeps compiled code unable to create an
// old→young pointer.
class JxLeaf {
    int v;

    JxLeaf(int v) {
        this.v = v;
    }
}

// A genuine subtype: passes `checkcast JxLeaf` by the JVMS and fails the compiled *equality* test,
// so every call is a deopt whose answer must still be "the cast succeeds".
class JxSub extends JxLeaf {
    JxSub(int v) {
        super(v * 2);
    }
}

// Unrelated: the cast fails, and the exception is the interpreter's to throw.
class JxOther {
    int w;

    JxOther(int w) {
        this.w = w;
    }
}

class JxMid {
    JxLeaf leaf;

    JxMid(JxLeaf l) {
        this.leaf = l;
    }
}

public class JxRich {
    JxMid mid;
    volatile int vi;
    volatile long vl;
    volatile JxLeaf vr; // null on half the instances: `nullable` is what reads it

    static JxLeaf shared;
    static volatile int vstat;

    JxRich(JxMid m, JxLeaf r) {
        this.mid = m;
        this.vr = r;
    }

    // Two reference `getfield`s and an `int` one: the offsets have to be right at every hop.
    static int chain(JxRich r) {
        return r.mid.leaf.v;
    }

    // `getstatic` of a reference, then a field through it.
    static int viaStatic() {
        return shared.v;
    }

    // A reference that is null half the time, loaded and only ever tested. No dereference, so no
    // deopt — which is the whole assertion.
    static int nullable(JxRich r) {
        JxLeaf l = r.vr;
        int s = 0;
        if (l == null) {
            s += 1;
        }
        if (l != null) {
            s += 2;
        }
        return s;
    }

    // volatile `int`: a read, and a **non-idempotent** read-modify-write.
    int readVi() {
        return this.vi;
    }

    void bumpVi(int d) {
        this.vi = this.vi + d;
    }

    // volatile `long`: eight bytes, whole, in both directions.
    long readVl() {
        return this.vl;
    }

    void setVl(long x) {
        this.vl = x;
    }

    // volatile **reference**, read only — writing one is outside the subset for the whole VM's
    // sake, not for this field's.
    static JxLeaf readVr(JxRich r) {
        return r.vr;
    }

    // A volatile static, read and written.
    static int readVstat() {
        return vstat;
    }

    static void bumpVstat() {
        vstat = vstat + 1;
    }

    // `checkcast` then `getfield`: exact class, subtype, and failure.
    static int cast(Object o) {
        return ((JxLeaf) o).v;
    }

    // `instanceof`: the same three, plus null.
    static int isLeaf(Object o) {
        return (o instanceof JxLeaf) ? 1 : 0;
    }

    // `ldc` of a class literal — a pinned mirror offset, materialised as an immediate.
    static Object leafClass() {
        return JxLeaf.class;
    }

    public static int run() {
        JxLeaf leaf = new JxLeaf(11);
        JxSub sub = new JxSub(3);
        JxOther other = new JxOther(5);
        JxRich withRef = new JxRich(new JxMid(leaf), leaf);
        JxRich withNull = new JxRich(new JxMid(new JxLeaf(4)), null);
        shared = new JxLeaf(23);

        int score = 0;
        for (int round = 0; round < 400; round++) {
            score = (score + chain(withRef) + chain(withNull) + viaStatic()) & 0xFFFFF;
            score = (score + nullable(withRef) + nullable(withNull)) & 0xFFFFF;

            // volatile, in every width the subset has: read, write, read back.
            withRef.bumpVi(round & 7);
            score = (score + withRef.readVi()) & 0xFFFFF;
            withRef.setVl((long) round * 1_000_003L);
            score = (score + (int) (withRef.readVl() % 9973L)) & 0xFFFFF;
            bumpVstat();
            score = (score + readVstat()) & 0xFFFFF;

            // A volatile *reference* read, checked for identity rather than nullness.
            JxLeaf got = readVr(withRef);
            if (got == leaf) {
                score += 1;
            } else {
                score += 1_000_000; // some other object entirely
            }
            if (readVr(withNull) == null) {
                score += 2;
            }

            // `checkcast`: the exact class is a native hit; the subtype and the failure both deopt.
            score = (score + cast(leaf) + cast(sub)) & 0xFFFFF;
            try {
                score += cast(other);
            } catch (ClassCastException e) {
                score += 4;
            }

            // `instanceof`: exact, subtype, unrelated, and null — the last with no dereference.
            score = (score + isLeaf(leaf) * 1 + isLeaf(sub) * 2 + isLeaf(other) * 4 + isLeaf(null) * 8) & 0xFFFFF;

            // The class literal, by identity — and it stays that object across the collections the
            // allocation below provokes, because a mirror is pinned.
            if (leafClass() == JxLeaf.class) {
                score += 16;
            }

            // Garbage, to move Eden along and give the collector something to do between rounds.
            int[] churn = new int[64];
            churn[round & 63] = round;
            score = (score + churn[round & 63]) & 0xFFFFF;

            score &= 0xFFFFF;
        }

        // ...and once more after all of it: a pinned mirror is the same object at the end.
        if (leafClass() == JxLeaf.class) {
            score += 32;
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
