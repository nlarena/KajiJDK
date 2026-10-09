package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@link MethodAccessor} {@link ReflectionFactory#newMethodAccessor} manufactures.
 *
 * <p>It delegates to {@link Method#invoke}, which in this VM is the intrinsic
 * {@code Intrinsic::MethodInvoke} of the interpreter. That inverts the arrow with respect to
 * HotSpot --over there {@code Method.invoke} calls the accessor, here the accessor calls
 * {@code Method.invoke}-- and that is why there is no cycle: the intrinsic does not come back to
 * Java, it pushes the frame of the destination method and that is that.
 *
 * <p>That the delegation is one line is the guarantee, not the suspicion: it means that there is no
 * second implementation of the reflective invocation that can fall out of step with the first. The
 * unpacking of arguments, the widening of primitives, the reboxing of the return and the wrapping
 * of the exception of the destination in {@link InvocationTargetException} happen once, inside the
 * interpreter ({@code reflective_call.rs}).
 */
final class MethodAccessorImpl implements MethodAccessor {

    private final Method method;

    MethodAccessorImpl(Method method) {
        this.method = method;
    }

    public Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        // `null` means "with no arguments", not "one null argument": it is what the contract of the
        // JDK promises and what the intrinsic expects to receive as an empty array.
        if (args == null) {
            return this.method.invoke(obj, new Object[0]);
        }
        return this.method.invoke(obj, args);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code caller} is ignored, and not out of convenience. The note said that neither
     * {@code @CallerSensitive} nor {@code Reflection.getCallerClass} existed in this library; both
     * are there now. What is true is the reason the ignoring rests on, and it is written in
     * {@link Reflection#getCallerClass()}: this VM interposes no frame of its own when invoking
     * reflectively --{@code Method.invoke} is an intrinsic that pushes the frame of the
     * destination-- so the walk of the stack already gives the real caller and there is nothing for
     * this argument to correct. The JDK does exactly this same thing --delegating to the two
     * argument form-- for every method that is not annotated.
     *
     * <p>With one observable difference that is as well noted down, because it was measured: in the
     * JDK 25 this overload <strong>cannot</strong> be called on an accessor that is not sensitive
     * to the caller. Its {@code DirectMethodHandleAccessor} puts together two different handles
     * according to the case, and calling the form of three on the handle of the common case throws
     * {@code IllegalArgumentException: argument type mismatch}. It is an artefact of how the handle
     * is put together and not something the interface promises; here there are not two handles, so
     * there is nothing that can fail to match, and the call works. That is why the test of
     * behaviour does not compare it between the two VMs: the only way of making them equal would be
     * to throw on purpose.
     */
    public Object invoke(Object obj, Object[] args, Class<?> caller)
            throws IllegalArgumentException, InvocationTargetException {
        return this.invoke(obj, args);
    }
}
