// Differential workload for the F3 JIT, group 3 stage 2 — dimension: the **monomorphic inline
// cache** (milestone F2).
//
// A virtual call cannot be inlined statically: which body runs depends on the receiver's runtime
// class. This tier's answer is to bind the call *speculatively* — bake in the one class the
// interpreter has actually seen at that site, compare the receiver's header word against it, and
// **deopt** on anything else. So the whole feature has exactly one way to be wrong, and it is not a
// slow program: it is running the wrong body. Every site below exists to make that failure loud.
//
//  - `mono` is the case the cache is for: one receiver class, forever. The guard hits every time
//    and the body that runs is `JcAlpha.twice`.
//  - `drift` is the **critical** case. It is warmed with a `JcAlpha` until it compiles — so the
//    guard is baked for `JcAlpha` — and then handed a `JcBeta`, which overrides `twice` with
//    different arithmetic. A cache without a guard, or with a guard that does not deopt, computes
//    `JcAlpha.twice` for a `JcBeta` and the answer moves. It must not.
//  - `viaInterface` is an `invokeinterface` to a **default** method which itself calls back through
//    the interface, so two levels of expansion hang off one guard — and `JcBeta` overrides `base`,
//    so the drifting half of the run exercises the miss at the inner level too.
//  - `one` is the receiver a guard must reject before it dereferences anything: `null`. The
//    interpreter's `invokevirtual` throws the `NullPointerException` *before* it dispatches, so
//    native code owes that check; what it must not do is run the body on a null `this`.
interface JcThing {
    int base();

    // Reached by `invokeinterface`, and inlined through the same exact-class guard — which is what
    // makes the signature search the interpreter would do unnecessary.
    default int scaled(int x) {
        return (x * 5) + base();
    }
}

class JcAlpha implements JcThing {
    int k;

    JcAlpha(int k) {
        this.k = k;
    }

    public int base() {
        return this.k;
    }

    int twice(int x) {
        return (x * 2) + this.k;
    }

    // A dispatched call **inside** a body that is itself reached by a dispatched call. The receiver
    // here is somebody else's, so the two guards are about two different objects and the inner one
    // can miss while the outer one hits — which is the only way this tier ever deopts out of the
    // middle of an expansion at a call site.
    int through(JcAlpha o, int x) {
        return o.twice(x) + this.k;
    }
}

// A genuine subtype that overrides **both** halves, so a guard that failed to fire would be visible
// in the arithmetic rather than merely in a statistic.
class JcBeta extends JcAlpha {
    JcBeta(int k) {
        super(k);
    }

    public int base() {
        return this.k + 7;
    }

    int twice(int x) {
        return (x * 3) - this.k;
    }
}

public class JcIc {
    // A nested class whose `private` method `javac` reaches by **`invokevirtual`** — nestmate
    // access, JVMS §6.5, and what every Java 11+ compiler emits. Such a site resolves *directly* to
    // the private method: it has no vtable slot and there is nothing to speculate on, so the only
    // guard it gets is the bare null check. That check is not decoration. The interpreter's
    // `invokevirtual` raises the `NullPointerException` **before** it dispatches, so a body inlined
    // without it would run on a `null` `this` and quietly compute a number.
    static class JcNest {
        int k;

        JcNest(int k) {
            this.k = k;
        }

        private int hidden(int x) {
            return (x * 11) + this.k;
        }

        // A `private` method that **never touches `this`**, which is the shape where the null check
        // is the only thing that can notice. A body that dereferenced the receiver would deopt at
        // its own `getfield` and the interpreter would raise the `NullPointerException` for it
        // anyway; this one would happily return a number.
        private int hollow(int x) {
            return (x * 13) + 5;
        }
    }

    // Site F — the nestmate site, warmed until it compiles.
    static int hiddenLoop(JcNest n, int m) {
        int acc = 0;
        for (int i = 0; i < m; i++) {
            acc = (acc + n.hidden(i)) & 0xFFFFF;
        }
        return acc;
    }

    // Site G — one nestmate call, so the null check is the whole method.
    static int oneHidden(JcNest n) {
        return n.hidden(3);
    }

    // Site H — one nestmate call to a body that never reads its receiver.
    static int oneHollow(JcNest n) {
        return n.hollow(3);
    }

    // Site A — monomorphic for the whole run.
    static int mono(JcAlpha a, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + a.twice(i)) & 0xFFFFF;
        }
        return acc;
    }

    // Site B — monomorphic until it is compiled, polymorphic afterwards.
    static int drift(JcAlpha a, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + a.twice(i)) & 0xFFFFF;
        }
        return acc;
    }

    // Site C — an interface call to a `default` method that calls back through the interface.
    static int viaInterface(JcThing t, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + t.scaled(i)) & 0xFFFFF;
        }
        return acc;
    }

    // Site D — the **inner** guard. `holder` never changes class, so `through` is expanded; the
    // call inside it is guarded on `other`, which the second half of the run makes drift. A miss
    // there has to hand back the frame `through` never had.
    static int relay(JcAlpha holder, JcAlpha other, int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + holder.through(other, i)) & 0xFFFFF;
        }
        return acc;
    }

    // Site E — one call, so the guard is the whole method.
    static int one(JcAlpha a) {
        return a.twice(3);
    }

    public static int run() {
        int score = 0;
        JcAlpha alpha = new JcAlpha(9);
        JcBeta beta = new JcBeta(4);
        JcNest nest = new JcNest(6);

        // Warm every site with a `JcAlpha`, which is what fixes the class each guard is compiled
        // against.
        for (int round = 0; round < 40; round++) {
            score = (score + mono(alpha, 200)) & 0xFFFFF;
            score = (score + drift(alpha, 200)) & 0xFFFFF;
            score = (score + viaInterface(alpha, 200)) & 0xFFFFF;
            score = (score + relay(alpha, alpha, 200)) & 0xFFFFF;
            score = (score + one(alpha)) & 0xFFFFF;
            score = (score + hiddenLoop(nest, 200)) & 0xFFFFF;
            score = (score + oneHidden(nest)) & 0xFFFFF;
            score = (score + oneHollow(nest)) & 0xFFFFF;
        }

        // The same two sites with a receiver they have never seen: every guard misses, every call
        // deopts, and the answer has to be the one the interpreter alone would have produced.
        for (int round = 0; round < 40; round++) {
            score = (score + drift(beta, 200)) & 0xFFFFF;
            score = (score + viaInterface(beta, 200)) & 0xFFFFF;
            score = (score + relay(alpha, beta, 200)) & 0xFFFFF;
        }

        // And `mono` keeps hitting after all that, so a miss elsewhere cannot be mistaken for the
        // cache having been disabled.
        score = (score + mono(alpha, 200)) & 0xFFFFF;

        // A `null` receiver at two sites that have been compiled for real ones: the dispatched one,
        // where the class comparison would notice anyway, and the **nestmate** one, where the null
        // check is all there is.
        try {
            score = (score + one(null)) & 0xFFFFF;
        } catch (NullPointerException e) {
            score = (score + 12345) & 0xFFFFF;
        }
        try {
            score = (score + oneHidden(null)) & 0xFFFFF;
        } catch (NullPointerException e) {
            score = (score + 6789) & 0xFFFFF;
        }
        try {
            score = (score + oneHollow(null)) & 0xFFFFF;
        } catch (NullPointerException e) {
            score = (score + 2468) & 0xFFFFF;
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
