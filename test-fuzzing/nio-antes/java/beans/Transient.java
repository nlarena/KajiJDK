package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// It marks a pair of accessors as NOT persistable: the property exists for the program but does not
// have to travel when the bean is serialized.
//
// It is declared on the method and not on the field on purpose: what is persisted is the property,
// and the property is its accessors.
//
// Note: in this tree the annotation is declared but NOBODY reads it. The javac loses
// @Retention(RUNTIME) when the annotation type comes from the classpath, so at run time it is not
// seen. See Introspector's header.
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {

    boolean value() default true;
}
