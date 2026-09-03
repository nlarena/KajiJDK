package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// Lo que un procesador ve de **una** ronda (JSR 269 §RoundEnvironment). Se construye de nuevo en
// cada ronda: preguntarle a uno viejo no da los elementos nuevos.
//
// El implementador de este proyecto es `RoundEnvironmentImpl`.
public interface RoundEnvironment {

    /**
     * Si esta es la ronda **final**, la que corre despues de que ya no se genero nada mas. En ella
     * un procesador puede hacer su verificacion global, pero ya no tiene sentido generar: no queda
     * ronda que procese lo generado.
     */
    boolean processingOver();

    /**
     * Si alguien reporto un error en la ronda **anterior**. Sirve para no encadenar errores
     * derivados sobre un modelo que ya se sabe roto.
     */
    boolean errorRaised();

    /** Los tipos raiz de esta ronda: lo que la herramienta va a compilar. */
    Set<? extends Element> getRootElements();

    /** Los elementos anotados con `a`, buscando en las raices y en lo anidado. */
    Set<? extends Element> getElementsAnnotatedWith(TypeElement a);

    /**
     * Igual, pero nombrando el tipo de anotacion por su `Class`. Es la variante comoda cuando el
     * procesador tiene la anotacion en su propio classpath; la de `TypeElement` es la general (una
     * anotacion puede no estar cargada).
     */
    Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a);

    // Las dos de abajo son `default` en el contrato: la union sobre varias anotaciones se define
    // enteramente en terminos de la busqueda de a una, asi que no hay nada que un implementador
    // pueda saber mejor. Se escriben sin streams, que es lo que hay aca.

    /** La union de {@link #getElementsAnnotatedWith(TypeElement)} sobre todas las de `annotations`. */
    default Set<? extends Element> getElementsAnnotatedWithAny(TypeElement... annotations) {
        // `LinkedHashSet` y no `HashSet`: el orden queda determinado por el de `annotations`, asi
        // que dos corridas iguales dan la misma secuencia y los mensajes no bailan. Inmutable al
        // salir, porque el conjunto lo fabrica el contrato y no es de nadie para modificar.
        Set<Element> result = new LinkedHashSet<Element>();
        for (TypeElement a : annotations) {
            result.addAll(this.getElementsAnnotatedWith(a));
        }
        return Collections.unmodifiableSet(result);
    }

    /** La union de {@link #getElementsAnnotatedWith(Class)} sobre todas las de `annotations`. */
    default Set<? extends Element> getElementsAnnotatedWithAny(
            Set<Class<? extends Annotation>> annotations) {
        Set<Element> result = new LinkedHashSet<Element>();
        for (Class<? extends Annotation> a : annotations) {
            result.addAll(this.getElementsAnnotatedWith(a));
        }
        return Collections.unmodifiableSet(result);
    }
}
