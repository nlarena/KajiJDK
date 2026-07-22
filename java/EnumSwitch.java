// Exercises D4: a `switch` over enum *patterns*. Each case label compiles to a dynamic
// constant (condy) built by ConstantBootstraps.invoke, which calls ClassDesc.of and
// Enum$EnumDesc.of — so running this needs the VM to evaluate constants by invoking Java.
public class EnumSwitch {
    static int classify(Object o) {
        return switch (o) {
            case null       -> 0;
            case Tone.RED   -> 1;
            case Tone.GREEN -> 2;
            default         -> 3;
        };
    }

    public static int run() {
        if (classify(null) != 0) return -1;
        if (classify(Tone.RED) != 1) return -2;
        if (classify(Tone.GREEN) != 2) return -3;
        if (classify("x") != 3) return -4;
        return 42;
    }
}
