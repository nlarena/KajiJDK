package javax.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks --or unmarks-- an interface as an MXBean.
 *
 * <p>It exists because the suffix convention is not enough. Without the annotation, an interface is
 * an MXBean only if its name ends in {@code MXBean}; with it the author decides explicitly, and
 * that is why the value is a {@code boolean} and not a bare marker: {@code @MXBean(false)} on
 * {@code FooMXBean} turns it into an ordinary interface, which is the only way to escape the
 * convention.
 *
 * <p>It is {@code RUNTIME} because whoever reads it is the MBean server when registering, not the
 * compiler.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MXBean {

    /** Whether the annotated interface is an MXBean. */
    boolean value() default true;
}
