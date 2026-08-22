// Finding #118 — ACC_VARARGS is never emitted, and a spread call to a CLASSPATH varargs
// method is silently DELETED.
//
// Two halves:
//
// (a) Write side. A `T...` parameter compiles to the right descriptor (`[Ljava/lang/Object;`)
//     but the method never gets ACC_VARARGS:
//       ours:       public static String join(String, Object[]);  flags: (0x0009) PUBLIC, STATIC
//       real javac: public static String join(String, Object...); flags: (0x0089) PUBLIC, STATIC, VARARGS
//
// (b) Read side — the dangerous half. A caller compiled against that .class cannot tell the
//     method is varargs, finds no applicable overload for a spread call, and emits NOTHING —
//     no diagnostic, no invoke, no array. `spread()` below compiles to:
//         ldc "-" ; ldc "a" ; ldc "b" ; areturn
//     which returns "b" and leaves two operands stranded on the stack (the real JVM verifier
//     would reject it). Passing the array explicitly is correct, and is the workaround.
//
// A same-FILE varargs call is also correct, because the compiler still has the source AST and
// never consults the flag — which is why this went unnoticed for so long.
//
// Same unguarded overload-lookup-failure path as #114: a call that resolves to nothing must be
// a compile error, never an empty expression. Closest relatives are #108 and #111.
//
// To see it: compile Va.java, put Va.class on a dir, then compile finding_118.java with
// `-cp <that dir>` and read `javap -c` on the result.
public class finding_118 {

    public String spread() {
        return Va118.join("-", "a", "b");      // BROKEN: the whole call is deleted
    }

    public String explicitArray() {
        Object[] parts = new Object[2];
        parts[0] = "a";
        parts[1] = "b";
        return Va118.join("-", parts);         // OK: this is the workaround
    }

    public String none() {
        return Va118.join("-");                // BROKEN too: no empty array is built
    }
}

// Compile this one FIRST, on its own, and put the .class on the -cp.
class Va118 {
    static String join(String sep, Object... parts) {
        return sep;
    }
}
