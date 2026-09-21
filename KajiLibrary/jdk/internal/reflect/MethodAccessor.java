package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;

/**
 * KajiLibrary's jdk.internal.reflect.MethodAccessor -- the contract of "invoke this method".
 *
 * <h2>Why this interface exists, and why here it means something different than in HotSpot</h2>
 *
 * <p>In the JDK the arrow goes {@code Method.invoke} &rarr; {@code MethodAccessor.invoke}: {@code
 * Method} does the check of access once, manufactures an accessor and delegates every call to it.
 * The accessor is the machinery, and {@code Method} the shell.
 *
 * <p>In this VM the arrow goes the other way round. {@code Method.invoke} is an <strong>intrinsic
 * of the interpreter</strong> ({@code Intrinsic::MethodInvoke}): the opcode of invocation
 * recognises it by {@code (class, name, descriptor)} and pushes the frame of the destination method
 * instead of running the Java body, which is why that one throws. The machinery is below the floor
 * of Java and there is no accessor in the middle.
 *
 * <p>That does not make this interface useless: it makes it <strong>a pure declaration</strong>,
 * which is what it always was. An interface has no bodies that can lie; it declares a shape --"an
 * object that knows how to invoke a method"-- and that shape is exactly that of the intrinsic that
 * already works. Whoever wants an accessor asks for it with {@link
 * ReflectionFactory#newMethodAccessor}, which returns one plugged into that machinery and not into
 * a second copy of it.
 *
 * <p>The precedent of the criterion is written in {@code jdk.internal.vm.VMSupport}: the nested
 * interface {@code AnnotationDecoder} came in for the same reason, "a pure declaration whose
 * contract does not depend on there being somebody who uses it".
 */
public interface MethodAccessor {

    /**
     * It invokes the method on {@code obj} with {@code args}.
     *
     * @param obj the receiver, or {@code null} if the method is static
     * @param args the arguments, already in the order of the parameters
     * @return the result, boxed if the return type is primitive; {@code null} if it is {@code void}
     * @throws IllegalArgumentException if the receiver or the arguments do not correspond
     * @throws InvocationTargetException wrapping whatever the destination method threw
     */
    Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException;

    /**
     * The same as {@link #invoke(Object, Object[])} but saying who calls.
     *
     * <p>The overload exists in the JDK because of the {@code @CallerSensitive} methods, which look
     * at the frame of whoever invoked them in order to decide what to answer: going through
     * reflection that frame would be that of the accessor, so the real caller has to be passed by
     * hand.
     *
     * <p>The note said that in this VM there was no machinery sensitive to the caller, neither
     * {@code Reflection.getCallerClass} nor the annotation that triggers it; both are there now.
     * What does hold is that the argument changes nothing here, and the reason is in
     * {@link Reflection#getCallerClass()}: this VM interposes no frame when invoking reflectively,
     * so the walk of the stack already sees the real caller. The signature is declared all the same
     * because it is part of the contract.
     *
     * @param obj the receiver, or {@code null} if the method is static
     * @param args the arguments
     * @param caller the class that passes itself off as the caller
     * @return the result, boxed if the return type is primitive
     * @throws IllegalArgumentException if the receiver or the arguments do not correspond
     * @throws InvocationTargetException wrapping whatever the destination method threw
     */
    Object invoke(Object obj, Object[] args, Class<?> caller)
            throws IllegalArgumentException, InvocationTargetException;
}
