package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// It declares on the class what a BeanDescriptor would say: the description, and which the default
// property and the default event set are.
//
// Note: declared but not read in this tree; see Introspector's header.
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaBean {

    String description() default "";

    String defaultProperty() default "";

    String defaultEventSet() default "";
}
