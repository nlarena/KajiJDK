// Exercises a record whose components include a **reference**. Everything here depends on
// asking the component itself: equality is `equals`, not `==`; the hash folds the
// component's own `hashCode`; and the text comes from its `toString`.
public class RecStrOps {
    public static int run() {
        RecStr a = new RecStr("bob", 1);
        RecStr b = new RecStr("bob", 1);
        RecStr c = new RecStr("ann", 1);

        // The two "bob" literals are distinct objects (no interning), so `==` is false
        // while `equals` must be true — the exact case identity comparison gets wrong.
        if (a == b) return -1;
        if (!a.equals(b)) return -2;
        if (a.equals(c)) return -3;
        if (a.equals(null)) return -4;

        // Equal values hash equally, and the hash folds the String's own.
        if (a.hashCode() != b.hashCode()) return -5;
        if (a.hashCode() != 3029228) return -6;

        // toString asks the component for its text.
        if (!a.toString().equals("RecStr[name=bob, age=1]")) return -7;

        return 42;
    }
}
