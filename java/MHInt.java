import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// MH-d: primitive-typed MethodHandle. `int.class` → Integer.TYPE (a primitive Class mirror);
// methodType(int.class, int.class) → "(I)I"; the handle invokes with a plain int, no boxing.
public class MHInt {
    static int twice(int x) {
        return x + x;
    }

    static int run() throws Throwable {
        MethodHandle h = MethodHandles.lookup()
            .findStatic(MHInt.class, "twice", MethodType.methodType(int.class, int.class));
        return (int) h.invoke(21); // 42
    }
}
