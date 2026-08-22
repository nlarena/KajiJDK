// A7 #11: VM lifecycle — System.exit(int) / Runtime.
//
// The point of the test is what *doesn't* happen. `probe()` calls System.exit(42) inside a
// try/finally with more code after it: if exit really terminates the VM instead of returning,
// then the `return 1`, the `finally`, and everything `run()` would do afterwards are all dead.
// So the only way the harness can see 42 is if execution was cut at the exit call itself —
// any other outcome (1, 8, 108...) means exit merely returned.
public class ExTest {
    static int probe() {
        try {
            // Sanity check on Runtime before exiting: the singleton resolves and its native
            // reports a real CPU count. Cheap, and it proves getRuntime() ran at all.
            if (Runtime.getRuntime().availableProcessors() > 0) {
                System.exit(42);
            }
            return 1; // unreachable: exit does not return
        } finally {
            ExMarker.value = 7; // must NOT run — exit does not unwind, so no finally
        }
    }

    public static int run() {
        int value = probe();
        return 100 + value + ExMarker.value; // unreachable too
    }
}

// Separate class so the `finally` writes somewhere observable — if the VM ever did unwind,
// the result would be 108 instead of 42.
class ExMarker {
    static int value = 0;
}
