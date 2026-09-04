package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Todo lo que un PropertyDescriptor lleva, dicho en la declaracion en vez de en una clase BeanInfo
// aparte: si la propiedad es ligada, si es de expertos, como describirla, y —cuando el valor sale
// de una lista cerrada— cuales son los valores validos.
//
// `bound` viene en true por defecto, al reves que el campo del descriptor: quien se toma el trabajo
// de anotar una propiedad casi siempre la esta exponiendo a una herramienta que quiere enterarse
// de los cambios.
//
// Nota: declarada pero no leida en este arbol; ver el encabezado de Introspector.
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanProperty {

    boolean bound() default true;

    boolean expert() default false;

    boolean hidden() default false;

    boolean preferred() default false;

    boolean required() default false;

    // Si al cambiar esta propiedad cambia tambien como se ve el bean.
    boolean visualUpdate() default false;

    String description() default "";

    // Los valores validos, de a ternas nombre/valor/codigo, cuando la propiedad es de lista cerrada.
    String[] enumerationValues() default {};
}
