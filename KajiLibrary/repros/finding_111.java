// Finding #111 — a method call whose RECEIVER's static type is a TYPE VARIABLE is silently
// dropped: the compiler emits the *argument* in place of the call's result.
//   boolean viaTypeVar(Object o) { return value.equals(o); }  ->  aload_1; areturn
// (a reference returned from a `boolean` method — the call never happens).
// Binding the receiver to an Object local first compiles correctly. Same silent-drop class
// as #108; found in ConcurrentHashMap.remove(key, value), which branched on its argument.
public class finding_111<T> {
    T value;
    boolean viaTypeVar(Object o) {
        return value.equals(o);        // BROKEN: call dropped
    }
    boolean viaObject(Object o) {
        Object v = value;
        return v.equals(o);            // OK: workaround
    }
}
