// Exercises `SwitchBootstraps.typeSwitch` (D3): a `switch` over type patterns, which
// since Java 21 compiles to an `invokedynamic` whose call site answers *which case to
// run* as an index fed straight into a `tableswitch`.
//
// The `case null` arm is deliberate: without it javac guards the switch with
// `Objects.requireNonNull`, which would drag in `java.util` before the opcode under test
// even runs. With it, null becomes the call site's own -1 outcome.
//
// The helper classes are named `Critter`/`Kitten`/`Pebble` rather than the obvious
// Animal/Dog/Rock because `java/` already holds an `Animal` and a `Dog` fixture with real
// members, and same-named classes here would overwrite their `.class` files.
public class TypeSwitch {
    static int classify(Object o) {
        return switch (o) {
            case null      -> 0; // the call site answers -1
            case String s  -> 1; // label 0
            case Critter c -> 2; // label 1
            default        -> 3; // no label matched
        };
    }

    public static int run() {
        if (classify(null) != 0) return -1;
        if (classify("hola") != 1) return -2;
        if (classify(new Critter()) != 2) return -3;

        // A subclass must match its superclass label: the check walks the hierarchy
        // rather than comparing class identity.
        if (classify(new Kitten()) != 2) return -4;

        // Nothing matches → the call site answers labels.length, landing on `default`.
        if (classify(new Pebble()) != 3) return -5;

        return 42;
    }
}

class Critter {}

class Kitten extends Critter {}

class Pebble {}
