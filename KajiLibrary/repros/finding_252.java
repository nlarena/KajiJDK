/**
 * A method call whose RECEIVER's static type is a type variable is dropped in silence, and the
 * `!` in front of it turns into an int branch over the leftover reference — a class the verifier
 * should reject and the interpreter panics on.
 *
 * Compile and disassemble:
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_252.java
 *   bin/jvm.exe -v KajiLibrary/repros/finding_252.class
 *
 * Expected of `porVariable`: `aload_1; aload_2; invokevirtual Object.equals; ifne ...`.
 * Actual: `aload_2` (the ARGUMENT alone, receiver and call gone) followed by `ifne` — an int
 * branch applied to a reference. `porObject` is the control: the same body with the receiver
 * declared `Object` compiles correctly.
 *
 * The StackMapTable of `porVariable` shows the third face of the same bug: it types the locals
 * as `class T`, as if the type variable were a class of that name, instead of erasing them to
 * `java/lang/Object`.
 *
 * Surfaced in ConcurrentSkipListMap.replace(K, V, V), whose `!oldValue.equals(held)` became
 * `aload held; ifne` and panicked the VM at bifurcation_operations.rs:246.
 */
public class finding_252<T> {

    /** Receiver typed as the type variable. */
    public boolean porVariable(T a, T b) {
        if (!a.equals(b)) {
            return false;
        }
        return true;
    }

    /** Receiver typed as Object — the control. */
    public boolean porObject(Object a, Object b) {
        if (!a.equals(b)) {
            return false;
        }
        return true;
    }

    /** The same call without the `!`, to separate "the call is dropped" from "the branch is wrong". */
    public boolean sinNegar(T a, T b) {
        if (a.equals(b)) {
            return true;
        }
        return false;
    }

    /** A no-argument call on a type variable, to show it is the receiver and not the argument. */
    public int hashDeVariable(T a) {
        return a.hashCode();
    }
}
