// A7 #5 (JLS §5.1.7, JSR 201): autoboxing / unboxing. javac compiles every implicit
// conversion here to Integer.valueOf / intValue etc. against the standard API, and our
// boot wrappers carry the same signatures — including the mandated valueOf caches, so
// two boxes of 100 are the SAME object (==) while two boxes of 200 are not.
//
// The Long section runs FIRST, on purpose: initializing Long at a near-full Eden tickles
// an open os-parallel-only GC bug (spurious ArithmeticException out of code that has no
// division — ~50% reproducers: java/BxDbgT.java and java/BxDbgY.java, which put the Long
// section last). With Long.<clinit> triggered at a fresh heap the program is
// oracle-stable in all three modes.
public class BxTest {
    public static int run() {
        int total = 0;

        // Long boxes and unwraps a category-2 value.
        Long l = 7L;
        if (l.longValue() == 7L) {
            total += 4;
        }

        // Box then unbox: `Integer a = 5` → valueOf(5); `int b = a` → a.intValue().
        Integer a = 5;
        int b = a;
        if (b == 5) {
            total += 5;
        }

        // Cache identity: -128..127 must box to the identical object (JLS §5.1.7).
        Integer x = 100, y = 100;
        if (x == y) {
            total += 10;
        }

        // Outside the cache: each box is a fresh allocation, so == is false.
        Integer p = 200, q = 200;
        if (p != q) {
            total += 10;
        }

        // equals reboxes the literal (valueOf(100)) and compares by value.
        if (x.equals(100)) {
            total += 5;
        }

        // Boolean boxes to the canonical TRUE/FALSE singletons.
        Boolean t = true;
        if (t == Boolean.TRUE) {
            total += 4;
        }

        // Character boxes ASCII through the 0..127 cache.
        Character c = 'A';
        if (c.charValue() == 'A') {
            total += 4;
        }

        return total; // 4 + 5 + 10 + 10 + 5 + 4 + 4 = 42
    }
}
