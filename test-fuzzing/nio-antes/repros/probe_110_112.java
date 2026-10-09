// Probe for findings #110 and #112 after refreshing the frozen compiler.
//   #110: a STATIC field of a CLASSPATH class must emit `getstatic`, not `getfield`.
//   #112: a `static final` primitive constant must be FOLDED at the use site, not read back.
public class probe_110_112 {

    // #110 — Integer.MAX_VALUE lives on a classpath class and is static.
    public static int classpathStatic() {
        return Integer.MAX_VALUE;
    }

    private static final int LOCAL_CONST = 7;

    // #112 — must compile to `bipush 7`, not `getstatic`.
    public static int ownConstant() {
        return LOCAL_CONST;
    }
}
