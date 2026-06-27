// Exercises the verifier's exception-handler checks (JVMS §4.10.1.6): an explicit
// `throw` (athrow) caught by a supertype handler, a method returning a reference
// (areturn), and one try with two typed handlers. All use no-arg constructors so it
// also *runs* on our minimal boot exception classes.
public class Exc {
    // athrow + a handler typed as a *supertype* of what's thrown.
    static int thrown() {
        try {
            throw new ArithmeticException();
        } catch (RuntimeException e) {
            return 1;
        }
    }

    // areturn: the result is a reference, returned on two control-flow paths.
    static String pick(boolean b) {
        if (b) {
            return "yes";
        }
        return "no";
    }

    // One try, two handlers of different exception types.
    static int classify(int x) {
        try {
            if (x == 0) {
                throw new ArithmeticException();
            }
            throw new NullPointerException();
        } catch (ArithmeticException e) {
            return 10;
        } catch (NullPointerException e) {
            return 20;
        }
    }

    public static int run() {
        pick(true);                 // exercise areturn at runtime; result discarded
        return thrown() + classify(0) + classify(1); // 1 + 10 + 20 = 31
    }
}
