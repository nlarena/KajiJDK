package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Declara sobre la clase lo que un BeanDescriptor diria: la descripcion, y cual es la propiedad y
// el conjunto de eventos por defecto.
//
// Nota: declarada pero no leida en este arbol; ver el encabezado de Introspector.
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaBean {

    String description() default "";

    String defaultProperty() default "";

    String defaultEventSet() default "";
}
