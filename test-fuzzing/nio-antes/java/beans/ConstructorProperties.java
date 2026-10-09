package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// It says which properties a constructor's parameters correspond to, in order. It is what allows
// rebuilding an IMMUTABLE object: with no setters, the only way of putting it back together is
// passing the values to the constructor, and one has to know which parameter is which property.
//
// Note: declared but not read in this tree; see Introspector's header.
@Target({ ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstructorProperties {

    String[] value();
}
