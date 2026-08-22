package java.lang.invoke;

import java.lang.constant.Constable;
import java.util.List;
import java.util.Optional;

// A typed, directly executable reference to a method, field or constructor. This is the class
// where a pure-Java standard library stops being possible, and it is worth being precise about
// WHY rather than just marking it unfinished.
//
// `invoke` and `invokeExact` are SIGNATURE-POLYMORPHIC (JLS 15.12.3). They are declared once, as
// `(Object...)Object`, but a call site does not adapt its arguments to that signature: the
// compiler emits an `invokevirtual` whose descriptor is whatever the CALL SITE looks like, and
// the VM links it against this single declaration. So `mh.invokeExact(1, "x")` compiles to a
// descriptor of `(ILjava/lang/String;)...` — a method that exists nowhere. No amount of Java can
// implement that; the resolution rule lives in the VM.
//
// That is why the two are `native` here, exactly as in the JDK. What the library CAN own is the
// type and the adaptations — and those adaptations still need a way to build a new handle, which
// is `MethodHandles`' job, so they throw for now rather than lie.
//
// KajiLibrary's own lambdas do not go through this class: our `invokedynamic` recognises
// `LambdaMetafactory` by name and spins the implementing class in Rust, never loading a handle.
public abstract class MethodHandle implements Constable {

    private final MethodType type;

    MethodHandle(MethodType type) {
        this.type = type;
    }

    public MethodType type() {
        return type;
    }

    // Signature-polymorphic — see above. `invokeExact` requires the call site's descriptor to
    // match the handle's type EXACTLY; `invoke` allows the same conversions an assignment would.
    public final native Object invokeExact(Object[] args) throws Throwable;

    public final native Object invoke(Object[] args) throws Throwable;

    // The reflective escape hatch: ordinary arity, arguments boxed, conversions applied. Being a
    // normal method, it is the one form of invocation a library could implement — given a way to
    // execute the underlying member, which is the part that is missing.
    public Object invokeWithArguments(Object[] args) throws Throwable {
        throw new UnsupportedOperationException("no method handle execution without VM support");
    }

    public Object invokeWithArguments(List<?> args) throws Throwable {
        throw new UnsupportedOperationException("no method handle execution without VM support");
    }

    // Every adaptation below returns a NEW handle wrapping this one. They are pure structure —
    // no invocation — but they still need a handle factory to produce the wrapper.
    public final MethodHandle asType(MethodType newType) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public MethodHandle asSpreader(int pos, Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public MethodHandle withVarargs(boolean makeVarargs) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public MethodHandle asCollector(Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public MethodHandle asCollector(int pos, Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public MethodHandle asVarargsCollector(Class<?> arrayType) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    public boolean isVarargsCollector() {
        return false;
    }

    public MethodHandle asFixedArity() {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    // Binds a leading argument — currying, in effect.
    public MethodHandle bindTo(Object x) {
        throw new UnsupportedOperationException("no method handle adaptation without a factory");
    }

    // A handle is describable only when it is DIRECT — one that points straight at a member. An
    // adapted or bound handle has no constant-pool form, which is what the empty `Optional`
    // means. Raw `Optional` for the same override reason as in `MethodType`.
    public Optional describeConstable() {
        return Optional.empty();
    }

    public String toString() {
        return "MethodHandle" + type.toString();
    }
}
