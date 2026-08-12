import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// MH-d: a virtual MethodHandle (REF_invokeVirtual) — findVirtual + invoke dispatch on the receiver.
public class MHVirtual {
    static int run() throws Throwable {
        MethodHandle h = MethodHandles.lookup()
            .findVirtual(String.class, "length", MethodType.methodType(int.class));
        return (int) h.invoke("hello"); // 5
    }
}
