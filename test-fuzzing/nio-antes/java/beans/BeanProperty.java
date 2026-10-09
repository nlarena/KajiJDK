package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Everything a PropertyDescriptor carries, said in the declaration instead of in a separate
// BeanInfo class: whether the property is bound, whether it is for experts, how to describe it, and
// —when the value comes out of a closed list— which the valid values are.
//
// `bound` defaults to true, the other way round from the descriptor's field: whoever takes the
// trouble of annotating a property is nearly always exposing it to a tool that wants to hear about
// the changes.
//
// Note: declared but not read in this tree; see Introspector's header.
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanProperty {

    boolean bound() default true;

    boolean expert() default false;

    boolean hidden() default false;

    boolean preferred() default false;

    boolean required() default false;

    // Whether changing this property also changes how the bean looks.
    boolean visualUpdate() default false;

    String description() default "";

    // The valid values, in name/value/code triples, when the property is of a closed list.
    String[] enumerationValues() default {};
}
