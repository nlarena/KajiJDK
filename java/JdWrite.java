// F3 step 6 coverage: the **real deopt** — rebuilding an interpreter frame at an arbitrary pc,
// operand stack included — and the observable writes that only became safe once a deopt could
// resume instead of restart.
//
// Every write here is deliberately **non-idempotent** (`+ 1`, never `= 1`). That is the whole
// point: a deopt that re-ran the method from its first byte would apply them twice, and the
// difference would show up in the totals below. Applying a write exactly once is not something
// this file merely hopes for — it is what the arithmetic measures.
class JdCell {
    int a;
    int b;
}

public class JdWrite {
    static int sink;

    /** A deopt with a **non-empty operand stack**: `a` is already pushed when the `idiv` gives up. */
    static int midExpression(int a, int b, int c) {
        return a + (b / c) * 3;
    }

    /** A `putfield`, then a deopt. The field must end up moved by exactly one. */
    static int writeThenFail(JdCell cell, int d) {
        cell.a = cell.a + 1;
        return 100 / d;
    }

    /** The same shape through a `putstatic`. */
    static int staticThenFail(int d) {
        sink = sink + 1;
        return 100 / d;
    }

    /** A deopt with **references live in locals and on the operand stack**: `y` is under the index
     *  when the second `iaload` finds it out of range, and `x`'s element is under both. */
    static int twoArrays(int[] x, int[] y, int i) {
        return x[i] + y[i];
    }

    /** A loop of `iastore`s that runs off the end. Every element in range moves by exactly one. */
    static int bump(int[] a, int n) {
        for (int j = 0; j < n; j++) {
            a[j] = a[j] + 1;
        }
        return n;
    }

    /** A deopt caught by a handler **in the very frame that was rebuilt** — the frame survives the
     *  deopt and goes on being interpreted. */
    static int guarded(int[] a, int i) {
        int s;
        try {
            s = a[i] + 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            s = -1;
        }
        return s;
    }

    /** Writes with no deopt in sight, so the ordinary path through the new opcodes is covered too. */
    static int scale(int[] a, JdCell cell, int k) {
        cell.b = (cell.b + k) & 0xFFFF;
        int s = 0;
        for (int j = 0; j < a.length; j++) {
            a[j] = (a[j] + k) & 0xFFFF;
            s = s + a[j];
        }
        return s & 0xFFFF;
    }

    static int sum(int[] a) {
        int s = 0;
        for (int j = 0; j < a.length; j++) {
            s = s + a[j];
        }
        return s;
    }

    public static int run() {
        int acc = 0;
        JdCell cell = new JdCell();
        int[] x = new int[8];
        int[] y = new int[4];

        // Warm every method well past the invocation threshold, on inputs that never deopt.
        for (int i = 0; i < 300; i++) {
            acc = (acc + midExpression(i, i + 7, 3)) & 0xFFFFF;
            acc = (acc + twoArrays(x, y, i & 3)) & 0xFFFFF;
            acc = (acc + scale(x, cell, 1 + (i & 1))) & 0xFFFFF;
            acc = (acc + writeThenFail(cell, 5)) & 0xFFFFF;
            acc = (acc + staticThenFail(7)) & 0xFFFFF;
            acc = (acc + bump(y, 4)) & 0xFFFFF;
            acc = (acc + guarded(x, i & 7)) & 0xFFFFF;
        }

        // 1. Mid-expression: the operand stack is not empty when the division gives up.
        try {
            acc = (acc + midExpression(11, 22, 0)) & 0xFFFFF;
        } catch (ArithmeticException e) {
            acc = (acc + 1) & 0xFFFFF;
        }

        // 2. A write, then a deopt, fifty times over: the field must move by exactly fifty.
        int beforeA = cell.a;
        for (int i = 0; i < 50; i++) {
            try {
                acc = (acc + writeThenFail(cell, 0)) & 0xFFFFF;
            } catch (ArithmeticException e) {
                acc = (acc + 2) & 0xFFFFF;
            }
        }
        acc = (acc + (cell.a - beforeA)) & 0xFFFFF;

        // 3. The same for a static.
        int beforeSink = sink;
        for (int i = 0; i < 50; i++) {
            try {
                acc = (acc + staticThenFail(0)) & 0xFFFFF;
            } catch (ArithmeticException e) {
                acc = (acc + 3) & 0xFFFFF;
            }
        }
        acc = (acc + (sink - beforeSink)) & 0xFFFFF;

        // 4. A deopt with a reference on the operand stack, with allocation between the rounds so a
        //    minor collection can fire while a rebuilt frame is a GC root.
        for (int i = 0; i < 50; i++) {
            try {
                acc = (acc + twoArrays(x, y, 5)) & 0xFFFFF;
            } catch (ArrayIndexOutOfBoundsException e) {
                acc = (acc + 4) & 0xFFFFF;
            }
            JdCell junk = new JdCell();
            junk.b = i;
            acc = (acc + junk.b) & 0xFFFFF;
        }

        // 5. A loop of array writes that runs off the end: every element in range moves by exactly
        //    one, however many iterations native code managed before giving up.
        int beforeSum = sum(y);
        try {
            acc = (acc + bump(y, 9)) & 0xFFFFF;
        } catch (ArrayIndexOutOfBoundsException e) {
            acc = (acc + 5) & 0xFFFFF;
        }
        acc = (acc + (sum(y) - beforeSum)) & 0xFFFFF;

        // 6. A deopt caught inside the rebuilt frame itself, then the same method used again.
        acc = (acc + guarded(x, 99)) & 0xFFFFF;
        acc = (acc + guarded(x, 3)) & 0xFFFFF;

        acc = (acc + cell.a + cell.b + sink + sum(x) + sum(y)) & 0xFFFFF;
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
