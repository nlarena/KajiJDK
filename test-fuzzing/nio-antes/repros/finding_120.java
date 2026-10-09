// Finding #120 — a call to a method the receiver's static type only INHERITS from an EXTERNAL
// superclass is silently deleted, leaving verifier-invalid bytecode.
//
// `WeakReference.get()` is declared on its superclass `Reference`. Calling it through a
// `WeakReference`-typed receiver produces NO invoke at all:
//
//     public Object viaWeak(WeakReference w) { return w.get(); }
//   compiles to:
//     0: areturn        <-- with an EMPTY operand stack
//
// That is worse than #111 and #108, which at least left the arguments behind: here the method
// body returns whatever is not on the stack. Observed in the wild as `astore 5` with nothing
// pushed (WeakHashMap.entryFor).
//
// Inheritance within the SAME compilation unit is unaffected, so this only bites across the
// classpath — the same blind spot as #110 (ACC_STATIC), #104 (Exceptions) and #118
// (ACC_VARARGS): the class-file reader does not carry the supertype's method table.
//
// WORKAROUND: cast the receiver to the class that actually DECLARES the method.
//     Reference r = (Reference) w; r.get();   -> emits invokevirtual java/lang/ref/Reference.get
// Note that the plain widening assignment `Reference r = w;` is rejected outright
// ("tipo incompatible"), so the explicit cast is required.
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class finding_120 {

    // BROKEN: get() is inherited from Reference, so the call vanishes.
    public Object viaSubclass(WeakReference w) {
        return w.get();
    }

    // OK: casting to the declaring class emits the invokevirtual.
    public Object viaDeclaringClass(WeakReference w) {
        Reference r = (Reference) w;
        return r.get();
    }
}
