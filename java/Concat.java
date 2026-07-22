// Exercises `invokedynamic` (0xba). Since Java 9 every `+` on strings compiles to an
// indy call site bootstrapped by StringConcatFactory.makeConcatWithConstants, so this
// whole class is one long test of that one opcode. Each negative return pins a distinct
// failure mode.
public class Concat {
    public static int run() {
        int n = 7;
        String name = "kaji";
        char c = 'A';
        boolean flag = true;
        long big = 42L;

        // Literal text on both sides of a spliced argument: the recipe carries the
        // text, the marker carries the value.
        if (!("n=" + n + "!").equals("n=7!")) return -1;

        // A String argument is read back out of the heap.
        if (!("hola " + name).equals("hola kaji")) return -2;

        // char must render as the character, not its numeric code — a `char` travels
        // as an Int, so only the descriptor can tell 'A' from 65.
        if (!("c=" + c).equals("c=A")) return -3;

        // boolean, likewise: true/false, not 1/0.
        if (!("f=" + flag).equals("f=true")) return -4;

        // A category-2 argument.
        if (!("l=" + big).equals("l=42")) return -5;

        // Several arguments spliced by one call site, no literal text at all.
        if (!(name + n + c).equals("kaji7A")) return -6;

        // A null reference renders as "null" rather than blowing up.
        String nothing = null;
        if (!("x" + nothing).equals("xnull")) return -7;

        return ("n=" + n + "!").length() * 10 + 2; // 4 * 10 + 2 = 42
    }
}
