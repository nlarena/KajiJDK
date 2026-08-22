// Differential workload for the F3 JIT, group 3 — dimension: the **cold call site**.
//
// The JIT learns what an invoke binds to from the F0 quickened call site, which the interpreter
// fills the first time it *executes* that pc. That is normally a better filter than resolution:
// inlining is offered only for calls that have really happened. But a hot method can contain a call
// it has never made — an untaken branch, an error path — and such a site leaves its cell at zero.
// One of them used to refuse the whole method, however hot the rest of it was.
//
// `hot` is exactly that shape: a loop that never takes its `if`, so `cold(…)` is never executed and
// its site is never quickened, while `warm(…)` right above it is executed every iteration. For the
// method to compile, the compiler has to resolve the cold site out of the metaspace read-only —
// which it can, because `cold` has been resolved from elsewhere (`prime` calls it once) and a
// statically bound call needs nothing from the receiver.
//
// `flip` is the other half of the claim: the branch **is** taken there, so the body the cold site
// was compiled for is the body that runs, and the answer says so.
public class JcCold {
    static int warm(int x) {
        return (x * 3) + 1;
    }

    static int cold(int x) {
        return (x * 7) - 2;
    }

    // Called once, from outside any compiled method, purely so `cold` is a resolved method: this is
    // what the read-only lookup finds. Without it the class would still be loadable and the
    // compiler would still refuse, which is the honest boundary — compilation may not load.
    static int prime() {
        return cold(1);
    }

    // The hot method. `flag` is 0 for every call that matters, so the `cold(i)` site is never
    // executed and never quickened.
    static int hot(int n, int flag) {
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = (acc + warm(i)) & 0xFFFFF;
            if (flag != 0) {
                acc = (acc + cold(i)) & 0xFFFFF;
            }
        }
        return acc;
    }

    public static int run() {
        int score = prime();
        for (int round = 0; round < 60; round++) {
            score = (score + hot(200, 0)) & 0xFFFFF;
        }
        // Now take the branch the compilation was made without ever seeing taken.
        for (int round = 0; round < 20; round++) {
            score = (score + hot(200, 1)) & 0xFFFFF;
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
