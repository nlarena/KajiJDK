import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// Exercises MethodHandle.invokeWithArguments — spread an Object[] into the handle (the VM primitive
// that ConstantBootstraps.invoke is built on). Reference-typed to avoid primitive mirrors.
public class MHSpread {
    static String id(String s) {
        return s;
    }

    static int run() throws Throwable {
        MethodHandle h = MethodHandles.lookup()
            .findStatic(MHSpread.class, "id", MethodType.methodType(String.class, String.class));
        Object[] args = new Object[] { "spread!" };
        return ((String) h.invokeWithArguments(args)).length(); // "spread!".length() = 7
    }
}
