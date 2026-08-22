// F3 step 8 — **a deopt from inside an inlined callee**, and the unwind that follows it.
//
// `step` is the compiled method and `div` is expanded into it, so at run time there is one native
// frame where the interpreter would have had two. Every eighth call divides by zero, which is a
// guard `div` cannot satisfy — and the *whole* of the step-8 protocol is what has to happen next:
// native code hands back two frames, the interpreter builds them, re-executes the `idiv` in the
// rebuilt `div` frame, throws `ArithmeticException`, unwinds it through `div` and `step` (neither
// has a handler) and lands in `run`'s `catch`.
//
// The score counts both arms — the seven good calls and the failing one — so a deopt that lost a
// frame, resumed at the wrong pc, or re-ran the division would not merely crash: it would print a
// different number, and the interpreted arm is what says which number is right.
public class JiDeopt {
    static int div(int n, int d) {
        return n / d;
    }

    static int step(int d) {
        return div(1000, d) + 1;
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 400; i++) {
            try {
                acc = (acc + step(i % 8)) & 0xFFFFF;
            } catch (ArithmeticException e) {
                acc = (acc + 7) & 0xFFFFF;
            }
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
