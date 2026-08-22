// Baseline workload — dimension: **dynamic dispatch** (`invokevirtual`).
//
// A three-deep hierarchy, all overriding `f`, called through a base-typed array whose
// element rotates every iteration: the call site is genuinely **polymorphic**, so nothing
// can be answered by "the receiver is what it was last time". Every call pays the full
// path — read the receiver's class from its header, find the vtable slot for the static
// type, then index the runtime type's table — which is the cost an inline cache would
// later attack. The objects are allocated once, so the GC stays out of the measurement.
class BmShape {
    int k;

    BmShape(int k) {
        this.k = k;
    }

    int f(int x) {
        return x + this.k;
    }
}

class BmSq extends BmShape {
    BmSq(int k) {
        super(k);
    }

    int f(int x) {
        return (x * 2) + this.k;
    }
}

class BmCir extends BmShape {
    BmCir(int k) {
        super(k);
    }

    int f(int x) {
        return x - this.k;
    }
}

class BmTri extends BmShape {
    BmTri(int k) {
        super(k);
    }

    int f(int x) {
        return (x ^ this.k) + 1;
    }
}

public class BmVirtual {
    static int run() {
        BmShape[] shapes = { new BmSq(1), new BmCir(2), new BmTri(3) };
        int acc = 0;
        int which = 0;
        for (int i = 0; i < 220000; i++) {
            acc = (acc + shapes[which].f(i)) & 0xFFFFF;
            which = which + 1;
            if (which == 3) {
                which = 0;
            }
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
