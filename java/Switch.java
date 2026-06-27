// Exercises both switch opcodes: a dense `switch` compiles to `tableswitch`, a sparse
// one to `lookupswitch`. Verifies (the verifier models both) and runs (the interpreter
// executes them).
public class Switch {
    static int dense(int x) {
        switch (x) {
            case 0:  return 100;
            case 1:  return 101;
            case 2:  return 102;
            default: return -1;
        }
    }

    static int sparse(int x) {
        switch (x) {
            case 0:    return 1;
            case 100:  return 2;
            case 1000: return 3;
            default:   return 0;
        }
    }

    static int run() {
        // dense: 100 + 102 + (-1) = 201 ; sparse: 1 + 3 + 0 = 4 ; total 205
        return dense(0) + dense(2) + dense(9) + sparse(0) + sparse(1000) + sparse(7);
    }
}
