package javax.annotation.processing;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.Set;

// El contrato que implementa todo procesador de anotaciones (JSR 269 §Processor).
//
// El **protocolo** es lo que hay que entender, y es rigido a proposito: la herramienta construye el
// procesador con su constructor sin argumentos, le pregunta que soporta (las tres `getSupported*`),
// le da `init(env)` **una sola vez**, y recien despues lo llama a `process(...)` una vez por ronda
// hasta que no quede nada generado, mas una ronda final con `processingOver() == true`. Nunca al
// reves: preguntar antes de `init` esta permitido, generar despues de la ronda final no.
//
// En este proyecto el que corre ese protocolo es la propia VM (`src/jvm/interpreter/apt.rs`); lo
// normal es no implementar esta interfaz a mano sino extender `AbstractProcessor`.
public interface Processor {

    /** Las opciones `-A` que este procesador entiende. */
    Set<String> getSupportedOptions();

    /**
     * Los tipos de anotacion que este procesador quiere ver, por nombre completo. `"*"` significa
     * todas.
     */
    Set<String> getSupportedAnnotationTypes();

    /** La ultima version del lenguaje que este procesador entiende. */
    SourceVersion getSupportedSourceVersion();

    /**
     * Le entrega el entorno. La herramienta lo llama exactamente una vez, antes de cualquier
     * `process`.
     */
    void init(ProcessingEnvironment processingEnv);

    /**
     * Procesa una ronda.
     *
     * @return `true` si este procesador **reclama** esas anotaciones, y entonces no se le ofrecen a
     *         ningun otro procesador posterior. Devolver `true` de mas es la forma clasica de
     *         romper a un procesador ajeno sin darse cuenta.
     */
    boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);

    /**
     * Sugerencias de completado para el valor de un elemento de anotacion, para un IDE. Devolver
     * una coleccion vacia es una respuesta valida y es lo que hace casi todo procesador.
     */
    Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
            ExecutableElement member, String userText);
}
