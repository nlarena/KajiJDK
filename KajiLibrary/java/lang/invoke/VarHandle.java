package java.lang.invoke;

import java.lang.constant.Constable;
import java.util.List;
import java.util.Optional;

// A typed reference to a VARIABLE — a field, an array element, a slice of a buffer — through
// which it can be read and written under a chosen memory ordering. It is the API that finally
// gave Java a supported way to do what `sun.misc.Unsafe` was being used for: plain, opaque,
// acquire/release and volatile accesses, plus compare-and-set, from ordinary code.
//
// Its accessors are SIGNATURE-POLYMORPHIC, the same mechanism as `MethodHandle.invoke`: one
// declaration of `(Object...)Object` that the VM links against whatever the call site looks like.
// That, plus the fact that the orderings are properties of the memory model rather than of any
// library code, is why this class cannot be written in Java — the declarations below are native
// exactly as in the JDK.
//
// OMITTED (subset): the nested `AccessMode` enum and the ~30 methods that mention it, and the
// remaining ordering variants of each accessor. KajiLibrary's `java.util.concurrent.atomic`
// already covers what this project needs from the area, and it does it with `synchronized` —
// which in a VM that interleaves BETWEEN opcodes is observationally identical to a CAS.
public abstract class VarHandle implements Constable {

    VarHandle() {
    }

    public final native Object get(Object[] args);

    public final native void set(Object[] args);

    public final native Object getVolatile(Object[] args);

    public final native void setVolatile(Object[] args);

    public final native boolean compareAndSet(Object[] args);

    public final native Object compareAndExchange(Object[] args);

    public final native Object getAndSet(Object[] args);

    public final native Object getAndAdd(Object[] args);

    // The type of the variable itself, and the types needed to LOCATE it — for an instance field
    // the receiver, for an array element the array and the index. Separating the two is what lets
    // one handle serve every element of an array.
    public Class<?> varType() {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    public List<Class<?>> coordinateTypes() {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    public Optional describeConstable() {
        return Optional.empty();
    }
}
