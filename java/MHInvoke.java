import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// H4/0xba — java.lang.invoke object model: build a MethodHandle via findStatic and call it
// through signature-polymorphic `invoke`. Reference-typed (String) to sidestep primitive mirrors.
public class MHInvoke {
    static String id(String s) {
        return s;
    }

    static int run() throws Throwable {
        MethodHandles.Lookup l = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(String.class, String.class);
        MethodHandle h = l.findStatic(MHInvoke.class, "id", mt);
        String r = (String) h.invoke("hello");
        return r.length(); // 5 if the handle round-tripped the string
    }
}
