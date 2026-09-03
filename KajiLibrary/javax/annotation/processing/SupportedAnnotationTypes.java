package javax.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Los tipos de anotacion que un procesador declara querer ver. La lee
// `AbstractProcessor.getSupportedAnnotationTypes()` por reflexion.
//
// AVISO — en esta VM esta anotacion NO se puede leer en tiempo de ejecucion, por un bug de nuestro
// javac (no emite `RuntimeVisibleAnnotations` cuando el tipo de la anotacion viene del classpath).
// Ver el encabezado de `AbstractProcessor` para el detalle y la consecuencia.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedAnnotationTypes {

    /** Los nombres completos de los tipos de anotacion; `"*"` son todos. */
    String[] value();
}
