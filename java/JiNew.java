// F3 step 8 — **the whole `new X(a, b)` pattern**, which is what the step exists for.
//
// `javac` never emits a bare `new`: it emits `new; dup; <args>; invokespecial <init>`, so the
// inline allocation step 7 built bought nothing until a call could be expanded. This is that
// pattern end to end, and it needs every piece of the step at once:
//
//   - `JiCell.<init>` is inlined into `run` — an `invokespecial` with a receiver and two arguments;
//   - `<init>` itself contains two `putfield`s, each of which **can deopt** (a null receiver), so
//     the expansion is only sound if a deopt can rebuild two interpreter frames;
//   - and `<init>` begins with `super()`, so `java.lang.Object.<init>` is inlined *into the inlined
//     constructor* — three frames deep, which is the depth limit.
//
// `c` is initialised to `null` before the loop on purpose. It is not decoration: with it, the outer
// loop header is reached with a reference in that slot on both paths, and without it the slot is an
// `int` on entry and a reference on the back-edge — the dead-slot merge the type map declines
// (`Ineligible::TypeMismatch`), which is a step-5 limitation and would hide what this file tests.
class JiCell {
    int a;
    int b;

    JiCell(int a, int b) {
        this.a = a;
        this.b = b;
    }
}

public class JiNew {
    static int run() {
        int acc = 0;
        JiCell c = null;
        for (int i = 0; i < 400; i++) {
            c = new JiCell(i, i + 1);
            acc = (acc + c.a + c.b) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
