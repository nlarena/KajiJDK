package javax.annotation.processing;

import javax.lang.model.SourceVersion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// La ultima version del lenguaje que un procesador declara soportar. La lee
// `AbstractProcessor.getSupportedSourceVersion()` por reflexion.
//
// AVISO — igual que `@SupportedOptions` y `@SupportedAnnotationTypes`: en esta VM no se puede leer
// en tiempo de ejecucion (ver el encabezado de `AbstractProcessor`), asi que
// `getSupportedSourceVersion()` cae siempre en su valor por defecto.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedSourceVersion {

    /** La version soportada. */
    SourceVersion value();
}
