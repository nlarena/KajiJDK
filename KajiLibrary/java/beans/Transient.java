package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marca un par de accesores como NO persistible: la propiedad existe para el programa pero no
// tiene que viajar cuando el bean se serializa.
//
// Se declara sobre el metodo y no sobre el campo a proposito: lo que se persiste es la propiedad,
// y la propiedad son sus accesores.
//
// Nota: en este arbol la anotacion se declara pero NO la lee nadie. El javac pierde
// @Retention(RUNTIME) cuando el tipo anotacion viene del classpath, asi que en ejecucion no se ve.
// Ver el encabezado de Introspector.
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {

    boolean value() default true;
}
