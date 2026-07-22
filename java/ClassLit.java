// Exercises `ldc` of a Class constant (`Foo.class`) — the D1 unblocker. A class literal
// resolves to the class's `Class<…>` mirror, which is cached by Class ID, so the same
// literal must yield the *same* reference twice while different classes stay distinct.
//
// The mirrors are held as `Object` because `Class<ClassLit>` and `Class<String>` are
// incomparable generic types: javac rejects `==` between them, though the references
// underneath are exactly what this test is about.
public class ClassLit {
    public static int run() {
        Object self = ClassLit.class;
        Object other = String.class;

        if (self == null) return -1;

        // Identity: the mirror is cached, so a literal is stable across evaluations.
        Object selfAgain = ClassLit.class;
        if (self != selfAgain) return -2;

        // ...and distinct classes must not collapse onto one mirror.
        if (self == other) return -3;

        // The mirror is a real object the existing natives can work with.
        Object text = "hola";
        if (!String.class.isInstance(text)) return -4;
        if (ClassLit.class.isInstance(text)) return -5;

        // An *array* class literal names a class with no `.class` file at all, so it
        // gets the same synthetic mirror `anewarray` builds for array types.
        Object ints = int[].class;
        Object strings = String[].class;
        if (ints == null || strings == null) return -6;
        if (ints == strings) return -7;
        if (ints != int[].class) return -8; // cached like any other
        if (ints == self) return -9;

        return 42;
    }
}
