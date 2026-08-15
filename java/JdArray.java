// The measurement `BmArray` cannot be — dimension: **array writes**.
//
// Same arithmetic, same 1024-element array, same 1000 x 1024 iterations as `BmArray`. The only
// difference is that the inner loop lives in a method of its own, so the JIT's trigger can see it:
// `BmArray.run` begins `new int[1024]`, and an allocation puts the *whole* method outside the
// compiled subset however ordinary the loop inside it is. Hoisting the loop moves the allocation
// out of the compiled method rather than out of the program.
//
// That relationship is exactly `JtLoop`'s to `BmLoop`, one step earlier: the workload the trigger
// could already see, kept beside the one it could not, so "the JIT works" and "the trigger reaches
// it" stay separate claims.
public class JdArray {
    static int pass(int[] a, int i) {
        int acc = 0;
        for (int j = 0; j < a.length; j++) {
            a[j] = (a[j] + i + j) & 0xFFFF;
            acc = acc + a[j];
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        int[] a = new int[1024];
        int acc = 0;
        for (int i = 0; i < 1000; i++) {
            acc = (acc + pass(a, i)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
