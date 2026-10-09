package jdk.internal.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's jdk.internal.reflect.CallerSensitive -- it marks a method whose behaviour depends
 * on <b>who called it</b>.
 *
 * <p>{@code Class.forName(String)} loads with the loader of its caller.
 * {@code MethodHandles.lookup()} returns the permissions of its caller. Neither of the two can
 * answer without looking at the stack, and that is why they call
 * {@link Reflection#getCallerClass()}.
 *
 * <h2>Why the mark is needed</h2>
 *
 * <p>{@code getCallerClass()} returns the frame above the one that calls it, and that breaks if
 * something gets in the middle: invoking {@code Class.forName} by reflection would put the frames
 * of the machinery of reflection between the real caller and the method, and the result would be
 * the loader of that machinery instead of that of whoever asked. The mark is what tells the runtime
 * "when you invoke this reflectively, do not change who appears to be the caller".
 *
 * <p>From there comes its most important condition, and it is one that cannot be verified by
 * itself: a marked method <b>must not</b> be public and at the same time delegate to another marked
 * one, because then the second would see the first as its caller and not the one from outside. The
 * JDK checks it with a separate tool.
 *
 * <p>Retention at runtime because {@link Reflection#isCallerSensitive} reads it from a
 * {@code java.lang.reflect.Method}, not from the source code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CallerSensitive {
}
