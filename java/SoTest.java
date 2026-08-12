// A7 #4 (JVMS §6.3): StackOverflowError. `deep` recurses with no base case, so the frame
// stack hits the VM's depth limit (MAX_FRAMES) and the invoke throws a catchable
// java.lang.StackOverflowError instead of growing without bound. The catch turns the
// overflow into the oracle value → green ≡ os-gil ≡ os = 42.
public class SoTest {
    static int deep(int n) {
        return deep(n + 1);
    }

    static int run() {
        try {
            return deep(0); // never returns normally: overflows the frame stack
        } catch (StackOverflowError e) {
            return 42;
        }
    }
}
