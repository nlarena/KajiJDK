// F3 step 8 — **references in a rebuilt virtual frame**, and the one mistake that would not fail
// where the bug is.
//
// A frame that inlining removed is re-materialised out of a flat `i64` buffer, so every value in it
// has to be tagged with what it *is*. A heap offset put back as a `Value::Int` is a live object the
// collector can no longer see or relocate; an `int` put back as a `Value::Reference` is a pointer
// made of arithmetic. Neither shows up at the write.
//
// Making that observable takes a deopt the program **survives**, because a frame that immediately
// throws never reads its locals again — and a mistagged local nothing reads is a mistake no test
// can see. So `at` catches its own exception: the guard fails every fourth call, the deopt rebuilds
// `at`'s frame in the middle of the `try`, the interpreter re-executes the array read, `at`'s *own*
// handler catches it, and execution carries on **inside the rebuilt frame** — where it then reads
// `c`, a reference local that was live across the whole thing.
//
// That also makes this the file for the other half of the question: an inlined callee with an
// exception table of its own. Native code throws nothing, so no handler can fire while it runs;
// what a handler does is catch in the frame the deopt rebuilt, which is an ordinary interpreter
// frame for `at` and needs nothing from this tier at all.
class JiPair {
    int a;
    int b;

    JiPair(int a, int b) {
        this.a = a;
        this.b = b;
    }
}

public class JiRef {
    static int at(int[] arr, JiPair c, int i) {
        int x;
        try {
            x = arr[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            x = 9;
        }
        return x + c.a + c.b;
    }

    static int outer(int[] arr, JiPair c, int i) {
        return at(arr, c, i) + 1;
    }

    static int run() {
        int[] arr = { 2, 3, 5 };
        JiPair c = new JiPair(11, 13);
        int acc = 0;
        for (int i = 0; i < 400; i++) {
            acc = (acc + outer(arr, c, i % 4)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
