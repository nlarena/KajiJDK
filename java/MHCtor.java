import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// MH-d: a constructor MethodHandle (REF_newInvokeSpecial) — findConstructor + invoke builds a Box.
public class MHCtor {
    static int run() throws Throwable {
        MethodHandle h = MethodHandles.lookup()
            .findConstructor(Box.class, MethodType.methodType(void.class, int.class));
        Box b = (Box) h.invoke(42);
        return b.get(); // 42
    }
}

class Box {
    int v;
    Box(int v) { this.v = v; }
    int get() { return this.v; }
}
