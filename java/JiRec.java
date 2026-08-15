// F3 step 8 — **recursion, which must be cut rather than expanded**.
//
// A method that calls itself would inline into itself without end, and the honest reason to stop is
// not a depth bound but identity: the compiler refuses a callee whose `Unit` is already on the path
// from the root. Both shapes are here, because a depth bound alone would catch neither cleanly:
//
//   - `down` calls itself — the direct cycle, caught at the first expansion;
//   - `mutualA` and `mutualB` call each other — a cycle no per-method check would see, caught two
//     expansions in.
//
// What the test asserts is the thing a missing cycle check would take away: that compiling
// **terminates**, and that the answer is the interpreter's. `sum` is here so the file is not
// entirely uncompilable — it is a leaf that inlines normally, which is what makes "the recursive
// ones were refused" a statement about them rather than about the whole file.
public class JiRec {
    static int sum(int a, int b) {
        return a + b;
    }

    static int down(int n) {
        return n <= 0 ? 0 : down(n - 1) + 1;
    }

    static int mutualA(int n) {
        return n <= 0 ? 0 : mutualB(n - 1) + 1;
    }

    static int mutualB(int n) {
        return n <= 0 ? 0 : mutualA(n - 1) + 2;
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 300; i++) {
            acc = (acc + sum(down(6), mutualA(5))) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
