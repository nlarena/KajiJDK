// Differential workload for **F3-H3: `aastore`** — the reference store that also owes JVMS §6.5's
// dynamic assignability check.
//
// Arrays are covariant, so the static types cannot make a store sound: `Object[] xs = new
// WbLeaf[2]; xs[0] = "s";` is legal bytecode and an `ArrayStoreException` at run time. Deciding
// that in general is a walk over class metadata, so compiled code answers only the **exact** class
// pair — the array's own class against the one the interpreter profiled at that site, and the
// value's class against that array class's element class — and hands the method back for anything
// else. Three shapes, so that all three answers are observed:
//
//  - `exact`     — a `WbLeaf` into a `WbLeaf[]`: both comparisons hold, and the store is native;
//  - `covariant` — a `WbLeaf` into an `Object[]`: legal, assignable, and *not* an exact match, so
//    native code deopts every single time and the interpreter does the store. The count it
//    produces must be identical, which is the whole claim;
//  - `illegal`   — a `String` into a `WbLeaf[]` seen as an `Object[]`: the exception case, caught
//    in Java, so the score itself says the throw happened and happened once per round.
//
// `store` is deliberately its own method with no handler in it: the `try`/`catch` belongs to the
// caller, so the site under test is an ordinary compilable body.
public class WbArr {
    static void store(Object[] xs, int at, Object v) {
        xs[at] = v; // the `aastore` under test
    }

    static int exact(int n) {
        WbLeaf[] xs = new WbLeaf[8];
        for (int i = 0; i < 8; i++) {
            WbLeaf seed = new WbLeaf();
            seed.tag = i;
            xs[i] = seed;
        }
        int acc = 0;
        for (int i = 0; i < n; i++) {
            WbLeaf leaf = new WbLeaf();
            leaf.tag = i & 15;
            xs[i & 7] = leaf; // aastore, exact element class
            acc = acc + xs[(i + 3) & 7].tag; // aaload, then getfield
        }
        return acc;
    }

    static int covariant(int n) {
        Object[] xs = new Object[8];
        int acc = 0;
        for (int i = 0; i < n; i++) {
            WbLeaf leaf = new WbLeaf();
            leaf.tag = i & 15;
            xs[i & 7] = leaf; // legal, assignable, and never an exact match
            Object back = xs[i & 7];
            acc = acc + ((WbLeaf) back).tag;
        }
        return acc;
    }

    static int illegal(int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            Object[] seen = new WbLeaf[2];
            try {
                store(seen, i & 1, "not a leaf");
                acc = acc + 1000; // never reached
            } catch (ArrayStoreException e) {
                acc = acc + 1;
            }
        }
        return acc;
    }

    // The case that isolates the **array**-class half of the guard from the value-class half.
    //
    // `mix` is called twice per round on the *same* site: once with an `Object[]`, which is what the
    // site gets profiled as, and once with a `WbLeaf[]` seen through an `Object[]` reference. The
    // payload is a plain `java.lang.Object` — so its class is exactly the element class of the
    // profiled array, and the value comparison alone would let the second call through. Only the
    // array comparison can stop it, and stopping it is the difference between an
    // `ArrayStoreException` and an `Object` sitting in a `WbLeaf[]`.
    static void mix(Object[] xs, Object v) {
        xs[0] = v;
    }

    static int arrayClass(int n) {
        int acc = 0;
        Object[] anything = new Object[1];
        for (int i = 0; i < n; i++) {
            Object payload = new Object();
            mix(anything, payload); // legal, and what the site is profiled on
            Object[] leaves = new WbLeaf[1];
            try {
                mix(leaves, payload); // an Object is not a WbLeaf
                acc = acc + 1000; // never reached
            } catch (ArrayStoreException e) {
                acc = acc + 1;
            }
        }
        return acc;
    }

    static int nulls(int n) {
        WbLeaf[] xs = new WbLeaf[4];
        int acc = 0;
        for (int i = 0; i < n; i++) {
            xs[i & 3] = null; // a null stores into any reference array, with no header read
            if (xs[i & 3] == null) {
                acc = acc + 1;
            }
        }
        return acc;
    }

    public static int run() {
        int acc = 0;
        acc = acc * 31 + exact(500);
        acc = acc * 31 + covariant(500);
        acc = acc * 31 + illegal(300);
        acc = acc * 31 + arrayClass(300);
        acc = acc * 31 + nulls(500);
        return acc & 0x3FFFFFF;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}

class WbLeaf {
    int tag;
}
