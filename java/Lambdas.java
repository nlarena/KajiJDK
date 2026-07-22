// Exercises `LambdaMetafactory.metafactory` (D6): lambdas and method references. Each
// one is an `invokedynamic` whose call site *produces an object* implementing the
// functional interface; calling the interface method then has to reach the body.
public class Lambdas {
    static int twice(int a) {
        return a * 2;
    }

    // Two closures over different values come from the SAME call site — the captured
    // value has to live in each object, not in the shape shared by the site.
    static Op adder(int k) {
        return a -> a + k;
    }

    public static int run() {
        // No capture: the call site takes nothing and returns the object.
        Op inc = a -> a + 1;
        if (inc.apply(10) != 11) return -1;

        // A method reference reaches an ordinary method rather than a synthetic body.
        Op dbl = Lambdas::twice;
        if (dbl.apply(10) != 20) return -2;

        // A capture becomes the implementation's *leading* parameter, ahead of the
        // interface method's own arguments.
        int n = 5;
        Op add = a -> a + n;
        if (add.apply(10) != 15) return -3;

        // The decisive case: one call site, two objects, two different captures.
        Op plus1 = adder(1);
        Op plus2 = adder(2);
        if (plus1.apply(10) != 11) return -4;
        if (plus2.apply(10) != 12) return -5;
        // ...and the first must not have been overwritten by the second.
        if (plus1.apply(0) != 1) return -6;

        // Lambdas stay independent objects.
        if (inc == dbl) return -7;

        return 42;
    }
}

interface Op {
    int apply(int a);
}
