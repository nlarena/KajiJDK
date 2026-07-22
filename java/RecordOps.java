// Exercises `ObjectMethods.bootstrap` (D5): a record's `equals`/`hashCode`/`toString`,
// all three bootstrapped from a *single* BootstrapMethods entry and told apart only by
// the call site's name. Drives the existing `Point` fixture.
public class RecordOps {
    public static int run() {
        Point a = new Point(1, 2);
        Point b = new Point(1, 2);
        Point c = new Point(1, 3);

        // equals is by value, not identity: two distinct objects compare equal.
        if (a == b) return -1;
        if (!a.equals(b)) return -2;
        if (a.equals(c)) return -3;
        if (a.equals(null)) return -4;
        if (a.equals("x")) return -5; // a different class is never equal

        // hashCode folds the components as 31*accumulator + hash, from zero:
        // Point(1, 2) -> 1*31 + 2 = 33.
        if (a.hashCode() != 33) return -6;
        if (a.hashCode() != b.hashCode()) return -7; // equal values, equal hashes
        if (new Point(0, 0).hashCode() != 0) return -8;
        if (new Point(1, 0).hashCode() != 31) return -9;

        // toString uses the class's simple name and the component names.
        if (!a.toString().equals("Point[x=1, y=2]")) return -10;

        return 42;
    }
}
