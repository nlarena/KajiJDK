package javax.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to a method of <b>another</b> annotation, it says under which key its value goes into the
 * MBean's {@link Descriptor}.
 *
 * <p>That is: it is an annotation on annotations. It serves so that a domain annotation --say
 * {@code @Units("ms")}-- ends up as the pair {@code units=ms} in the operation's descriptor,
 * without the MBean server having to know {@code @Units}. Hence the {@code @Target(METHOD)}: the
 * target is the annotation's element, not the MBean's method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DescriptorKey {

    /** The descriptor key. */
    String value();
}
