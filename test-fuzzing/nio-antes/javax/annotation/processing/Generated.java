package javax.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marks code a tool **generated**, to tell it apart from the code a person wrote.
//
// The retention is SOURCE, and that is the design decision that matters: the mark is of use to
// whoever reads the source and to the tools that process it, not to anyone at run time, so it has no
// reason to survive the compiler. (It is also why the retention warning `@SupportedOptions` and
// company carry does not apply here: this annotation never claimed to be visible at run time.)
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
          ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
public @interface Generated {

    /**
     * The generator's name or names. The JDK recommends the full name of the class that generated the
     * code, so that it is traceable.
     */
    String[] value();

    /** The generation date, in ISO 8601. Empty if the tool did not set it. */
    String date() default "";

    /** Whatever comment the tool wants to leave. */
    String comments() default "";
}
