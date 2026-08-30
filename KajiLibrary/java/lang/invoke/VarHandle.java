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
// The full access-mode grid is now declared. It is worth reading it as a GRID rather than as a
// list of 31 methods, because that is what it is: five *shapes* of access (read, write,
// compare-and-set, compare-and-exchange, read-modify-write) crossed with the memory orderings
// the shape admits. The naming is regular and carries the ordering as a suffix:
//
//   (none)     plain     — no ordering at all beyond what the variable's own declaration gives
//   Opaque     opaque    — the access happens, and cannot be elided or reordered with ITSELF
//   Acquire    acquire   — a read after which nothing may be hoisted
//   Release    release   — a write before which nothing may be sunk
//   Volatile   volatile  — full sequential consistency with other volatile accesses
//
// so `getAcquire` is a read with acquire semantics, `setRelease` a write with release semantics,
// and `compareAndExchangeAcquire` a CAS whose read half is an acquire and whose write half is
// plain. `weakCompareAndSetPlain` is the one whose name says the ordering it does NOT have.
//
// `AccessMode` — the grid REIFIED — is now declared, together with the three methods that take
// it. The earlier note here argued that "declaring the enum without the method that gives it a
// purpose would add 31 constants of pure decoration", and that was true while the enum stood
// alone; it stops being true once `accessModeType`, `isAccessModeSupported` and `toMethodHandle`
// are there, because those three are what turn the grid from a naming convention into data a
// caller can iterate. `MethodHandles.varHandleInvoker` needs the enum for the same reason, and
// with it the last two holes of `MethodHandles` close as well.
//
// NOT modelled inside it: the JDK's package-private `AccessType`, which groups the modes by the
// SHAPE of their signature. It is a HotSpot internal (it exists to compute `accessModeType`
// against a real handle), it is not public API, and nothing here can use it. `methodName` — the
// half of the JDK's pair that IS observable, through `methodName()` and `valueFromMethodName` —
// is kept.
public abstract class VarHandle implements Constable {

    VarHandle() {
    }

    // ---- read ----

    public final native Object get(Object... args);

    public final native Object getVolatile(Object... args);

    public final native Object getOpaque(Object... args);

    public final native Object getAcquire(Object... args);

    // ---- write ----

    public final native void set(Object... args);

    public final native void setVolatile(Object... args);

    public final native void setOpaque(Object... args);

    public final native void setRelease(Object... args);

    // ---- compare-and-set ----
    //
    // The `weak` family may fail SPURIOUSLY — return false with the variable still holding the
    // expected value. That is not a defect to be worked around but the point: on the hardware
    // where a CAS is a load-linked/store-conditional pair, letting the store fail for reasons of
    // its own is what makes the loop cheap. Consequently every `weak*` call belongs inside a
    // retry loop, and the non-weak `compareAndSet` is the one that may be used bare.

    public final native boolean compareAndSet(Object... args);

    public final native boolean weakCompareAndSet(Object... args);

    public final native boolean weakCompareAndSetPlain(Object... args);

    public final native boolean weakCompareAndSetAcquire(Object... args);

    public final native boolean weakCompareAndSetRelease(Object... args);

    // ---- compare-and-exchange ----
    //
    // Same operation as `compareAndSet`, except the answer is the WITNESSED value rather than a
    // boolean. A retry loop written over this one does not have to re-read the variable after a
    // failure, because the failure already told it what it found.

    public final native Object compareAndExchange(Object... args);

    public final native Object compareAndExchangeAcquire(Object... args);

    public final native Object compareAndExchangeRelease(Object... args);

    // ---- read-modify-write ----
    //
    // Each returns the value the variable held BEFORE the update, which is what makes them
    // composable: the caller learns the old state and the new state in one atomic step.

    public final native Object getAndSet(Object... args);

    public final native Object getAndSetAcquire(Object... args);

    public final native Object getAndSetRelease(Object... args);

    public final native Object getAndAdd(Object... args);

    public final native Object getAndAddAcquire(Object... args);

    public final native Object getAndAddRelease(Object... args);

    public final native Object getAndBitwiseOr(Object... args);

    public final native Object getAndBitwiseOrAcquire(Object... args);

    public final native Object getAndBitwiseOrRelease(Object... args);

    public final native Object getAndBitwiseAnd(Object... args);

    public final native Object getAndBitwiseAndAcquire(Object... args);

    public final native Object getAndBitwiseAndRelease(Object... args);

    public final native Object getAndBitwiseXor(Object... args);

    public final native Object getAndBitwiseXorAcquire(Object... args);

    public final native Object getAndBitwiseXorRelease(Object... args);

    // ---- invocation behaviour ----
    //
    // A `VarHandle` accepts either EXACT argument types (like `MethodHandle.invokeExact`) or
    // types that an assignment conversion would accept (like `MethodHandle.invoke`). The choice
    // is a property of the handle, not of the call site, and these three read and switch it. The
    // two switches are abstract for the same reason as in the JDK: producing the sibling handle
    // is the concrete subclass's job, and there are no concrete subclasses here.

    public boolean hasInvokeExactBehavior() {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    public abstract VarHandle withInvokeExactBehavior();

    public abstract VarHandle withInvokeBehavior();

    // ---- shape ----

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

    public final String toString() {
        return "VarHandle";
    }

    // ---- standalone fences ----
    //
    // Ordering with no access attached: a barrier the surrounding code may lean on. They are the
    // one part of this class that is not about a particular variable, which is why they are
    // static.
    //
    // In the JDK these are ordinary Java bodies that delegate to `Unsafe` intrinsics; here there
    // is no `Unsafe` to delegate to, so the barrier has to come from the VM. An earlier revision
    // put `native` on the public methods themselves, which cost nothing semantically but made
    // their `ACC_NATIVE` flag differ from the JDK's for five members of the public surface.
    //
    // The shape below keeps both halves: the PUBLIC method is an ordinary Java body with exactly
    // the JDK's flags, and the intrinsic sits one layer down in a private `native` sibling. That
    // is the same arrangement the JDK uses, with `Unsafe.fullFence` playing the part of `fence0`.
    // What must NOT be done is give the public method an empty body: under `JVM_THREADS=os` —
    // real OS threads, real hardware reordering — a no-op fence is a silently wrong answer
    // rather than a missing one, and the `native` sibling is what keeps it from becoming one.

    public static void fullFence() {
        fullFence0();
    }

    public static void acquireFence() {
        acquireFence0();
    }

    public static void releaseFence() {
        releaseFence0();
    }

    public static void loadLoadFence() {
        loadLoadFence0();
    }

    public static void storeStoreFence() {
        storeStoreFence0();
    }

    private static native void fullFence0();

    private static native void acquireFence0();

    private static native void releaseFence0();

    private static native void loadLoadFence0();

    private static native void storeStoreFence0();

    // ---- the access grid, reified ----

    // Every access mode this class declares a method for, as data. The JDK pairs each constant
    // with the NAME of the method that performs it, which is what makes the enum useful for
    // reflection over the grid: `valueFromMethodName("getAcquire")` is how a tool goes from the
    // name it read somewhere back to the mode. The order is the JDK's, because `ordinal()` is
    // part of the observable behaviour of an enum and the JDK's own internals index by it.
    public enum AccessMode {

        GET("get"),
        SET("set"),
        GET_VOLATILE("getVolatile"),
        SET_VOLATILE("setVolatile"),
        GET_ACQUIRE("getAcquire"),
        SET_RELEASE("setRelease"),
        GET_OPAQUE("getOpaque"),
        SET_OPAQUE("setOpaque"),
        COMPARE_AND_SET("compareAndSet"),
        COMPARE_AND_EXCHANGE("compareAndExchange"),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire"),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease"),
        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain"),
        WEAK_COMPARE_AND_SET("weakCompareAndSet"),
        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire"),
        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease"),
        GET_AND_SET("getAndSet"),
        GET_AND_SET_ACQUIRE("getAndSetAcquire"),
        GET_AND_SET_RELEASE("getAndSetRelease"),
        GET_AND_ADD("getAndAdd"),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire"),
        GET_AND_ADD_RELEASE("getAndAddRelease"),
        GET_AND_BITWISE_OR("getAndBitwiseOr"),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease"),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire"),
        GET_AND_BITWISE_AND("getAndBitwiseAnd"),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease"),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire"),
        GET_AND_BITWISE_XOR("getAndBitwiseXor"),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease"),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire");

        private final String methodName;

        private AccessMode(String methodName) {
            this.methodName = methodName;
        }

        // The name of the `VarHandle` method that performs this access. Note it is NOT the
        // constant's own name lowercased: `WEAK_COMPARE_AND_SET_PLAIN` maps to
        // `weakCompareAndSetPlain`, and the mapping is stored rather than computed precisely so
        // that renaming a constant cannot silently rename a method.
        public String methodName() {
            return methodName;
        }

        // The inverse. Linear over 31 constants instead of the JDK's map, which is the right
        // trade at this size and avoids depending on a hash container this early in the library.
        public static AccessMode valueFromMethodName(String methodName) {
            AccessMode[] all = values();
            int i = 0;
            while (i < all.length) {
                AccessMode candidate = all[i];
                String name = candidate.methodName;
                if (name.equals(methodName)) {
                    return candidate;
                }
                i = i + 1;
            }
            throw new IllegalArgumentException("No AccessMode value for method name " + methodName);
        }
    }

    // ---- the grid, asked about ----

    // Whether this handle admits the mode at all. Not every variable admits every mode: a
    // `getAndAdd` needs a numeric type, a bitwise mode needs an integral one, and a `final`
    // field admits no write. So the grid is the full vocabulary and this is the per-handle
    // subset of it.
    public boolean isAccessModeSupported(AccessMode accessMode) {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    // The signature the mode's call site must have for THIS handle: the coordinate types, then
    // whatever the shape adds (an expected and a new value for a CAS), returning what the shape
    // returns. It is `final` in the JDK because it is derived from `varType` and
    // `coordinateTypes` rather than chosen by a subclass.
    public final MethodType accessModeType(AccessMode accessMode) {
        throw new UnsupportedOperationException("no VarHandle without VM support");
    }

    // The escape hatch out of signature polymorphism: an ordinary `MethodHandle` for one mode of
    // this handle, which can then be fed to the combinators in `MethodHandles`. It cannot be
    // built here for the same reason nothing else can build one.
    public MethodHandle toMethodHandle(AccessMode accessMode) {
        throw new UnsupportedOperationException("no MethodHandle without VM support");
    }
}
