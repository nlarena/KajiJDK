package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * KajiLibrary's javax.lang.model.util.ElementScanner6 — el visitante que **baja** por la estructura en
 * vez de quedarse en el elemento que le dieron.
 *
 * <h2>En que se diferencia del `Simple`</h2>
 *
 * <p>{@link SimpleElementVisitor6} visita **un** elemento: le das una clase y te contesta sobre la
 * clase. Este recorre el arbol: le das una clase y visita la clase, sus campos, sus metodos, los
 * parametros de cada metodo, y los tipos anidados con todo lo suyo. Es lo que hace falta para "listame
 * todos los metodos publicos de este paquete", que es la mitad de lo que hace un procesador de
 * anotaciones.
 *
 * <p>El recorrido esta en los `visitXxx`: cada uno, en vez de caer en un embudo, llama a
 * {@link #scan(Iterable, Object)} sobre sus hijos. Cuales son "sus hijos" depende del elemento, y ahi
 * hay una asimetria que conviene ver: un paquete y un tipo bajan por `getEnclosedElements()`, pero un
 * **ejecutable baja por sus parametros**, no por sus elementos contenidos. Es deliberado — las variables
 * locales de un metodo no estan en el modelo, y los parametros si.
 *
 * <h2>Como se combinan los resultados, que es la parte que sorprende</h2>
 *
 * <p>`scan(Iterable, P)` recorre los hijos y devuelve **el resultado del ultimo**, descartando los
 * anteriores. No los suma ni los junta en una lista: no podria, porque `R` es cualquier cosa y no hay un
 * "combinar" generico. Si la coleccion esta vacia devuelve `DEFAULT_VALUE`.
 *
 * <p>Eso hace que el valor de retorno sea casi inutil tal cual viene, y por eso el uso normal de un
 * escaner es **acumular en el visitante o en `P`** y no mirar lo que devuelve. Quien quiera un
 * `R` con sentido redefine {@link #scan(Element, Object)} para combinar; para eso no es final, mientras
 * que las otras dos formas de `scan` si lo son.
 *
 * <p>`visitVariable` aparta `RESOURCE_VARIABLE` por la misma razon que en {@link SimpleElementVisitor6},
 * y `visitRecordComponent` cae en `visitUnknown` porque los registros son de Java 14.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ElementScanner6<R, P> extends AbstractElementVisitor6<R, P> {

    /** Lo que devuelve un recorrido que no visito nada. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected ElementScanner6() {
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected ElementScanner6(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** Recorre los hijos y devuelve el resultado del ultimo. Ver el encabezado por que solo el ultimo. */
    public final R scan(Iterable<? extends Element> iterable, P p) {
        R result = this.DEFAULT_VALUE;
        for (Element e : iterable) {
            result = this.scan(e, p);
        }
        return result;
    }

    /** El paso de un elemento. Es el punto que se redefine para combinar resultados o llevar cuenta. */
    public R scan(Element e, P p) {
        return e.accept(this, p);
    }

    /** Igual, con parametro nulo. */
    public final R scan(Element e) {
        return this.scan(e, null);
    }

    public R visitPackage(PackageElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }

    public R visitType(TypeElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }

    public R visitVariable(VariableElement e, P p) {
        if (e.getKind() != ElementKind.RESOURCE_VARIABLE) {
            return this.scan(e.getEnclosedElements(), p);
        }
        return this.visitUnknown(e, p);
    }

    // Los parametros y no los contenidos: ver el encabezado.
    public R visitExecutable(ExecutableElement e, P p) {
        return this.scan(e.getParameters(), p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.visitUnknown(e, p);
    }
}
