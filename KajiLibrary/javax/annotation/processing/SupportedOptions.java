package javax.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Las opciones (`-Aclave=valor`) que un procesador declara entender. La lee
// `AbstractProcessor.getSupportedOptions()` por reflexion.
//
// AVISO — en esta VM esta anotacion NO se puede leer en tiempo de ejecucion. Nuestro javac no emite
// `RuntimeVisibleAnnotations` cuando el tipo de la anotacion se resuelve desde el **classpath** (ver
// el encabezado de `AbstractProcessor`), que es siempre el caso para un procesador de usuario. La
// declaracion es correcta y sirve como documentacion y para el javac real; `getSupportedOptions()`
// devolvera el conjunto vacio hasta que el compilador se arregle.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedOptions {

    /** Los nombres de las opciones. */
    String[] value();
}
