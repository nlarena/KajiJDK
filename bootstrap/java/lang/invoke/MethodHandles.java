package java.lang.invoke;

// Minimal java.lang.invoke.MethodHandles — the factory for lookups and, through a Lookup, for
// MethodHandles. `lookup()` hands back a capability object; `findStatic` records a static-method
// target into a MethodHandle. Access checks aren't modelled yet, so Lookup carries no state.
public class MethodHandles {
    public static Lookup lookup() {
        return new Lookup();
    }

    public static final class Lookup {
        Lookup() {
        }

        // Resolve a static method into a MethodHandle. REF_invokeStatic = 6 (JVMS Table 5.4.3.5-A).
        public MethodHandle findStatic(Class<?> refc, String name, MethodType type) {
            return new MethodHandle(refc, name, type.descriptorString(), 6);
        }

        // A virtual method — dispatched on the receiver at invoke time. REF_invokeVirtual = 5. The
        // `type` is the method's own (no receiver); the handle prepends it when invoked.
        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) {
            return new MethodHandle(refc, name, type.descriptorString(), 5);
        }

        // A constructor — the handle allocates and initialises, returning the new object.
        // REF_newInvokeSpecial = 8. `type`'s return is `void`; its parameters are the ctor's.
        public MethodHandle findConstructor(Class<?> refc, MethodType type) {
            return new MethodHandle(refc, "<init>", type.descriptorString(), 8);
        }
    }
}
